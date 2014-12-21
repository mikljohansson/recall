package se.embargo.recall.database;

import java.util.UUID;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.net.Uri;

public class Phonecall {
	public static final String TABLENAME = "phonecall";
	public static final String ID = "_id";
	public static final String PHONENUMBER = "phonenumber";
	public static final String URI = "uri";
	public static final String MIMETYPE = "mimetype";
	public static final String MODIFIED = "modified";
	
	@SuppressLint("DefaultLocale")
	public static ContentValues create(String phonenumber, String mimetype, Uri uri) {
		if (phonenumber == null) {
			phonenumber = "unknown";
		}

		ContentValues values = new ContentValues();
		values.put(ID, UUID.randomUUID().toString());
		values.put(URI, new Uri.Builder().scheme(uri.getScheme().toLowerCase()).encodedOpaquePart(uri.getSchemeSpecificPart()).encodedFragment(uri.getFragment()).toString());
		values.put(PHONENUMBER, phonenumber);
		values.put(MIMETYPE, mimetype.toLowerCase());
		values.put(MODIFIED, System.currentTimeMillis());
		return values;
	}
}
