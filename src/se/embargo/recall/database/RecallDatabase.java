package se.embargo.recall.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Increment schema version each time a new block of SQL statements are added and tag the block with the
 * application version number in which it gets released. Once released a block of statements must not be 
 * modified or users won't be able to upgrade the application properly. 
 */
public class RecallDatabase extends SQLiteOpenHelper {
	/**
	 * Increment schema version each time a new block of SQL statements are added. 
	 */
	private static int DB_SCHEMA_VERSION = 1;
	
	public RecallDatabase(Context context) {
		super(context, "recall", null, DB_SCHEMA_VERSION);
	}
	
	@Override
	public void onOpen(SQLiteDatabase db) {
		super.onOpen(db);
	
		if (!db.isReadOnly()) {
			db.execSQL(
				// Enable foreign key constraints
				"PRAGMA foreign_keys=ON; " +

				// Allow triggers to trigger other triggers
				"PRAGMA recursive_triggers=ON; " +
				
				// Automatically garbage collect unused database pages 
				"PRAGMA auto_vacuum=FULL; ");
		}
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		// Automatically garbage collect unused database pages (must also be run before creating any tables)
		db.execSQL("PRAGMA auto_vacuum=FULL;");

		// Execute all the normal statements to rebuild database
		onUpgrade(db, 0, DB_SCHEMA_VERSION);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		for (int i = oldVersion; i < newVersion; i++) {
			for (String sql : _statements[i]) {
				db.execSQL(sql);
			}
		}
	}
	
	private String[][] _statements = new String[][] {
		// 1.0.0
		new String[] {
			"CREATE TABLE phonecall (" +
				"_id TEXT PRIMARY KEY, " +
				"phonenumber TEXT, " +
				"uri TEXT NOT NULL, " +
				"mimetype TEXT NOT NULL, " +
				"direction INTEGER NOT NULL, " +
				"duration INTEGER NOT NULL, " +
				"modified INTEGER NOT NULL)",

			"CREATE INDEX phonecall_modified_idx ON phonecall (modified DESC)",
		},
	};
}
