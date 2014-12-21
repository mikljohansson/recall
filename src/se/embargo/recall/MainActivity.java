package se.embargo.recall;

import se.embargo.recall.phone.CallRecorderService;
import se.embargo.recall.phone.PhonecallListFragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class MainActivity extends SherlockFragmentActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent args = new Intent(this, CallRecorderService.class);
		args.putExtra(CallRecorderService.EXTRA_EVENT, CallRecorderService.EXTRA_STATE_BOOT);
		startService(args);
		
		setContentView(R.layout.main_activity);

		Fragment fragment = Fragment.instantiate(this, PhonecallListFragment.class.getName());
		getSupportFragmentManager().beginTransaction().replace(R.id.fragment, fragment).commit();
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
