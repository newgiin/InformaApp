package org.witness.iwitness.utils.adapters;

import java.util.List;

import org.witness.iwitness.R;
import org.witness.iwitness.utils.Constants.MainFragmentListener;
import org.witness.iwitness.models.Media;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

public class GalleryGridAdapter extends BaseAdapter {
	List<Media> media;
	LayoutInflater li;
	Activity a;
	
	public GalleryGridAdapter(Activity a, List<Media> media) {
		this.media = media;
		this.a = a;
		li = LayoutInflater.from(a);
	}
	
	@Override
	public int getCount() {
		return media.size();
	}

	@Override
	public Object getItem(int position) {
		return media.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		View view = li.inflate(R.layout.adapter_gallery_grid, null);
		
		ImageView iv = (ImageView) view.findViewById(R.id.gallery_thumb);
		iv.setImageBitmap(media.get(position).bitmapThumb);
		iv.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				((MainFragmentListener) a).launchEditor(media.get(position)._id);
			}
			
		});
		
		return view;
	}

}
