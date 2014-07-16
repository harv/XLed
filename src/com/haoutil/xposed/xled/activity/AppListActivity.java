package com.haoutil.xposed.xled.activity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.haoutil.xposed.xled.R;
import com.haoutil.xposed.xled.model.AppListItem;
import com.haoutil.xposed.xled.util.SettingsHelper;

public class AppListActivity extends Activity {
	private SettingsHelper settingsHelper;
	
	private ListView mListView;
	private List<AppListItem> appList = new ArrayList<AppListItem>();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.app_list);
		
		settingsHelper = new SettingsHelper(AppListActivity.this.getApplicationContext());
		
		mListView = (ListView) findViewById(R.id.lv_applist);
		mListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Intent intent = new Intent(AppListActivity.this.getApplicationContext(), AppItemActivity.class);
				intent.putExtra("appName", ((TextView) view.findViewById(R.id.tv_name)).getText().toString());
				intent.putExtra("packageName", ((TextView) view.findViewById(R.id.tv_packagename)).getText().toString());
				AppListActivity.this.startActivityForResult(intent, position);
			}
		});
		
		new loadAppListAdapterTask().execute();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if (this.mListView != null) {
			AppListItem app = (AppListItem) this.mListView.getAdapter().getItem(requestCode);
			app.setEnable(data.getBooleanExtra("enable", false));
			app.setColor(data.getIntExtra("color", Color.TRANSPARENT));
			
			AppListActivity.this.sort();
			
			((AppListAdapter) this.mListView.getAdapter()).notifyDataSetChanged();
		}
	}
	
	public void loadApps(ProgressDialog dialog) {
		PackageManager packageManager = getPackageManager();
		List<ApplicationInfo> list = packageManager.getInstalledApplications(0);
		dialog.setMax(list.size());
		for (int i = 0; i < list.size(); i++) {
			ApplicationInfo appInfo = list.get(i);
			if (!appInfo.packageName.equals("com.haoutil.xposed.xled")) {
				AppListActivity.this.appList.add(new AppListItem(
						settingsHelper.getBoolean("pref_app_enable_" + appInfo.packageName, false),
						appInfo.loadIcon(packageManager),
						appInfo.loadLabel(packageManager).toString(),
						appInfo.packageName,
						settingsHelper.getInt("pref_app_color_" + appInfo.packageName, Color.TRANSPARENT))
				);
			}
			dialog.setProgress(i + 1);
		}
		
		AppListActivity.this.sort();
	}
	
	private void sort() {
		Collections.sort(AppListActivity.this.appList, new Comparator<AppListItem>() {
			@Override
			public int compare(AppListItem app1, AppListItem app2) {
				int i = app1.getColor() - app2.getColor();
				if (i == 0) {
					i = app1.getName().compareTo(app2.getName());
				}
				
				return i;
			}
		});
	}
	
	private class loadAppListAdapterTask extends AsyncTask<Void, Void, AppListAdapter> {
		ProgressDialog dialog;
		
		private loadAppListAdapterTask() {};
		
		@Override
		protected void onPreExecute() {
			this.dialog = new ProgressDialog(AppListActivity.this.mListView.getContext());
			this.dialog.setMessage(AppListActivity.this.getString(R.string.load_apps_title));
			this.dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			this.dialog.setCancelable(false);
			this.dialog.show();
		}
		
		@Override
		protected AppListAdapter doInBackground(Void... paramVarArgs) {
			if (AppListActivity.this.appList.size() == 0) {
				AppListActivity.this.loadApps(this.dialog);
			}
			return null;
		}
		@Override
		protected void onPostExecute(AppListAdapter result) {
			AppListAdapter adapter = new AppListAdapter(AppListActivity.this.getLayoutInflater(), AppListActivity.this.appList);
			AppListActivity.this.mListView.setAdapter(adapter);
			AppListActivity.this.mListView.setFastScrollEnabled(true);
			try {
				this.dialog.dismiss();
				return;
			} catch (Exception localException) {
			}
		}
	}
	
	private class AppListAdapter extends BaseAdapter {
		private LayoutInflater inflater;
		private List<AppListItem> list;
	
		public AppListAdapter(LayoutInflater inflater, List<AppListItem> list) {
			this.inflater = inflater;
			this.list = list;
		}
	
		@Override
		public int getCount() {
			return list.size();
		}
	
		@Override
		public Object getItem(int position) {
			if (position < list.size()) {
				return list.get(position);
			} else {
				return null;
			}
		}
	
		@Override
		public long getItemId(int position) {
			return -1;
		}
	
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			AppListItem item = list.get(position);
			View view = null;
			if (convertView != null) {
				view = convertView;
			} else {
				view = inflater.inflate(R.layout.app_list_item, parent, false);
			}
			ItemViewHolder holder = (ItemViewHolder) view.getTag();
			if (holder == null) {
				holder = new ItemViewHolder();
				
				holder.ll_item = (LinearLayout) view.findViewById(R.id.ll_item);
				holder.iv_icon = (ImageView) view.findViewById(R.id.iv_icon);
				holder.tv_name = (TextView) view.findViewById(R.id.tv_name);
				holder.tv_packagename = (TextView) view.findViewById(R.id.tv_packagename);
				holder.cb_enable = (CheckBox) view.findViewById(R.id.cb_enable);
				
				view.setTag(holder);
			}
			
			if (item != null) {
				holder.ll_item.setBackgroundColor(item.getColor());
				holder.iv_icon.setImageDrawable(item.getIcon());
				holder.tv_name.setText(item.getName());
				holder.tv_packagename.setText(item.getPackageName());
				holder.cb_enable.setChecked(item.isEnable());
			}
			
			return view;
		}
		
		private class ItemViewHolder {
			private LinearLayout ll_item;
			public ImageView iv_icon;
			private TextView tv_name;
			private TextView tv_packagename;
			private CheckBox cb_enable;
		}
	}
}
