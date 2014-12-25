package se.embargo.recall.database;

import java.util.UUID;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.net.Uri;

public class Phonecall {
	public static final String TABLENAME = "phonecall";
	public static final String ID = "_id";
	public static final String PHONENUMBER = "phonenumber";
	public static final String MIMETYPE = "mimetype";
	public static final String URI = "uri";
	public static final String DIRECTION = "direction";
	public static final String DURATION = "duration";
	public static final String MODIFIED = "modified";
	
	public enum Direction { INCOMING, OUTGOING };
	
	@SuppressLint("DefaultLocale")
	public static ContentValues create(String phonenumber, String mimetype, Uri uri, Direction direction, long duration) {
		ContentValues values = new ContentValues();
		values.put(ID, UUID.randomUUID().toString());
		values.put(PHONENUMBER, phonenumber);
		values.put(URI, new Uri.Builder().scheme(uri.getScheme().toLowerCase()).encodedOpaquePart(uri.getSchemeSpecificPart()).encodedFragment(uri.getFragment()).toString());
		values.put(MIMETYPE, mimetype.toLowerCase());
		values.put(DIRECTION, toInteger(direction));
		values.put(DURATION, duration);
		values.put(MODIFIED, System.currentTimeMillis());
		return values;
	}

	public static int toInteger(Direction direction) {
		switch (direction) {
			case INCOMING:
				return 0;
				
			case OUTGOING:
				return 1;
		}
		
		return 0;
	}
	
	public static Direction toAction(int direction) {
		switch (direction) {
			case 0:
				return Direction.INCOMING;
				
			case 1:
				return Direction.OUTGOING;
		}
		
		return Direction.INCOMING;
	}
}
