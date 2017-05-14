package android.preference;


import android.content.Context;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class MiuiSeekBarPreference extends MiuiDialogPreference implements MiuiSeekBar.OnSeekBarHintProgressChangeListener {
    private int mProgress;
    private int mMax;
    private int mMin;
    private int mStep;
    private int mHintWidth;
    private int mYOffset;
    private int mDefaultValue;
    private boolean mShow;
    private MiuiSeekBar mMiuiSeekBar;
    private String mHintStyle;
    private String[] mPrSfSummary;

    public MiuiSeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialization(context);
    }

    private void initialization(Context context) {

    }

    protected View onCreateView(ViewGroup mViewGroup) {

        mViewGroup = (ViewGroup) super.onCreateView(mViewGroup);
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View undoView = inflater.inflate(LayoutToID("miui_seekbar_preference_layout"), null);
        ((ViewGroup) undoView).addView(mViewGroup, 0);

        return undoView;

    }

    public boolean getDependents(Integer value) {
        boolean result = false;
        if (value <= 0) {
            result = true;
        }
        return result;
    }

    @Override
    public boolean shouldDisableDependents() {
        boolean result = false;
        if (getIntegerValue() <= 0) {
            result = true;
        }
        return result;
    }

    @Override
    public void setSummary(CharSequence summary) {
        //summary = mPrSfSummary[0] + summary+ mPrSfSummary[1];
        super.setSummary(summary);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        mMiuiSeekBar = (MiuiSeekBar) view.findViewById(IDtoID("miui_seekbar"));
        if (mShow) {
            mMiuiSeekBar.setHintWidth(mHintWidth);
        } else {
            mMiuiSeekBar.setHintWidth(0);
        }
        mMiuiSeekBar.mYOffset = mYOffset;
        mMiuiSeekBar.setHintStyle(mHintStyle);
        mMiuiSeekBar.setHintView(view, mMiuiSeekBar);
        mMiuiSeekBar.setOnProgressChangeListener(this);
        mMiuiSeekBar.mMax = mMax;
        mMiuiSeekBar.mMin = mMin;
        mMiuiSeekBar.mStep = mStep;
        mMiuiSeekBar.mYOffset = (mYOffset >= 10 ? mYOffset : 10);
        mMiuiSeekBar.mProgress = mProgress;
        mMiuiSeekBar.setMax((mMax - mMin) / mStep);
        mMiuiSeekBar.setProgress((mProgress - mMin) / mStep);
        mMiuiSeekBar.setEnabled(isEnabled());
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        int value = getIntegerValue();

        if (restorePersistedValue) {
            setProgress(value);
            setSummary(String.valueOf(value));
        } else {
            setProgress(value);
            setSummary(String.valueOf(value));
            //отправка интента
            //setSummary(getEntry());

        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (isPersistent()) {
            return superState;
        }
        final SavedState myState = new SavedState(superState);
        myState.progress = mProgress;
        myState.max = mMax;
        myState.min = mMin;
        myState.step = mStep;
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!state.getClass().equals(SavedState.class)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        mProgress = myState.progress;
        mMax = myState.max;
        mStep = myState.step;
        mMin = myState.min;
        notifyChanged();
    }

    @Override
    public String onHintTextChanged(MiuiSeekBar seekBarHint, int progress) {
        return null;
    }

    @Override
    public void onMiuiSeekBarProgress(int progress) {
        mProgress = progress * mStep + mMin;
        mMiuiSeekBar.setProgress(mProgress);
        setSummary(String.valueOf(mProgress));//mProgress
        notifyDependencyChange(getDependents(mProgress));
        setIntegerValue(mProgress);//Запись состояния
        //sendIntent();//посылка Intent


    }


    private static class SavedState extends Preference.BaseSavedState {
        int progress;
        int max;
        int min;
        int step;

        public SavedState(Parcel source) {
            super(source);

            progress = source.readInt();
            max = source.readInt();
            min = source.readInt();
            step = source.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);

            dest.writeInt(progress);
            dest.writeInt(max);
            dest.writeInt(min);
            dest.writeInt(step);
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @SuppressWarnings("unused")
        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }


    public void setProgress(int progress) {
        setProgress(progress, true);
    }

    private void setProgress(int progress, boolean notifyChanged) {
        if (progress > mMax) {
            progress = mMax;
        }
        if (progress < 0) {
            progress = 0;
        }
        if (progress != mProgress) {
            mProgress = progress;
            persistInt(progress);
            if (notifyChanged) {
                notifyChanged();
            }
        }
    }

    public void setMin(int min) {
        if (min != mMin) {
            mMin = min;
            notifyChanged();
        }
    }

    public void setMax(int max) {
        if (max != mMax) {
            mMax = max;
            notifyChanged();
        }
    }

    public void setStep(int step) {
        if (step != mStep) {
            mStep = step;
            notifyChanged();
        }
    }
}
