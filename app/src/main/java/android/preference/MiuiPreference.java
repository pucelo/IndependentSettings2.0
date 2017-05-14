package android.preference;


import android.content.Context;
import com.android.internal.util.CharSequences;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.Settings;
import android.annotation.CallSuper;
import android.annotation.DrawableRes;
import android.annotation.LayoutRes;
import android.annotation.StringRes;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.AbsSavedState;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MiuiPreference implements Comparable<MiuiPreference>{

    public static final  int DEFAULT_ORDER = Integer.MAX_VALUE;
    private Context mContext;
    private MiuiPreferenceManager mMiuiPreferenceManager;
    private long mId;
    private OnMiuiPreferenceChangeListener mOnChangeListener;
    private OnMiuiPreferenceClickListener mOnClickListener;
    private int mOrder = DEFAULT_ORDER;
    private CharSequence mTitle;
    private int mTitleRes;
    private CharSequence mSummary;
    private int mIconResId;
    private int mStorageType;
    private Drawable mIcon;
    private String mKey;
    private Intent mIntent;
    private String mFragment;
    private Bundle mExtras;
    private boolean mEnabled = true;
    private boolean mSelectable = true;
    private boolean mRequiresKey;
    private boolean mPersistent = true;
    private String mDependencyKey;
    private Object mDefaultValue;
    private boolean mDependencyMet = true;
    private boolean mParentDependencyMet = true;
    private boolean mShouldDisableView = true;
    private int mLayoutResId = com.android.internal.R.layout.preference;
    private int mWidgetLayoutResId;
    private boolean mCanRecycleLayout = true;
    private OnMiuiPreferenceChangeInternalListener mListener;
    private List<MiuiPreference> mDependents;

    private boolean mBaseMethodCalled;

    @Override
    public int compareTo(MiuiPreference another) {
        if (mOrder != another.mOrder) {
            // Do order comparison
            return mOrder - another.mOrder;
        } else if (mTitle == another.mTitle) {
            // If titles are null or share same object comparison
            return 0;
        } else if (mTitle == null) {
            return 1;
        } else if (another.mTitle == null) {
            return -1;
        } else {
            // Do name comparison

            return CharSequences.compareToIgnoreCase(mTitle, another.mTitle);
        }
    }

    public interface OnMiuiPreferenceChangeListener {
        boolean onMiuiPreferenceChange(MiuiPreference preference, Object newValue);
    }
    public interface OnMiuiPreferenceClickListener {
        boolean onMiuiPreferenceClick(MiuiPreference preference);
    }
    interface OnMiuiPreferenceChangeInternalListener {
        void onMiuiPreferenceChange(MiuiPreference preference);
        void onMiuiPreferenceHierarchyChange(MiuiPreference preference);
    }

    public MiuiPreference(Context context) {
        this(context, null);
    }

    public MiuiPreference(Context context, AttributeSet attrs) {
        this(context, attrs, context.getResources().getIdentifier("preferenceStyle", "attr", context.getPackageName()));
    }

    public MiuiPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public MiuiPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        mContext = context;
        final TypedArray a = context.obtainStyledAttributes(
                attrs, com.android.internal.R.styleable.Preference, defStyleAttr, defStyleRes);
        for (int i = a.getIndexCount() - 1; i >= 0; i--) {
            int attr = a.getIndex(i);
            switch (attr) {
                case com.android.internal.R.styleable.Preference_icon:
                    mIconResId = a.getResourceId(attr, 0);
                    break;

                case com.android.internal.R.styleable.Preference_key:
                    mKey = a.getString(attr);
                    break;

                case com.android.internal.R.styleable.Preference_title:
                    mTitleRes = a.getResourceId(attr, 0);
                    mTitle = a.getString(attr);
                    break;

                case com.android.internal.R.styleable.Preference_summary:
                    mSummary = a.getString(attr);
                    break;

                case com.android.internal.R.styleable.Preference_order:
                    mOrder = a.getInt(attr, mOrder);
                    break;

                case com.android.internal.R.styleable.Preference_fragment:
                    mFragment = a.getString(attr);
                    break;

                case com.android.internal.R.styleable.Preference_layout:
                    mLayoutResId = a.getResourceId(attr, mLayoutResId);
                    break;

                case com.android.internal.R.styleable.Preference_widgetLayout:
                    mWidgetLayoutResId = a.getResourceId(attr, mWidgetLayoutResId);
                    break;

                case com.android.internal.R.styleable.Preference_enabled:
                    mEnabled = a.getBoolean(attr, true);
                    break;

                case com.android.internal.R.styleable.Preference_selectable:
                    mSelectable = a.getBoolean(attr, true);
                    break;

                case com.android.internal.R.styleable.Preference_persistent:
                    mPersistent = a.getBoolean(attr, mPersistent);
                    break;

                case com.android.internal.R.styleable.Preference_dependency:
                    mDependencyKey = a.getString(attr);
                    break;

                case com.android.internal.R.styleable.Preference_defaultValue:
                    mDefaultValue = onGetDefaultValue(a, attr);
                    break;

                case com.android.internal.R.styleable.Preference_shouldDisableView:
                    mShouldDisableView = a.getBoolean(attr, mShouldDisableView);
                    break;
            }
        }
        a.recycle();

        if (!getClass().getName().startsWith("android.preference.independentsettings")
                && !getClass().getName().startsWith("com.android")) {
            // For non-framework subclasses, assume the worst and don't cache views.
            mCanRecycleLayout = false;
        }
    }

    protected Object onGetDefaultValue(TypedArray a, int index) {
            return null;
    }

    public void setIntent(Intent intent) {
        mIntent = intent;
    }

    public Intent getIntent() {
        return mIntent;
    }

    public void setFragment(String fragment) {
        mFragment = fragment;
    }

    public String getFragment() {
        return mFragment;
    }

    public Bundle getExtras() {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        return mExtras;
    }

    public Bundle peekExtras() {
        return mExtras;
    }

    public void setLayoutResource(@LayoutRes int layoutResId) {
        if (layoutResId != mLayoutResId) {
            // Layout changed
            mCanRecycleLayout = false;
        }

        mLayoutResId = layoutResId;
    }

    @LayoutRes
    public int getLayoutResource() {
        return mLayoutResId;
    }

    public void setWidgetLayoutResource(@LayoutRes int widgetLayoutResId) {
        if (widgetLayoutResId != mWidgetLayoutResId) {
            // Layout changed
            mCanRecycleLayout = false;
        }
        mWidgetLayoutResId = widgetLayoutResId;
    }

    @LayoutRes
    public int getWidgetLayoutResource() {
        return mWidgetLayoutResId;
    }

    public View getView(View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = onCreateView(parent);
        }
        onBindView(convertView);
        return convertView;
    }

    @CallSuper
    protected View onCreateView(ViewGroup parent) {
        final LayoutInflater layoutInflater =
                (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        final View layout = layoutInflater.inflate(mLayoutResId, parent, false);

        final ViewGroup widgetFrame = (ViewGroup) layout
                .findViewById(com.android.internal.R.id.widget_frame);
        if (widgetFrame != null) {
            if (mWidgetLayoutResId != 0) {
                layoutInflater.inflate(mWidgetLayoutResId, widgetFrame);
            } else {
                widgetFrame.setVisibility(View.GONE);
            }
        }
        return layout;
    }

    @CallSuper
    protected void onBindView(View view) {
        final TextView titleView = (TextView) view.findViewById(com.android.internal.R.id.title);
        if (titleView != null) {
            final CharSequence title = getTitle();
            if (!TextUtils.isEmpty(title)) {
                titleView.setText(title);
                titleView.setVisibility(View.VISIBLE);
            } else {
                titleView.setVisibility(View.GONE);
            }
        }

        final TextView summaryView = (TextView) view.findViewById(
                com.android.internal.R.id.summary);
        if (summaryView != null) {
            final CharSequence summary = getSummary();
            if (!TextUtils.isEmpty(summary)) {
                summaryView.setText(summary);
                summaryView.setVisibility(View.VISIBLE);
            } else {
                summaryView.setVisibility(View.GONE);
            }
        }

        final ImageView imageView = (ImageView) view.findViewById(com.android.internal.R.id.icon);
        if (imageView != null) {
            if (mIconResId != 0 || mIcon != null) {
                if (mIcon == null) {
                    mIcon = getContext().getDrawable(mIconResId);
                }
                if (mIcon != null) {
                    imageView.setImageDrawable(mIcon);
                }
            }
            imageView.setVisibility(mIcon != null ? View.VISIBLE : View.GONE);
        }

        final View imageFrame = view.findViewById(com.android.internal.R.id.icon_frame);
        if (imageFrame != null) {
            imageFrame.setVisibility(mIcon != null ? View.VISIBLE : View.GONE);
        }

        if (mShouldDisableView) {
            setEnabledStateOnViews(view, isEnabled());
        }
    }

    private void setEnabledStateOnViews(View v, boolean enabled) {
        v.setEnabled(enabled);

        if (v instanceof ViewGroup) {
            final ViewGroup vg = (ViewGroup) v;
            for (int i = vg.getChildCount() - 1; i >= 0; i--) {
                setEnabledStateOnViews(vg.getChildAt(i), enabled);
            }
        }
    }

    public void setOrder(int order) {
        if (order != mOrder) {
            mOrder = order;

            // Reorder the list
            notifyHierarchyChanged();
        }
    }

    public int getOrder() {
        return mOrder;
    }

    public void setTitle(CharSequence title) {
        if (title == null && mTitle != null || title != null && !title.equals(mTitle)) {
            mTitleRes = 0;
            mTitle = title;
            notifyChanged();
        }
    }

    public void setTitle(@StringRes int titleResId) {
        setTitle(mContext.getString(titleResId));
        mTitleRes = titleResId;
    }

    @StringRes
    public int getTitleRes() {
        return mTitleRes;
    }

    public CharSequence getTitle() {
        return mTitle;
    }

    public void setIcon(Drawable icon) {
        if ((icon == null && mIcon != null) || (icon != null && mIcon != icon)) {
            mIcon = icon;

            notifyChanged();
        }
    }

    public void setIcon(@DrawableRes int iconResId) {
        if (mIconResId != iconResId) {
            mIconResId = iconResId;
            setIcon(mContext.getDrawable(iconResId));
        }
    }

    public Drawable getIcon() {
        if (mIcon == null && mIconResId != 0) {
            mIcon = getContext().getDrawable(mIconResId);
        }
        return mIcon;
    }

    public CharSequence getSummary() {
        return mSummary;
    }

    public void setSummary(CharSequence summary) {
        if (summary == null && mSummary != null || summary != null && !summary.equals(mSummary)) {
            mSummary = summary;
            notifyChanged();
        }
    }

    public void setSummary(@StringRes int summaryResId) {
        setSummary(mContext.getString(summaryResId));
    }

    public void setEnabled(boolean enabled) {
        if (mEnabled != enabled) {
            mEnabled = enabled;

            // Enabled state can change dependent preferences' states, so notify
            notifyDependencyChange(shouldDisableDependents());

            notifyChanged();
        }
    }

    public boolean isEnabled() {
        return mEnabled && mDependencyMet && mParentDependencyMet;
    }

    public void setSelectable(boolean selectable) {
        if (mSelectable != selectable) {
            mSelectable = selectable;
            notifyChanged();
        }
    }

    public boolean isSelectable() {
        return mSelectable;
    }

    public void setShouldDisableView(boolean shouldDisableView) {
        mShouldDisableView = shouldDisableView;
        notifyChanged();
    }

    public boolean getShouldDisableView() {
        return mShouldDisableView;
    }

    long getId() {
        return mId;
    }

    protected void onClick() {
    }

    public void setKey(String key) {
        mKey = key;

        if (mRequiresKey && !hasKey()) {
            requireKey();
        }
    }

    public String getKey() {
        return mKey;
    }

    void requireKey() {
        if (mKey == null) {
            throw new IllegalStateException("Preference does not have a key assigned.");
        }

        mRequiresKey = true;
    }

    public boolean hasKey() {
        return !TextUtils.isEmpty(mKey);
    }

    public boolean isPersistent() {
        return mPersistent;
    }

    protected boolean shouldPersist() {
        return mMiuiPreferenceManager != null && isPersistent() && hasKey();
    }

    public void setPersistent(boolean persistent) {
        mPersistent = persistent;
    }

    protected boolean callChangeListener(Object newValue) {
        return mOnChangeListener == null ? true : mOnChangeListener.onMiuiPreferenceChange(this, newValue);
    }

    public void setOnMiuiPreferenceChangeListener(OnMiuiPreferenceChangeListener onMiuiPreferenceChangeListener) {
        mOnChangeListener = onMiuiPreferenceChangeListener;
    }

    public OnMiuiPreferenceChangeListener getOnMiuiPreferenceChangeListener() {
        return mOnChangeListener;
    }

    public void setOnMiuiPreferenceClickListener(OnMiuiPreferenceClickListener onMiuiPreferenceClickListener) {
        mOnClickListener = onMiuiPreferenceClickListener;
    }

    public OnMiuiPreferenceClickListener getOnMiuiPreferenceClickListener() {
        return mOnClickListener;
    }

    public void performClick(MiuiPreferenceScreen preferenceScreen) {

        if (!isEnabled()) {
            return;
        }

        onClick();

        if (mOnClickListener != null && mOnClickListener.onMiuiPreferenceClick(this)) {
            return;
        }

        MiuiPreferenceManager preferenceManager = getMiuiPreferenceManager();
        if (preferenceManager != null) {
            MiuiPreferenceManager.OnPreferenceTreeClickListener listener = preferenceManager
                    .getOnPreferenceTreeClickListener();
            if (preferenceScreen != null && listener != null
                    && listener.onPreferenceTreeClick(preferenceScreen, this)) {
                return;
            }
        }

        if (mIntent != null) {
            Context context = getContext();
            context.startActivity(mIntent);
        }
    }

    public boolean onKey(View v, int keyCode, KeyEvent event) {
        return false;
    }

    public Context getContext() {
        return mContext;
    }

    public SharedPreferences getSharedPreferences() {
        if (mMiuiPreferenceManager == null) {
            return null;
        }

        return mMiuiPreferenceManager.getSharedPreferences();
    }

    public SharedPreferences.Editor getEditor() {
        if (mMiuiPreferenceManager == null) {
            return null;
        }

        return mMiuiPreferenceManager.getEditor();
    }

    public boolean shouldCommit() {
        if (mMiuiPreferenceManager == null) {
            return false;
        }

        return mMiuiPreferenceManager.shouldCommit();
    }


    final void setOnMiuiPreferenceChangeInternalListener(OnMiuiPreferenceChangeInternalListener listener) {
        mListener = listener;
    }

    protected void notifyChanged() {
        if (mListener != null) {
            mListener.onMiuiPreferenceChange(this);
        }
    }

    protected void notifyHierarchyChanged() {
        if (mListener != null) {
            mListener.onMiuiPreferenceHierarchyChange(this);
        }
    }

    public MiuiPreferenceManager getMiuiPreferenceManager() {
        return mMiuiPreferenceManager;
    }

    protected void onAttachedToHierarchy(MiuiPreferenceManager MiuipreferenceManager) {
        mMiuiPreferenceManager = MiuipreferenceManager;

        mId = MiuipreferenceManager.getNextId();

        dispatchSetInitialValue();
    }

    protected void onAttachedToActivity() {
        // At this point, the hierarchy that this preference is in is connected
        // with all other preferences.
        registerDependency();
    }

    private void registerDependency() {

        if (TextUtils.isEmpty(mDependencyKey)) return;

        MiuiPreference preference = findMiuiPreferenceInHierarchy(mDependencyKey);
        if (preference != null) {
            preference.registerDependent(this);
        } else {
            throw new IllegalStateException("Dependency \"" + mDependencyKey
                    + "\" not found for preference \"" + mKey + "\" (title: \"" + mTitle + "\"");
        }
    }

    private void unregisterDependency() {
        if (mDependencyKey != null) {
            final MiuiPreference oldDependency = findMiuiPreferenceInHierarchy(mDependencyKey);
            if (oldDependency != null) {
                oldDependency.unregisterDependent(this);
            }
        }
    }

    protected MiuiPreference findMiuiPreferenceInHierarchy(String key) {
        if (TextUtils.isEmpty(key) || mMiuiPreferenceManager == null) {
            return null;
        }

        return mMiuiPreferenceManager.findPreference(key);
    }

    private void registerDependent(MiuiPreference dependent) {
        if (mDependents == null) {
            mDependents = new ArrayList<MiuiPreference>();
        }

        mDependents.add(dependent);

        dependent.onDependencyChanged(this, shouldDisableDependents());
    }

    private void unregisterDependent(MiuiPreference dependent) {
        if (mDependents != null) {
            mDependents.remove(dependent);
        }
    }

    public void notifyDependencyChange(boolean disableDependents) {
        final List<MiuiPreference> dependents = mDependents;

        if (dependents == null) {
            return;
        }

        final int dependentsCount = dependents.size();
        for (int i = 0; i < dependentsCount; i++) {
            dependents.get(i).onDependencyChanged(this, disableDependents);
        }
    }

    public void onDependencyChanged(MiuiPreference dependency, boolean disableDependent) {
        if (mDependencyMet == disableDependent) {
            mDependencyMet = !disableDependent;

            // Enabled state can change dependent preferences' states, so notify
            notifyDependencyChange(shouldDisableDependents());

            notifyChanged();
        }
    }

    public void onParentChanged(MiuiPreference parent, boolean disableChild) {
        if (mParentDependencyMet == disableChild) {
            mParentDependencyMet = !disableChild;

            // Enabled state can change dependent preferences' states, so notify
            notifyDependencyChange(shouldDisableDependents());

            notifyChanged();
        }
    }
    public boolean shouldDisableDependents() {
        return !isEnabled();
    }

    public void setDependency(String dependencyKey) {
        // Unregister the old dependency, if we had one
        unregisterDependency();

        // Register the new
        mDependencyKey = dependencyKey;
        registerDependency();
    }

    public String getDependency() {
        return mDependencyKey;
    }

    @CallSuper
    protected void onPrepareForRemoval() {
        unregisterDependency();
    }

    public void setDefaultValue(Object defaultValue) {
        mDefaultValue = defaultValue;
    }

    private void dispatchSetInitialValue() {
        // By now, we know if we are persistent.
        final boolean shouldPersist = shouldPersist();
        if (!shouldPersist || !getSharedPreferences().contains(mKey)) {
            if (mDefaultValue != null) {
                onSetInitialValue(false, mDefaultValue);
            }
        } else {
            onSetInitialValue(true, null);
        }
    }

    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
    }

    private void tryCommit(SharedPreferences.Editor editor) {
        if (mMiuiPreferenceManager.shouldCommit()) {
            try {
                editor.apply();
            } catch (AbstractMethodError unused) {
                // The app injected its own pre-Gingerbread
                // SharedPreferences.Editor implementation without
                // an apply method.
                editor.commit();
            }
        }
    }

    protected boolean persistString(String value) {
        if (shouldPersist()) {
            // Shouldn't store null
            if (TextUtils.equals(value, getPersistedString(null))) {
                // It's already there, so the same as persisting
                return true;
            }

            SharedPreferences.Editor editor = mMiuiPreferenceManager.getEditor();
            editor.putString(mKey, value);
            tryCommit(editor);
            return true;
        }
        return false;
    }

    protected String getPersistedString(String defaultReturnValue) {
        if (!shouldPersist()) {
            return defaultReturnValue;
        }

        return mMiuiPreferenceManager.getSharedPreferences().getString(mKey, defaultReturnValue);
    }

    public boolean persistStringSet(Set<String> values) {
        if (shouldPersist()) {
            // Shouldn't store null
            if (values.equals(getPersistedStringSet(null))) {
                // It's already there, so the same as persisting
                return true;
            }

            SharedPreferences.Editor editor = mMiuiPreferenceManager.getEditor();
            editor.putStringSet(mKey, values);
            tryCommit(editor);
            return true;
        }
        return false;
    }

    public Set<String> getPersistedStringSet(Set<String> defaultReturnValue) {
        if (!shouldPersist()) {
            return defaultReturnValue;
        }

        return mMiuiPreferenceManager.getSharedPreferences().getStringSet(mKey, defaultReturnValue);
    }

    protected boolean persistInt(int value) {
        if (shouldPersist()) {
            if (value == getPersistedInt(~value)) {
                // It's already there, so the same as persisting
                return true;
            }

            SharedPreferences.Editor editor = mMiuiPreferenceManager.getEditor();
            editor.putInt(mKey, value);
            tryCommit(editor);
            return true;
        }
        return false;
    }

    protected int getPersistedInt(int defaultReturnValue) {
        if (!shouldPersist()) {
            return defaultReturnValue;
        }

        return mMiuiPreferenceManager.getSharedPreferences().getInt(mKey, defaultReturnValue);
    }

    protected boolean persistBoolean(boolean value) {
        if (shouldPersist()) {
            if (value == getPersistedBoolean(!value)) {
                // It's already there, so the same as persisting
                return true;
            }

            SharedPreferences.Editor editor = mMiuiPreferenceManager.getEditor();
            editor.putBoolean(mKey, value);
            tryCommit(editor);
            return true;
        }
        return false;
    }

    protected boolean getPersistedBoolean(boolean defaultReturnValue) {
        if (!shouldPersist()) {
            return defaultReturnValue;
        }

        return mMiuiPreferenceManager.getSharedPreferences().getBoolean(mKey, defaultReturnValue);
    }

    boolean canRecycleLayout() {
        return mCanRecycleLayout;
    }

    @Override
    public String toString() {
        return getFilterableStringBuilder().toString();
    }

    StringBuilder getFilterableStringBuilder() {
        StringBuilder sb = new StringBuilder();
        CharSequence title = getTitle();
        if (!TextUtils.isEmpty(title)) {
            sb.append(title).append(' ');
        }
        CharSequence summary = getSummary();
        if (!TextUtils.isEmpty(summary)) {
            sb.append(summary).append(' ');
        }
        if (sb.length() > 0) {
            // Drop the last space
            sb.setLength(sb.length() - 1);
        }
        return sb;
    }

    public void saveHierarchyState(Bundle container) {
        dispatchSaveInstanceState(container);
    }

    void dispatchSaveInstanceState(Bundle container) {
        if (hasKey()) {
            mBaseMethodCalled = false;
            Parcelable state = onSaveInstanceState();
            if (!mBaseMethodCalled) {
                throw new IllegalStateException(
                        "Derived class did not call super.onSaveInstanceState()");
            }
            if (state != null) {
                container.putParcelable(mKey, state);
            }
        }
    }

    protected Parcelable onSaveInstanceState() {
        mBaseMethodCalled = true;
        return BaseSavedState.EMPTY_STATE;
    }

    public void restoreHierarchyState(Bundle container) {
        dispatchRestoreInstanceState(container);
    }

    void dispatchRestoreInstanceState(Bundle container) {
        if (hasKey()) {
            Parcelable state = container.getParcelable(mKey);
            if (state != null) {
                mBaseMethodCalled = false;
                onRestoreInstanceState(state);
                if (!mBaseMethodCalled) {
                    throw new IllegalStateException(
                            "Derived class did not call super.onRestoreInstanceState()");
                }
            }
        }
    }

    protected void onRestoreInstanceState(Parcelable state) {
        mBaseMethodCalled = true;
        if (state != BaseSavedState.EMPTY_STATE && state != null) {
            throw new IllegalArgumentException("Wrong state class -- expecting Preference State");
        }
    }

    public static class BaseSavedState extends AbsSavedState {
        public BaseSavedState(Parcel source) {
            super(source);
        }

        public BaseSavedState(Parcelable superState) {
            super(superState);
        }

        public static final Parcelable.Creator<BaseSavedState> CREATOR =
                new Parcelable.Creator<BaseSavedState>() {
                    public BaseSavedState createFromParcel(Parcel in) {
                        return new BaseSavedState(in);
                    }

                    public BaseSavedState[] newArray(int size) {
                        return new BaseSavedState[size];
                    }
                };
    }

  /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    +                                                                                              +
    +                           MiuiCORE [Independents Settings]                                   +
    +                                                                                              +
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    //==========================================Init=================================================
    public void setStorageType(int value){
        mStorageType = value;
    }

    //===========================================GET/SET Key Value===================================
    public void setStringValue(String value){
        // try {
        switch (mStorageType) {
            case 0:
                Settings.System.putString(mContext.getContentResolver(), mKey, value);
                break;
            case 1:
                Settings.Global.putString(mContext.getContentResolver(), mKey, value);
                break;
            case 2:
                Settings.Secure.putString(mContext.getContentResolver(), mKey, value);
                break;
        }

    }

    public void setIntegerValue(int value){

        switch (mStorageType) {
            case 0:
                Settings.System.putInt(mContext.getContentResolver(), mKey, value);
                break;
            case 1:
                Settings.Global.putInt(mContext.getContentResolver(), mKey, value);
                break;
            case 2:
                Settings.Secure.putInt(mContext.getContentResolver(), mKey, value);
                break;
        }
    }

    public String getStringValue(){
        String value = null;
        switch (mStorageType) {
            case 0:
                value = Settings.System.getString(mContext.getContentResolver(), mKey);
                break;
            case 1:
                value = Settings.Global.getString(mContext.getContentResolver(), mKey);
                break;
            case 2:
                value = Settings.Secure.getString(mContext.getContentResolver(), mKey);
                break;
        }
        return value != null ? value : (String) mDefaultValue;
    }

    public Integer  getIntegerValue(){
        try {
            switch (mStorageType) {
                case 0:
                    return Settings.System.getInt(mContext.getContentResolver(), mKey);
                case 1:
                    return Settings.Global.getInt(mContext.getContentResolver(), mKey);
                case 2:
                    return Settings.Secure.getInt(mContext.getContentResolver(), mKey);
            }
        } catch (Settings.SettingNotFoundException e) {
            setIntegerValue((Integer) mDefaultValue);
        }
        return (Integer) mDefaultValue;
    }


    //===========================================GET Resource========================================
    public int IDtoID(String mName){
        return mContext.getResources().getIdentifier(mName, "id", mContext.getPackageName());
    }

    public int LayoutToID(String mName){
        return  mContext.getResources().getIdentifier(mName, "layout", mContext.getPackageName());
    }

    public int StyleToID(String mName){
        return  mContext.getResources().getIdentifier(mName, "style", mContext.getPackageName());
    }

    public int AttrToID(String mName){
        return  mContext.getResources().getIdentifier(mName, "attr", mContext.getPackageName());
    }

    //======================================Other Method=============================================

    public boolean getPackageInstall(String packageName){
        boolean result = false;
        PackageInfo mPackageInfo;
        try {
            mPackageInfo = mContext.getPackageManager().getPackageInfo(packageName,0);
            if (mPackageInfo != null) {result = true;}
        }catch (PackageManager.NameNotFoundException e){
           return result;
        }
        return result;
    }

    public static void LaunchCustomApplication(Context mContext, String value, int mStorageType) {
        String mPackageName = Settings.System.getString(mContext.getContentResolver(), value);
        switch (mStorageType) {
            case 1:
                mPackageName = Settings.Global.getString(mContext.getContentResolver(), value);
                break;
            case 2:
                mPackageName = Settings.Secure.getString(mContext.getContentResolver(), value);
                break;
        }
        Intent intent = mContext.getPackageManager().getLaunchIntentForPackage(mPackageName);
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(intent);
        }

    }

    public static int getKeyParam(Context mContext, String value, int mStorageType) {
        switch (mStorageType) {
            case 1:
                return Settings.Global.getInt(mContext.getContentResolver(), value, 0);
            case 2:
                return Settings.Secure.getInt(mContext.getContentResolver(), value, 0);
            default:
                return Settings.System.getInt(mContext.getContentResolver(), value, 0);

        }

    }

    public static long getKeyTime(Context mContext, String value, int mStorageType) {
        switch (mStorageType) {
            case 1:
                return Settings.Global.getLong(mContext.getContentResolver(), value, 0);
            case 2:
                return Settings.Secure.getLong(mContext.getContentResolver(), value, 0);
            default:
                return Settings.System.getLong(mContext.getContentResolver(), value, 0);

        }
    }





}
