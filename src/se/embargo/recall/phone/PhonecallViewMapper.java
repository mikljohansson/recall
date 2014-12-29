package se.embargo.recall.phone;

import se.embargo.core.Contacts;
import se.embargo.core.Dates;
import se.embargo.core.databinding.IViewMapper;
import se.embargo.recall.Phonenumbers;
import se.embargo.recall.R;
import se.embargo.recall.database.Phonecall;
import android.content.ContentValues;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class PhonecallViewMapper implements IViewMapper<ContentValues> {
    public static final int ID_TAG = R.id.itemThumbnail;
    public static final int URI_TAG = R.id.itemTitle;
    public static final int MIMETYPE_TAG = R.id.itemModified;

    private final Contacts _contacts;
    
    public PhonecallViewMapper(Context context) {
		_contacts = new Contacts(context);
	}
    
    @Override
	public View convert(ContentValues item, View view, ViewGroup parent) {
    	if (view == null) {
			LayoutInflater inflater = (LayoutInflater)parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = inflater.inflate(R.layout.phonecall_listitem, parent, false);
		}
    	
    	String phonenumber = item.getAsString(Phonecall.PHONENUMBER);
    	Contacts.ContactDetails contact = _contacts.getPhonecallDetails(phonenumber);
    	
		ImageView thumbnailview = (ImageView)view.findViewById(R.id.itemThumbnail);
		thumbnailview.setImageResource(R.drawable.ic_list_phone_incoming);
		
		TextView titleview = (TextView)view.findViewById(R.id.itemTitle);
		TextView descriptionview = (TextView)view.findViewById(R.id.itemDescription);
		
		if (contact != null) {
			if (contact.photo != null) {
				thumbnailview.setImageBitmap(contact.photo);
			}
			
			titleview.setText(contact.name);
			descriptionview.setText(phonenumber);
			descriptionview.setVisibility(View.VISIBLE);
		}
		else if (Phonenumbers.isPrivateNumber(phonenumber)) {
			titleview.setText(R.string.phonecall_private_number);
			descriptionview.setVisibility(View.INVISIBLE);
		}
		else {
			titleview.setText(phonenumber);
			descriptionview.setVisibility(View.INVISIBLE);
		}

		// Bind the date
		TextView modifiedview = (TextView)view.findViewById(R.id.itemModified);
		modifiedview.setText(Dates.formatRelativeTimeSpan(item.getAsLong(Phonecall.MODIFIED)));
		
		view.setTag(ID_TAG, item.getAsString(Phonecall.ID));
		view.setTag(URI_TAG, item.getAsString(Phonecall.URI));
		view.setTag(MIMETYPE_TAG, item.getAsString(Phonecall.MIMETYPE));
		return view;
	}
	
	@Override
	public int getItemViewType(ContentValues item) {
		return 0;
	}
	
	@Override
	public int getViewTypeCount() {
		return 1;
	}
}
