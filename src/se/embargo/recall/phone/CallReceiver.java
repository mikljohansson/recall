package se.embargo.recall.phone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;

public class CallReceiver extends BroadcastReceiver {
	private static final String TAG = "CallReceiver";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		if (Intent.ACTION_NEW_OUTGOING_CALL.equals(intent.getAction())) {
			String phonenumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
			Intent args = new Intent(context, CallRecorderService.class);
			Log.i(TAG, "Received action: " + intent.getAction());
			
			args.putExtra(CallRecorderService.EXTRA_EVENT, CallRecorderService.EXTRA_STATE_OUTGOING);
			args.putExtra(CallRecorderService.EXTRA_PHONE_NUMBER, phonenumber);
			context.startService(args);
		}
		else if (TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(intent.getAction())) {
			String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
			Intent args = new Intent(context, CallRecorderService.class);
			Log.i(TAG, "Received action: " + intent.getAction() + "/" + state);
			
			if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
				String phonenumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
				args.putExtra(CallRecorderService.EXTRA_EVENT, CallRecorderService.EXTRA_STATE_RINGING);
				args.putExtra(CallRecorderService.EXTRA_PHONE_NUMBER, phonenumber);
				context.startService(args);
			}
			else if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)) {
				args.putExtra(CallRecorderService.EXTRA_EVENT, CallRecorderService.EXTRA_STATE_OFFHOOK);
				context.startService(args);
			}
			else if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
				args.putExtra(CallRecorderService.EXTRA_EVENT, CallRecorderService.EXTRA_STATE_IDLE);
				context.startService(args);
			}
		}
		else if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
			Intent args = new Intent(context, CallRecorderService.class);
			args.putExtra(CallRecorderService.EXTRA_EVENT, CallRecorderService.EXTRA_STATE_BOOT);
			context.startService(args);
		}
	}
}
