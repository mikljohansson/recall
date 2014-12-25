package se.embargo.recall;

import se.embargo.core.databinding.DataBindingContext;
import se.embargo.core.databinding.PreferenceProperties;
import se.embargo.core.databinding.WidgetProperties;
import se.embargo.core.databinding.observable.ObservableValueAdapter;
import se.embargo.recall.phone.CallRecorderService;
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
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		_prefs = getSharedPreferences(SettingsActivity.PREFS_NAMESPACE, MODE_PRIVATE);

		Intent args = new Intent(this, CallRecorderService.class);
		args.putExtra(CallRecorderService.EXTRA_EVENT, CallRecorderService.EXTRA_STATE_BOOT);
		startService(args);
		
		setContentView(R.layout.main_activity);

		Fragment fragment = Fragment.instantiate(this, PhonecallListFragment.class.getName());
		getSupportFragmentManager().beginTransaction().replace(R.id.fragment, fragment).commit();
		
		View unsupportedLayout = findViewById(R.id.recordingUnsupportedLayout);
		_binding.bindValue(
			WidgetProperties.visible().observe(unsupportedLayout), 
			new ObservableValueAdapter<Boolean, Integer>(PreferenceProperties.bool(SettingsActivity.PREF_RECORDING_SUPPORTED, true).observe(_prefs)) {
				@Override
				public Integer getValue() {
					return _object.getValue() ? View.GONE : View.VISIBLE;
				}

				@Override
				public void setValue(Integer value) {}
			});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.main_options, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
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
}
