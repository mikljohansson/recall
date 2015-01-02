package se.embargo.recall;

import android.content.SharedPreferences;
import android.os.Bundle;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;

public class SettingsActivity extends SherlockPreferenceActivity {
	public static final String PREFS_NAMESPACE = "se.embargo.recollect";
	
	public static final String PREF_VOICE_RECORDING_COUNT = "voice-recording-count";
	public static final String PREF_PHONECALL_RECORDING_COUNT = "phonecall-recording-count";

	public static final String PREF_PHONECALL_RECORD_ALL = "phonecall-record-all";
	public static final boolean PREF_PHONECALL_RECORD_ALL_DEFAULT = true;

	public static final String PREF_PHONECALL_RECORD_UNKNOWN = "phonecall-record-unknown";
	public static final boolean PREF_PHONECALL_RECORD_UNKNOWN_DEFAULT = true;
	
	public static final String PREF_PHONECALL_RECORD_PRIVATE = "phonecall-record-private";
	public static final boolean PREF_PHONECALL_RECORD_PRIVATE_DEFAULT = true;

	public static final String PREF_PHONECALL_RECORD_GESTURE = "phonecall-record-gesture";
	public static final boolean PREF_PHONECALL_RECORD_GESTURE_DEFAULT = true;
	
	public static final String PREF_NOTIFICATION_RECORDED = "notification-recorded";
	public static final boolean PREF_NOTIFICATION_RECORDED_DEFAULT = true;

	public static final String PREF_RECORDING_SUPPORTED = "recording-supported";
	public static final boolean PREF_RECORDING_SUPPORTED_DEFAULT = false;

	public static final String PREF_RECORDING = "recording-active";

	private SharedPreferences _prefs;
	private PreferenceListener _prefsListener;
	
	@Override
	@SuppressWarnings("deprecation")
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		getPreferenceManager().setSharedPreferencesName(PREFS_NAMESPACE);
		addPreferencesFromResource(R.xml.settings);
		
		_prefsListener = new PreferenceListener();
		_prefs = getSharedPreferences(PREFS_NAMESPACE, MODE_PRIVATE);
		_prefs.registerOnSharedPreferenceChangeListener(_prefsListener);
		
		init();
	}

	@SuppressWarnings("deprecation")
	private void init() {
		boolean enabled = _prefs.getBoolean(PREF_PHONECALL_RECORD_ALL, PREF_PHONECALL_RECORD_ALL_DEFAULT);
		getPreferenceScreen().findPreference(PREF_PHONECALL_RECORD_UNKNOWN).setEnabled(!enabled);
		getPreferenceScreen().findPreference(PREF_PHONECALL_RECORD_PRIVATE).setEnabled(!enabled);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
            case android.R.id.home: {
                finish();
                return true;
            }

			default:
				return super.onOptionsItemSelected(item);
		}
	}
	
	private class PreferenceListener implements SharedPreferences.OnSharedPreferenceChangeListener {
		@Override
		public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
			if (PREF_PHONECALL_RECORD_ALL.equals(key)) {
				init();
			}
		}
	}
}
