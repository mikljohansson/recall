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
import se.embargo.recall.R;
import se.embargo.recall.SettingsActivity;
import se.embargo.recall.database.Phonecall;
import se.embargo.recall.database.RecallRepository;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

public class CallRecorderService extends AbstractService {
	private static final String TAG = "CallRecorderService";
	
	private static final String DIRECTORY = "Recollect/%s";
	private static final String FILENAME_PATTERN = "%s-%s-%d.mp4";
	
	public static final String EXTRA_EVENT = "se.embargo.oddjob.phone.CallRecorderService.event";
	public static final String EXTRA_PHONE_NUMBER = "se.embargo.oddjob.phone.CallRecorderService.phonenumber";
	
	public static final String EXTRA_STATE_OUTGOING = "outgoing";
	public static final String EXTRA_STATE_RINGING = "ringing";
	public static final String EXTRA_STATE_OFFHOOK = "offhook";
	public static final String EXTRA_STATE_IDLE = "idle";
	public static final String EXTRA_STATE_BOOT = "boot";
	
	private static final int NOTIFICATION_ID = 0;
	
	private SharedPreferences _prefs;
	
	private GestureDetector _gesture = null;
	private String _phonenumber = null;
	
	private MediaRecorder _recorder = null;
	private String _filename = null;
	private boolean _incall = false, _prepared = false;
	
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
	
				if (_phonenumber != null) {
					// Start gesture detection before call has been answered
					if (_gesture == null && _prefs.getBoolean(SettingsActivity.PREF_PHONECALL_RECORD_GESTURE, SettingsActivity.PREF_PHONECALL_RECORD_GESTURE_DEFAULT)) {
						_gesture = new GestureDetector();
					}
				}
			}
			else if (EXTRA_STATE_OFFHOOK.equals(event)) {
				if (_phonenumber != null) {
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
		}
		
		return result;
	}
	
	@SuppressWarnings("deprecation")
	private void startRecording() {
		boolean first = (_recorder == null);
		
		if (first) {
	        try {
				_recorder = new MediaRecorder();
		        _recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_CALL);
		    	_recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
		    	_recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
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
        
        if (_incall && _recorder != null) {
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
        }
        
        if (first) {
			// Display a notification about us starting and put a persistent icon in the status bar
			Notification statusMessage = new Notification(
				R.drawable.ic_launcher, getText(R.string.msg_recording_phonecall), System.currentTimeMillis());
	
			// The PendingIntent to launch our activity if the user selects this notification
			PendingIntent contentIntent = PendingIntent.getActivity(
					this, 0, new Intent(this, MainActivity.class), 0);
			statusMessage.setLatestEventInfo(this, 
				getText(R.string.msg_recording_phonecall), 
				getText(R.string.msg_recording_phonecall), contentIntent);
			
			startForeground(R.string.msg_recording_phonecall, statusMessage);
        }
	}
	
	private void stopRecording() {
		if (_recorder != null) {
			// Finalize audio recording
			try {
				_recorder.stop();
				_recorder.reset();
				_recorder.release();

				// Insert into database
				Uri uri = Uri.parse("file://" + _filename);
				ContentValues phonecall = Phonecall.create(_phonenumber, "audio/mp4", uri);
	        	getContentResolver().insert(RecallRepository.PHONECALL_URI, phonecall);

				Log.i(TAG, "Finished recording phonecall with " + _phonenumber);
			}
			catch (RuntimeException e) {
				// Cleanup file if recorder throws
				new File(_filename).delete();
				Log.w(TAG, "Failed to record phonecall with " + _phonenumber, e);
			}
			finally {
				_recorder = null;
				_filename = null;
				_prepared = false;
			}
		}
		
		// Remove the status message and stop the service
		stopForeground(true);
		stopService();
		
		// Show notification with link to recording
		if (_prefs.getBoolean(SettingsActivity.PREF_NOTIFICATION_RECORDED, SettingsActivity.PREF_NOTIFICATION_RECORDED_DEFAULT)) {
			PendingIntent contentIntent = TaskStackBuilder.create(this).
				addParentStack(MainActivity.class).
				addNextIntent(new Intent(this, MainActivity.class)).
				getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
			
			NotificationCompat.Builder builder = new NotificationCompat.Builder(this).
				setSmallIcon(R.drawable.ic_notification_phone_missed).
				setContentTitle(getString(R.string.msg_recorded_phonecall)).
				setContentIntent(contentIntent);

			ContactDetails contact = new Contacts(this).getPhonecallDetails(_phonenumber);
			if (contact != null) {
				builder.setContentText(getString(R.string.msg_recorded_phonecall_from, contact.name));
				builder.setLargeIcon(contact.photo);
			}
			else if (_phonenumber != null && !"".equals(_phonenumber)) {
				builder.setContentText(getString(R.string.msg_recorded_phonecall_from, _phonenumber));
			}
			else {
				builder.setContentText(getString(R.string.phonecall_private_number));
			}
			
			NotificationManager manager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
			manager.notify(NOTIFICATION_ID, builder.build());
		}
	}
	
	@SuppressLint({ "DefaultLocale", "SimpleDateFormat" })
	private File createOutputFile() {
		String datetime = new SimpleDateFormat("yyyy-MM-dd_HHmm").format(new Date());
		String phonenumber = _phonenumber.replaceFirst("\\+", "00").replaceAll("[^\\d]", "");
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
			(phonenumber != null && !"".equals(phonenumber)) && !isContact(phonenumber)) {
			return true;
		}
		
		if (_prefs.getBoolean(SettingsActivity.PREF_PHONECALL_RECORD_PRIVATE, SettingsActivity.PREF_PHONECALL_RECORD_PRIVATE_DEFAULT) && 
			(phonenumber == null || "".equals(phonenumber))) {
			return true;
		}

		return false;
	}
	
	private boolean isContact(String number) {
		if (number == null) {
			return false;
		}
		
	    Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
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
			super(CallRecorderService.this);
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
