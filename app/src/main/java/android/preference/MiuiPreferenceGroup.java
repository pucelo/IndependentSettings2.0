package android.preference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.AttributeSet;

public abstract class MiuiPreferenceGroup extends MiuiPreference implements GenericInflater.Parent<MiuiPreference>  {

    private List<MiuiPreference> mPreferenceList;
    private boolean mOrderingAsAdded = true;
    private int mCurrentPreferenceOrder = 0;
    private boolean mAttachedToActivity = false;

    public MiuiPreferenceGroup(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MiuiPreferenceGroup(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public MiuiPreferenceGroup(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        mPreferenceList = new ArrayList<MiuiPreference>();

        final TypedArray a = context.obtainStyledAttributes(
                attrs, com.android.internal.R.styleable.PreferenceGroup, defStyleAttr, defStyleRes);
        mOrderingAsAdded = a.getBoolean(com.android.internal.R.styleable.PreferenceGroup_orderingFromXml,
                mOrderingAsAdded);
        a.recycle();
    }

    public void setOrderingAsAdded(boolean orderingAsAdded) {
        mOrderingAsAdded = orderingAsAdded;
    }

    public boolean isOrderingAsAdded() {
        return mOrderingAsAdded;
    }

    public void addItemFromInflater(MiuiPreference preference) {
        addPreference(preference);
    }

    public int getPreferenceCount() {
        return mPreferenceList.size();
    }

    public MiuiPreference getMiuiPreference(int index) {
        return mPreferenceList.get(index);
    }

    public boolean addPreference(MiuiPreference preference) {
        if (mPreferenceList.contains(preference)) {
            // Exists
            return true;
        }

        if (preference.getOrder() == MiuiPreference.DEFAULT_ORDER) {
            if (mOrderingAsAdded) {
                preference.setOrder(mCurrentPreferenceOrder++);
            }

            if (preference instanceof MiuiPreferenceGroup) {
                // TODO: fix (method is called tail recursively when inflating,
                // so we won't end up properly passing this flag down to children
                ((MiuiPreferenceGroup)preference).setOrderingAsAdded(mOrderingAsAdded);
            }
        }

        int insertionIndex = Collections.binarySearch(mPreferenceList, preference);
        if (insertionIndex < 0) {
            insertionIndex = insertionIndex * -1 - 1;
        }

        if (!onPrepareAddPreference(preference)) {
            return false;
        }

        synchronized(this) {
            mPreferenceList.add(insertionIndex, preference);
        }

        preference.onAttachedToHierarchy(getMiuiPreferenceManager());

        if (mAttachedToActivity) {
            preference.onAttachedToActivity();
        }

        notifyHierarchyChanged();

        return true;
    }

    public boolean removePreference(MiuiPreference preference) {
        final boolean returnValue = removePreferenceInt(preference);
        notifyHierarchyChanged();
        return returnValue;
    }

    private boolean removePreferenceInt(MiuiPreference preference) {
        synchronized(this) {
            preference.onPrepareForRemoval();
            return mPreferenceList.remove(preference);
        }
    }

    public void removeAll() {
        synchronized(this) {
            List<MiuiPreference> preferenceList = mPreferenceList;
            for (int i = preferenceList.size() - 1; i >= 0; i--) {
                removePreferenceInt(preferenceList.get(0));
            }
        }
        notifyHierarchyChanged();
    }

    protected boolean onPrepareAddPreference(MiuiPreference preference) {
        preference.onParentChanged(this, shouldDisableDependents());
        return true;
    }

    public MiuiPreference findPreference(CharSequence key) {
        if (TextUtils.equals(getKey(), key)) {
            return this;
        }
        final int preferenceCount = getPreferenceCount();
        for (int i = 0; i < preferenceCount; i++) {
            final MiuiPreference preference = getMiuiPreference(i);
            final String curKey = preference.getKey();

            if (curKey != null && curKey.equals(key)) {
                return preference;
            }

            if (preference instanceof MiuiPreferenceGroup) {
                final MiuiPreference returnedPreference = ((MiuiPreferenceGroup)preference)
                        .findPreference(key);
                if (returnedPreference != null) {
                    return returnedPreference;
                }
            }
        }

        return null;
    }

    protected boolean isOnSameScreenAsChildren() {
        return true;
    }

    @Override
    protected void onAttachedToActivity() {
        super.onAttachedToActivity();

        // Mark as attached so if a preference is later added to this group, we
        // can tell it we are already attached
        mAttachedToActivity = true;

        // Dispatch to all contained preferences
        final int preferenceCount = getPreferenceCount();
        for (int i = 0; i < preferenceCount; i++) {
            getMiuiPreference(i).onAttachedToActivity();
        }
    }

    @Override
    protected void onPrepareForRemoval() {
        super.onPrepareForRemoval();

        // We won't be attached to the activity anymore
        mAttachedToActivity = false;
    }

    @Override
    public void notifyDependencyChange(boolean disableDependents) {
        super.notifyDependencyChange(disableDependents);

        // Child preferences have an implicit dependency on their containing
        // group. Dispatch dependency change to all contained preferences.
        final int preferenceCount = getPreferenceCount();
        for (int i = 0; i < preferenceCount; i++) {
            getMiuiPreference(i).onParentChanged(this, disableDependents);
        }
    }

    void sortPreferences() {
        synchronized (this) {
            Collections.sort(mPreferenceList);
        }
    }

    @Override
    protected void dispatchSaveInstanceState(Bundle container) {
        super.dispatchSaveInstanceState(container);

        // Dispatch to all contained preferences
        final int preferenceCount = getPreferenceCount();
        for (int i = 0; i < preferenceCount; i++) {
            getMiuiPreference(i).dispatchSaveInstanceState(container);
        }
    }

    @Override
    protected void dispatchRestoreInstanceState(Bundle container) {
        super.dispatchRestoreInstanceState(container);

        // Dispatch to all contained preferences
        final int preferenceCount = getPreferenceCount();
        for (int i = 0; i < preferenceCount; i++) {
            getMiuiPreference(i).dispatchRestoreInstanceState(container);
        }
    }

}
