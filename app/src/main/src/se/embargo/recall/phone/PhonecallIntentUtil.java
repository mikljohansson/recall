package se.embargo.recall.phone;

import java.util.ArrayList;
import java.util.Collection;

import se.embargo.core.database.Cursors;
import se.embargo.recall.R;
import se.embargo.recall.database.Phonecall;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public class PhonecallIntentUtil {
	private static final String TAG = "JobIntentUtil";

	/**
	 * Dispatches an Intent.ACTION_SEND_MULTIPLE to send the selected content.
	 */
	public static void send(Context context, Collection<Uri> uris) {
    	// Add attachments 
    	ArrayList<Uri> extrauris = new ArrayList<Uri>();
		for (Uri uri : uris) {
			ContentValues values = getContentValues(context, uri);
			if (values != null) {
				String contenturi = values.getAsString(Phonecall.URI);
				String mimetype = values.getAsString(Phonecall.MIMETYPE);
				
				if (contenturi != null && mimetype != null) {
					extrauris.add(Uri.parse(contenturi));
				}
			}
		}
		
		// Build the message intent
		Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
		intent.putExtra(Intent.EXTRA_SUBJECT, "");
		intent.putExtra(Intent.EXTRA_TEXT, "");
		intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, extrauris);
		intent.setType("*/*");
		
		// Present a chooser dialog without the "Complete action using" and "Always" buttons
		Intent chooser = Intent.createChooser(intent, context.getString(R.string.msg_action_send));
		
		try {
			context.startActivity(chooser);
		}
		catch (RuntimeException e) {
			Log.e(TAG, "Failed to send message", e);
		}
	}

	private static ContentValues getContentValues(Context context, Uri uri) {
		Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
		try {
			if (cursor != null && cursor.moveToFirst()) {
				return Cursors.toContentValues(cursor);
			}
		}
		finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		
		return null;
	}
}
