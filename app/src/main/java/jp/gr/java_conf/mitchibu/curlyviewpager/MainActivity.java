package jp.gr.java_conf.mitchibu.curlyviewpager;

import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
	private final PagerAdapter adapter = new PagerAdapter() {
		@Override
		public int getCount() {
			return cursor == null ? 0 : cursor.getCount();
		}

		@Override
		public CharSequence getPageTitle(int position) {
			cursor.moveToPosition(position);
			long time = cursor.getLong(cursor.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED));
			return new SimpleDateFormat("yyyy/MM/dd HH:ss", Locale.getDefault()).format(new Date(time * 1000));
		}

		@Override
		public boolean isViewFromObject(View view, Object o) {
			return view.equals(o);
		}

		@Override
		public Object instantiateItem(ViewGroup container, final int position) {
			cursor.moveToPosition(position);
			String data = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DATA));
			ImageView v = new ImageView(container.getContext());
			v.setImageURI(Uri.parse(data));
			v.setBackgroundColor(Color.WHITE);
			container.addView(v);
			return v;
		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			container.removeView((View)object);
		}
	};

	private Cursor cursor = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		ViewPager pager = (ViewPager)findViewById(R.id.pager);
		pager.setAdapter(adapter);

		getSupportLoaderManager().initLoader(0, null, this);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		getSupportLoaderManager().destroyLoader(0);
		onLoaderReset(null);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new CursorLoader(this, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, null, null, MediaStore.MediaColumns.DATE_ADDED + " desc");
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		cursor = data;
		adapter.notifyDataSetChanged();
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		if(cursor == null) return;
		cursor.close();
		cursor = null;
	}
}
