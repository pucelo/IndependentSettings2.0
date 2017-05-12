package android.preference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.preference.MiuiPreference.OnMiuiPreferenceChangeInternalListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;

public class MiuiPreferenceGroupAdapter extends BaseAdapter implements OnMiuiPreferenceChangeInternalListener{
    private static final String TAG = "MiuiPreferenceGroupAdapter";
    private MiuiPreferenceGroup mPreferenceGroup;
    private List<MiuiPreference> mPreferenceList;
    private ArrayList<MiuiPreferenceLayout> mPreferenceLayouts;
    private MiuiPreferenceLayout mTempPreferenceLayout = new MiuiPreferenceLayout();
    private boolean mHasReturnedViewTypeCount = false;

    private volatile boolean mIsSyncing = false;

    private Handler mHandler = new Handler();

    private Runnable mSyncRunnable = new Runnable() {
        public void run() {
            syncMyPreferences();
        }
    };

    private int mHighlightedPosition = -1;
    private Drawable mHighlightedDrawable;

    private static ViewGroup.LayoutParams sWrapperLayoutParams = new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

    private static class MiuiPreferenceLayout implements Comparable<MiuiPreferenceLayout> {
        private int resId;
        private int widgetResId;
        private String name;

        public int compareTo(MiuiPreferenceLayout other) {
            int compareNames = name.compareTo(other.name);
            if (compareNames == 0) {
                if (resId == other.resId) {
                    if (widgetResId == other.widgetResId) {
                        return 0;
                    } else {
                        return widgetResId - other.widgetResId;
                    }
                } else {
                    return resId - other.resId;
                }
            } else {
                return compareNames;
            }
        }
    }

    public MiuiPreferenceGroupAdapter(MiuiPreferenceGroup preferenceGroup) {
        mPreferenceGroup = preferenceGroup;
        // If this group gets or loses any children, let us know
        mPreferenceGroup.setOnMiuiPreferenceChangeInternalListener(this);

        mPreferenceList = new ArrayList<MiuiPreference>();
        mPreferenceLayouts = new ArrayList<MiuiPreferenceLayout>();

        syncMyPreferences();
    }

    private void syncMyPreferences() {
        synchronized(this) {
            if (mIsSyncing) {
                return;
            }

            mIsSyncing = true;
        }

        List<MiuiPreference> newPreferenceList = new ArrayList<MiuiPreference>(mPreferenceList.size());
        flattenPreferenceGroup(newPreferenceList, mPreferenceGroup);
        mPreferenceList = newPreferenceList;

        notifyDataSetChanged();

        synchronized(this) {
            mIsSyncing = false;
            notifyAll();
        }
    }

    private void flattenPreferenceGroup(List<MiuiPreference> preferences, MiuiPreferenceGroup group) {
        // TODO: shouldn't always?
        group.sortPreferences();

        final int groupSize = group.getPreferenceCount();
        for (int i = 0; i < groupSize; i++) {
            final MiuiPreference preference = group.getMiuiPreference(i);

            preferences.add(preference);

            if (!mHasReturnedViewTypeCount && preference.canRecycleLayout()) {
                addPreferenceClassName(preference);
            }

            if (preference instanceof MiuiPreferenceGroup) {
                final MiuiPreferenceGroup preferenceAsGroup = (MiuiPreferenceGroup) preference;
                if (preferenceAsGroup.isOnSameScreenAsChildren()) {
                    flattenPreferenceGroup(preferences, preferenceAsGroup);
                }
            }

            preference.setOnMiuiPreferenceChangeInternalListener(this);
        }
    }

    private MiuiPreferenceLayout createPreferenceLayout(MiuiPreference preference, MiuiPreferenceLayout in) {
        MiuiPreferenceLayout pl = in != null? in : new MiuiPreferenceLayout();
        pl.name = preference.getClass().getName();
        pl.resId = preference.getLayoutResource();
        pl.widgetResId = preference.getWidgetLayoutResource();
        return pl;
    }

    private void addPreferenceClassName(MiuiPreference preference) {
        final MiuiPreferenceLayout pl = createPreferenceLayout(preference, null);
        int insertPos = Collections.binarySearch(mPreferenceLayouts, pl);

        // Only insert if it doesn't exist (when it is negative).
        if (insertPos < 0) {
            // Convert to insert index
            insertPos = insertPos * -1 - 1;
            mPreferenceLayouts.add(insertPos, pl);
        }
    }

    public int getCount() {
        return mPreferenceList.size();
    }

    public MiuiPreference getItem(int position) {
        if (position < 0 || position >= getCount()) return null;
        return mPreferenceList.get(position);
    }

    public long getItemId(int position) {
        if (position < 0 || position >= getCount()) return ListView.INVALID_ROW_ID;
        return this.getItem(position).getId();
    }

    public void setHighlighted(int position) {
        mHighlightedPosition = position;
    }

    public void setHighlightedDrawable(Drawable drawable) {
        mHighlightedDrawable = drawable;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        final MiuiPreference preference = this.getItem(position);
        // Build a PreferenceLayout to compare with known ones that are cacheable.
        mTempPreferenceLayout = createPreferenceLayout(preference, mTempPreferenceLayout);

        // If it's not one of the cached ones, set the convertView to null so that
        // the layout gets re-created by the Preference.
        if (Collections.binarySearch(mPreferenceLayouts, mTempPreferenceLayout) < 0 ||
                (getItemViewType(position) == getHighlightItemViewType())) {
            convertView = null;
        }
        View result = preference.getView(convertView, parent);
        if (position == mHighlightedPosition && mHighlightedDrawable != null) {
            ViewGroup wrapper = new FrameLayout(parent.getContext());
            wrapper.setLayoutParams(sWrapperLayoutParams);
            wrapper.setBackgroundDrawable(mHighlightedDrawable);
            wrapper.addView(result);
            result = wrapper;
        }
        return result;
    }

    @Override
    public boolean isEnabled(int position) {
        if (position < 0 || position >= getCount()) return true;
        return this.getItem(position).isSelectable();
    }

    @Override
    public boolean areAllItemsEnabled() {
        // There should always be a preference group, and these groups are always
        // disabled
        return false;
    }

    public void onMiuiPreferenceChange(MiuiPreference preference) {
        notifyDataSetChanged();
    }

    public void onMiuiPreferenceHierarchyChange(MiuiPreference preference) {
        mHandler.removeCallbacks(mSyncRunnable);
        mHandler.post(mSyncRunnable);
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    private int getHighlightItemViewType() {
        return getViewTypeCount() - 1;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == mHighlightedPosition) {
            return getHighlightItemViewType();
        }

        if (!mHasReturnedViewTypeCount) {
            mHasReturnedViewTypeCount = true;
        }

        final MiuiPreference preference = this.getItem(position);
        if (!preference.canRecycleLayout()) {
            return IGNORE_ITEM_VIEW_TYPE;
        }

        mTempPreferenceLayout = createPreferenceLayout(preference, mTempPreferenceLayout);

        int viewType = Collections.binarySearch(mPreferenceLayouts, mTempPreferenceLayout);
        if (viewType < 0) {
            // This is a class that was seen after we returned the count, so
            // don't recycle it.
            return IGNORE_ITEM_VIEW_TYPE;
        } else {
            return viewType;
        }
    }

    @Override
    public int getViewTypeCount() {
        if (!mHasReturnedViewTypeCount) {
            mHasReturnedViewTypeCount = true;
        }

        return Math.max(1, mPreferenceLayouts.size()) + 1;
    }

}
