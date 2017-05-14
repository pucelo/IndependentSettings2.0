package android.preference;


import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SectionIndexer;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class MiuiLaunchPreference extends MiuiDialogPreference implements AdapterView.OnItemClickListener {
    private Context mContext;
    private PackageManager mPackageManager;
    private ListView mListView;
    private ProgressBar mProgressBar;
    private AppListAdapter mAppListAdapter;
    private LoadApplication mLoadApplication;

    public MiuiLaunchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialization(context);
    }

    private void initialization(Context context) {
        mContext = context;
        mPackageManager = context.getPackageManager();
        setDialogLayoutResource(LayoutToID("launch_preference_layout"));
        setWidgetLayoutResource(LayoutToID("launch_preference_app_icon"));
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        String value = getStringValue();
        ImageView prefAppIcon = (ImageView) view.findViewById(IDtoID("iconForApp"));
        prefAppIcon.setImageDrawable(getAppIcon(value));
        setSummary(value != null ? getAppName(value) : "");
    }

    private String getAppName(String value) {
        String appName = null;
        if (value != null) {
            try {
                appName = (String) mPackageManager.getApplicationLabel(mPackageManager.getApplicationInfo(value, mPackageManager.GET_META_DATA));
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }

        return appName;
    }

    private Drawable getAppIcon(String value) {
        Drawable appIcon;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            appIcon = mContext.getResources().getDrawable(com.android.internal.R.mipmap.sym_def_app_icon, null);
        } else {

            appIcon = mContext.getResources().getDrawable(com.android.internal.R.mipmap.sym_def_app_icon);
        }
        if (value != null) {
            try {
                appIcon = this.mPackageManager.getApplicationIcon(value);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
        return appIcon;

    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        super.onSetInitialValue(restorePersistedValue, defaultValue);
        if (restorePersistedValue) {
            String value = getStringValue();
            setSummary(value != null ? getAppName(value) : "");
        } else {
            setStringValue((String) defaultValue);
            setSummary(getSummary());

        }
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);
        AlertDialog dialog = (AlertDialog) getDialog();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        dialog.show();
        Button ok = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        ok.setVisibility(View.GONE);

    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (mLoadApplication != null && mLoadApplication.getStatus() == AsyncTask.Status.RUNNING) {
            mLoadApplication.cancel(true);
            mLoadApplication = null;
        }

    }

    @Override
    protected void onBindDialogView(View view) {

        super.onBindDialogView(view);

        mListView = (ListView) view.findViewById(IDtoID("appsList"));
        mListView.setOnItemClickListener(this);
        mListView.setFastScrollEnabled(false);
        mListView.setFadingEdgeLength(1);
        mListView.setDivider(null);
        mListView.setDividerHeight(0);
        mListView.setScrollingCacheEnabled(false);
        mProgressBar = (ProgressBar) view.findViewById(IDtoID("progressBar"));
        EditText search = (EditText) view.findViewById(IDtoID("searchApp"));
        createList();
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (mAppListAdapter != null) {
                    mAppListAdapter.getFilter().filter(s);
                }

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

    }

    private void createList() {
        LoadApplication mLoadApplication = new LoadApplication();
        mLoadApplication.execute();
        mProgressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        AppInfo appInfo = (AppInfo) parent.getItemAtPosition(position);
        Intent intent = appInfo.mIntent;
        ResolveInfo ri = mPackageManager.resolveActivity(intent, 0);
        String intentString = String.format("%1$s%2$s", appInfo.mPackageName, ri.activityInfo.name);
        setSummary(intentString == null ? "" : appInfo.mAppName);
        //persistString(appInfo.mPackageName);
        setStringValue(appInfo.mPackageName);
        getDialog().dismiss();

    }

    @Override
    protected boolean persistString(String value) {

        //Toast.makeText(mContext,getKey()+' '+value,Toast.LENGTH_LONG).show();
        return super.persistString(value);
    }

    private class LoadApplication extends AsyncTask<Void, Void, List<AppInfo>> {
        //private class LoadApplication extends AsyncTask<Void, Void, Void> {

        @Override
        protected List<AppInfo> doInBackground(Void... voids) {
            //protected Void doInBackground(Void... params) {
            ArrayList appList = new ArrayList<>();
            Intent intent = new Intent(Intent.ACTION_MAIN, null);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> resolveInfoList = mPackageManager.queryIntentActivities(intent, 0);
            for (ResolveInfo resolveInfo : resolveInfoList) {
                AppInfo appInfo = new AppInfo();
                appInfo.mAppName = resolveInfo.activityInfo.loadLabel(mPackageManager).toString();
                appInfo.mIcon = resolveInfo.activityInfo.loadIcon(mPackageManager);
                appInfo.mPackageName = resolveInfo.activityInfo.packageName;
                Intent explicitIntent = new Intent();
                explicitIntent.setComponent(new ComponentName(appInfo.mPackageName, resolveInfo.activityInfo.name));
                appInfo.mIntent = explicitIntent;
                appList.add(appInfo);
            }


            return appList;
        }

        @Override
        protected void onPostExecute(List<AppInfo> appInfo) {
            super.onPostExecute(appInfo);
            mAppListAdapter = new AppListAdapter(appInfo);
            mProgressBar.setVisibility(View.GONE);
            mListView.setAdapter(mAppListAdapter);


        }
    }

    public class AppInfo {
        public String mAppName;
        public String mPackageName;
        public Drawable mIcon;
        public Intent mIntent;
    }

    private class AppListAdapter extends BaseAdapter implements SectionIndexer, Filterable {

        List<AppInfo> mAppList, filteredList;
        private HashMap<String, Integer> alphaIndexer;
        private String[] sections;

        AppListAdapter(List<AppInfo> appList) {

            this.mAppList = appList;
            filteredList = mAppList;
            //adding Indexer to display the first letter of an app while using fast scroll
            alphaIndexer = new HashMap<>();
            for (int i = 0; i < filteredList.size(); i++) {
                String s = filteredList.get(i).mAppName;
                String s1 = s.substring(0, 1).toUpperCase();
                if (!alphaIndexer.containsKey(s1))
                    alphaIndexer.put(s1, i);
            }

            Set<String> sectionLetters = alphaIndexer.keySet();
            ArrayList<String> sectionList = new ArrayList<>(sectionLetters);
            Collections.sort(sectionList);
            sections = new String[sectionList.size()];
            for (int i = 0; i < sectionList.size(); i++)
                sections[i] = sectionList.get(i);

        }

        @Override
        public Object[] getSections() {
            return sections;
        }

        @Override
        public int getPositionForSection(int sectionIndex) {
            return alphaIndexer.get(sections[sectionIndex]);
        }

        @Override
        public int getSectionForPosition(int position) {
            for (int i = sections.length - 1; i >= 0; i--) {
                if (position >= alphaIndexer.get(sections[i])) {
                    return i;
                }
            }
            return 0;
        }

        @Override
        public Filter getFilter() {
            return new Filter() {

                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults fr = new FilterResults();
                    ArrayList<AppInfo> ai = new ArrayList<>();

                    for (int i = 0; i < mAppList.size(); i++) {
                        String label = mAppList.get(i).mAppName;
                        if (label.toLowerCase().contains(constraint.toString().toLowerCase())) {
                            ai.add(mAppList.get(i));
                        }
                    }

                    fr.count = ai.size();
                    fr.values = ai;

                    return fr;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    filteredList = (List<AppInfo>) results.values;
                    notifyDataSetChanged();
                }
            };
        }

        @Override
        public int getCount() {
            if (filteredList != null) {
                return filteredList.size();
            }
            return 0;
        }

        @Override
        public AppInfo getItem(int position) {
            if (filteredList != null) {
                return filteredList.get(position);
            }
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(mContext);
                convertView = inflater.inflate(LayoutToID("launch_preference_item"), parent, false);
                ViewHolder viewHolder = new ViewHolder();
                viewHolder.mAppNames = (TextView) convertView.findViewById(IDtoID("appName"));
                viewHolder.mAppPackage = (TextView) convertView.findViewById(IDtoID("appPackage"));
                viewHolder.mAppIcon = (ImageView) convertView.findViewById(IDtoID("appIcon"));
                convertView.setTag(viewHolder);
            }
            final ViewHolder holder = (ViewHolder) convertView.getTag();
            final AppInfo appInfo = filteredList.get(position);

            holder.mAppNames.setText(appInfo.mAppName);
            holder.mAppPackage.setText(appInfo.mPackageName);
            holder.mAppIcon.setImageDrawable(appInfo.mIcon);

            return convertView;
        }

        class ViewHolder {
            TextView mAppNames;
            TextView mAppPackage;
            ImageView mAppIcon;
        }
    }


}
