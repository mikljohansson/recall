package se.embargo.recall;

import se.embargo.core.databinding.DataBindingContext;
import se.embargo.core.databinding.PreferenceProperties;
import se.embargo.core.databinding.WidgetProperties;
import se.embargo.core.databinding.observable.ObservableValueAdapter;
import se.embargo.recall.phone.CallService;
import se.embargo.recall.phone.PhonecallListFragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class MainActivity extends SherlockFragmentActivity {
	private DataBindingContext _binding = new DataBindingContext();
	private SharedPreferences _prefs;
	
	/**
	 * The listener needs to be kept alive since SharedPrefernces only keeps a weak reference to it
	 */
	private PreferencesListener _prefsListener = new PreferencesListener();
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		_prefs = getSharedPreferences(SettingsActivity.PREFS_NAMESPACE, MODE_PRIVATE);

		Intent args = new Intent(this, CallService.class);
		args.putExtra(CallService.EXTRA_EVENT, CallService.EXTRA_STATE_BOOT);
		startService(args);
		
		setContentView(R.layout.main_activity);

		Fragment fragment = Fragment.instantiate(this, PhonecallListFragment.class.getName());
		getSupportFragmentManager().beginTransaction().replace(R.id.fragment, fragment).commit();
		
		View unsupportedLayout = findViewById(R.id.recordingUnsupportedLayout);
		_binding.bindValue(
			WidgetProperties.visible().observe(unsupportedLayout), 
			new ObservableValueAdapter<Boolean, Integer>(PreferenceProperties.bool(
				SettingsActivity.PREF_RECORDING_SUPPORTED, SettingsActivity.PREF_RECORDING_SUPPORTED_DEFAULT).observe(_prefs)) {
				@Override
				public Integer getValue() {
					return _object.getValue() ? View.GONE : View.VISIBLE;
				}

				@Override
				public void setValue(Integer value) {}
			});
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		_prefs.registerOnSharedPreferenceChangeListener(_prefsListener);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		_prefs.unregisterOnSharedPreferenceChangeListener(_prefsListener);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.main_options, menu);
		
		// Set the scan button icon depending on the scanner service
		if (isRecording()) {
			menu.getItem(0).setIcon(R.drawable.ic_action_micoff);
			menu.getItem(0).setTitle(R.string.menu_option_micoff);
		}
		
		return true;
	}

	private boolean isRecording() {
		return _prefs.getBoolean(SettingsActivity.PREF_RECORDING, false);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.micButton: {
				Intent args = new Intent(this, CallService.class);
				args.putExtra(CallService.EXTRA_EVENT, isRecording() ? CallService.EXTRA_STATE_STOP_RECORDING : CallService.EXTRA_STATE_START_RECORDING);
				startService(args);
				return true;
			}
			
			case R.id.editSettingsButton: {
				// Start preferences activity
				Intent intent = new Intent(this, SettingsActivity.class);
				startActivity(intent);
				return true;
			}

			default:
				return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * Listens for preference changes and applies updates
	 */
	private class PreferencesListener implements SharedPreferences.OnSharedPreferenceChangeListener {
		@Override
		public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
			if (SettingsActivity.PREF_RECORDING.equals(key)) {
				supportInvalidateOptionsMenu();
			}
		}
	}
}
