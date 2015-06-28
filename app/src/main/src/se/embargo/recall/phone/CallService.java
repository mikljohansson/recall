package se.embargo.recall.phone;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import se.embargo.core.Contacts;
import se.embargo.core.Contacts.ContactDetails;
import se.embargo.core.gesture.ShakeGestureDetector;
import se.embargo.core.service.AbstractService;
import se.embargo.recall.MainActivity;
import se.embargo.recall.Phonenumbers;
import se.embargo.recall.R;
import se.embargo.recall.SettingsActivity;
import se.embargo.recall.database.Phonecall;
import se.embargo.recall.database.RecallRepository;
import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

public class CallService extends AbstractService {
	private static final int AUDIO_ENCODER = MediaRecorder.AudioEncoder.AAC;
	private static final int AUDIO_FORMAT = MediaRecorder.OutputFormat.MPEG_4;
	private static final int AUDIO_SAMPLING_RATE = 44100;
	private static final int AUDIO_ENCODING_BITRATE = 24000;

	private static final String TAG = "CallRecorderService";
	
	private static final String DIRECTORY = "Recall/%s";
	private static final String FILENAME_PATTERN = "%s-%s-%d.mp4";
	
	public static final String EXTRA_EVENT = "se.embargo.recall.phone.CallRecorderService.event";
	public static final String EXTRA_PHONE_NUMBER = "se.embargo.recall.phone.CallRecorderService.phonenumber";
	
	public static final String EXTRA_STATE_OUTGOING = "outgoing";
	public static final String EXTRA_STATE_RINGING = "ringing";
	public static final String EXTRA_STATE_OFFHOOK = "offhook";
	public static final String EXTRA_STATE_IDLE = "idle";
	public static final String EXTRA_STATE_BOOT = "boot";
	public static final String EXTRA_STATE_START_RECORDING = "start-recording";
	public static final String EXTRA_STATE_STOP_RECORDING = "stop-recording";
	
	private static final int NOTIFICATION_ID = 0;
	
	private SharedPreferences _prefs;
	
	private GestureDetector _gesture = null;
	private String _phonenumber = null;
	private Phonecall.Direction _direction = Phonecall.Direction.INCOMING;
	private long _recordingStartTime = 0;
	
	private MediaRecorder _recorder = null;
	private String _filename = null;
	private boolean _incall = false, _prepared = false;
	
	private TestRecordingTask _task = null;
	
	@Override
	public void onCreate() {
		super.onCreate();
		_prefs = getSharedPreferences(SettingsActivity.PREFS_NAMESPACE, MODE_PRIVATE);
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		int result = super.onStartCommand(intent, flags, startId);
		
		if (intent != null) {
			String event = intent.getStringExtra(EXTRA_EVENT);
			if (EXTRA_STATE_OUTGOING.equals(event) || EXTRA_STATE_RINGING.equals(event)) {
				_phonenumber = intent.getStringExtra(EXTRA_PHONE_NUMBER);
				_direction = EXTRA_STATE_OUTGOING.equals(event) ? Phonecall.Direction.OUTGOING : Phonecall.Direction.INCOMING;
	
				// Start gesture detection before call has been answered
				if (_gesture == null && _prefs.getBoolean(SettingsActivity.PREF_PHONECALL_RECORD_GESTURE, SettingsActivity.PREF_PHONECALL_RECORD_GESTURE_DEFAULT)) {
					_gesture = new GestureDetector();
				}
			}
			else if (EXTRA_STATE_OFFHOOK.equals(event)) {
				Log.i(TAG, "Call went through to: " + _phonenumber);
				_incall = true;
	
				// Start recording if shaken or if phonenumber match the rules
				if (_prepared || isRecordedNumber(_phonenumber)) {
					startRecording();
				}
				else {
					Log.i(TAG, "Ignoring phonecall with: " + _phonenumber);
				}
			}
			else if (EXTRA_STATE_IDLE.equals(event)) {
				// Stop gesture detection
				if (_gesture != null) {
					_gesture.dispose();
					_gesture = null;
				}
	
				// Stop recording
				stopRecording();
				
				_phonenumber = null;
				_incall = false;
			}
			else if (EXTRA_STATE_START_RECORDING.equals(event)) {
				startRecording(true);
			}
			else if (EXTRA_STATE_STOP_RECORDING.equals(event)) {
				stopRecording();
			}
			else if (EXTRA_STATE_BOOT.equals(event)) {
				// Test if VOICE_CALL is supported
				if (_task == null && _recorder == null) {
					_task = new TestRecordingTask();
					_task.execute();
				}
			}
		}
		
		return result;
	}
	
	private class TestRecordingTask extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... params) {
			File file = null;
			try {
				Log.i(TAG, "Test recording from MediaRecorder.AudioSource.VOICE_CALL");
				file = File.createTempFile("recall", ".mp4");
				MediaRecorder recorder = new MediaRecorder();
				recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_CALL);
				recorder.setOutputFormat(AUDIO_FORMAT);
				recorder.setAudioChannels(1);
				recorder.setAudioEncoder(AUDIO_ENCODER);
		    	recorder.setAudioSamplingRate(AUDIO_SAMPLING_RATE);
		    	recorder.setAudioEncodingBitRate(AUDIO_ENCODING_BITRATE);
				recorder.setOutputFile(file.toString());
				recorder.prepare();
				recorder.start();
				_prefs.edit().putBoolean(SettingsActivity.PREF_RECORDING_SUPPORTED, true).commit();
				Log.i(TAG, "Voice call recording is supported");
				
				try {
					recorder.stop();
					recorder.reset();
					recorder.release();
				}
				catch (Exception e) {
					Log.w(TAG, "Exception when stopping test recording", e);
				}
			}
			catch (Exception e) {
				Log.w(TAG, "Voice call recording unsupported", e);
				_prefs.edit().putBoolean(SettingsActivity.PREF_RECORDING_SUPPORTED, false).commit();
			}
			finally {
				if (file != null) {
					file.delete();
				}
			}
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			_task = null;
		}
		
		@Override
		protected void onCancelled(Void result) {
			_task = null;
		}
	}
	
	private void startRecording(boolean force) {
		boolean first = (_recorder == null);
		
		if (first) {
	        try {
	        	int source = _prefs.getBoolean(SettingsActivity.PREF_RECORDING_SUPPORTED, SettingsActivity.PREF_RECORDING_SUPPORTED_DEFAULT) ? 
	        		MediaRecorder.AudioSource.VOICE_CALL : MediaRecorder.AudioSource.MIC;
	        	Log.i(TAG, "Using audio source: " + source);
	        	
	        	_recorder = new MediaRecorder();
		        _recorder.setAudioSource(source);
		    	_recorder.setOutputFormat(AUDIO_FORMAT);
		    	_recorder.setAudioEncoder(AUDIO_ENCODER);
		    	_recorder.setAudioSamplingRate(AUDIO_SAMPLING_RATE);
		    	_recorder.setAudioEncodingBitRate(AUDIO_ENCODING_BITRATE);
				_filename = createOutputFile().getAbsolutePath();
		        _recorder.setOutputFile(_filename);
				_recorder.prepare();
				Log.i(TAG, "Started recording phonecall to: " + _filename);
			}
			catch (RuntimeException e) {
				Log.e(TAG, "Failed to start recording", e);
				_recorder = null;
				stopRecording();
				return;
			}
			catch (IOException e) {
				Log.e(TAG, "Failed to start recording", e);
				_recorder = null;
				stopRecording();
				return;
			}
	        
	        _prepared = true;
		}
        
        if ((_incall || force) && _recorder != null) {
        	try {
        		_recorder.start();
        	}
        	catch (RuntimeException e) {
				Log.e(TAG, "Failed to start recording", e);
				_recorder.release();
				_recorder = null;
				stopRecording();
				return;
        	}

            _recordingStartTime = System.currentTimeMillis();
            _prefs.edit().putBoolean(SettingsActivity.PREF_RECORDING, true).commit();
        }

        if (first) {
			// Display a notification about us starting and put a persistent icon in the status bar
			PendingIntent contentIntent = TaskStackBuilder.create(this).
				addParentStack(MainActivity.class).
				addNextIntent(new Intent(this, MainActivity.class)).
				getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

			NotificationCompat.Builder builder = new NotificationCompat.Builder(this).
				setSmallIcon(R.drawable.ic_notification_mic).
				setContentTitle(getString(R.string.msg_recording_phonecall)).
				setContentIntent(contentIntent);
			
			startForeground(R.string.msg_recording_phonecall, builder.build());
        }
	}
	
	private void startRecording() {
		startRecording(false);
	}
	
	private void stopRecording() {
		boolean recorded = false;
		if (_recorder != null) {
			// Finalize audio recording
			try {
				_recorder.stop();
				_recorder.reset();
				_recorder.release();

				// Insert into database
				Uri uri = Uri.parse("file://" + _filename);
				ContentValues phonecall = Phonecall.create(
					_phonenumber, "audio/mp4", uri, 
					_direction, System.currentTimeMillis() - _recordingStartTime);
	        	getContentResolver().insert(RecallRepository.PHONECALL_URI, phonecall);
	        	
				Log.i(TAG, "Finished recording phonecall with " + _phonenumber);
	        	recorded = true;
			}
			catch (RuntimeException e) {
				// Cleanup file if recorder throws
				new File(_filename).delete();
				Log.e(TAG, "Failed to record phonecall with " + _phonenumber, e);
			}
			finally {
				_recorder = null;
				_filename = null;
				_prepared = false;
			}
		}
		
		// Remove the status message and stop the service
		_prefs.edit().putBoolean(SettingsActivity.PREF_RECORDING, false).commit();
		stopForeground(true);
		
		// Show notification with link to recording
		if (recorded && _prefs.getBoolean(SettingsActivity.PREF_NOTIFICATION_RECORDED, SettingsActivity.PREF_NOTIFICATION_RECORDED_DEFAULT)) {
			PendingIntent contentIntent = TaskStackBuilder.create(this).
				addParentStack(MainActivity.class).
				addNextIntent(new Intent(this, MainActivity.class)).
				getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
			
			NotificationCompat.Builder builder = new NotificationCompat.Builder(this).
				setSmallIcon(R.drawable.ic_notification_phone_end).
				setContentTitle(getString(R.string.msg_recorded_phonecall)).
				setContentIntent(contentIntent).
				setAutoCancel(true);

			ContactDetails contact = new Contacts(this).getPhonecallDetails(_phonenumber);
			if (contact != null) {
				builder.setContentText(getString(R.string.msg_recorded_phonecall_from, contact.name));
				builder.setLargeIcon(contact.photo);
			}
			else if (Phonenumbers.isPrivateNumber(_phonenumber)) {
				builder.setContentText(getString(R.string.msg_recorded_phonecall_from_private_number));
			}
			else {
				builder.setContentText(getString(R.string.msg_recorded_phonecall_from, _phonenumber));
			}
			
			NotificationManager manager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
			manager.notify(NOTIFICATION_ID, builder.build());
		}
	}
	
	@SuppressLint({ "DefaultLocale", "SimpleDateFormat" })
	private File createOutputFile() {
		String datetime = new SimpleDateFormat("yyyy-MM-dd_HHmm").format(new Date());
		String phonenumber = _phonenumber != null ? _phonenumber.replaceFirst("\\+", "00").replaceAll("[^\\d]", "") : "private_number";
		String filename;
		File file;
		
		do {
			// Create a new sequential name
			int count = _prefs.getInt(SettingsActivity.PREF_PHONECALL_RECORDING_COUNT, 0);
			filename = String.format(FILENAME_PATTERN, datetime, phonenumber, count);
			
			// Increment the counter
			SharedPreferences.Editor editor = _prefs.edit();
			editor.putInt(SettingsActivity.PREF_PHONECALL_RECORDING_COUNT, count + 1);
			editor.commit();
			
			file = new File(getStorageDirectory(), filename);
		} while (file.exists());
		
		return file;
	}

	/**
	 * @return	The directory where recordings are stored
	 */
	@SuppressLint("SimpleDateFormat")
	private static File getStorageDirectory() {
		File parent = Environment.getExternalStorageDirectory();
		String calldir = String.format(DIRECTORY, new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
		File result = new File(parent + "/" + calldir);
		result.mkdirs();
		return result;
	}
	
	private boolean isRecordedNumber(String phonenumber) {
		if (_prefs.getBoolean(SettingsActivity.PREF_PHONECALL_RECORD_ALL, SettingsActivity.PREF_PHONECALL_RECORD_ALL_DEFAULT)) {
			return true;
		}
		
		if (_prefs.getBoolean(SettingsActivity.PREF_PHONECALL_RECORD_UNKNOWN, SettingsActivity.PREF_PHONECALL_RECORD_UNKNOWN_DEFAULT) && 
			!Phonenumbers.isPrivateNumber(phonenumber) && !isContact(phonenumber)) {
			return true;
		}
		
		if (_prefs.getBoolean(SettingsActivity.PREF_PHONECALL_RECORD_PRIVATE, SettingsActivity.PREF_PHONECALL_RECORD_PRIVATE_DEFAULT) && 
			Phonenumbers.isPrivateNumber(phonenumber)) {
			return true;
		}

		return false;
	}

	private boolean isContact(String phonenumber) {
		if (phonenumber == null) {
			return false;
		}
		
	    Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phonenumber));
	    Cursor cursor = getContentResolver().query(uri, new String[] {BaseColumns._ID}, null, null, null);

	    try {
	        return cursor != null && cursor.moveToFirst();
	    } 
	    finally {
	        if (cursor != null) {
	            cursor.close();
	        }
	    }
	}
	
	private class GestureDetector extends ShakeGestureDetector {
		public GestureDetector() {
			super(CallService.this);
		}

		@Override
		protected void onGestureDetected() {
			if (_recorder == null) {
				startRecording();
			}
			else {
				stopRecording();
			}
		}
	}
}
