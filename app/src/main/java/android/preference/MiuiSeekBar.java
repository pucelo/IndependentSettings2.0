package android.preference;


import android.content.Context;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;

import static android.view.View.MeasureSpec.UNSPECIFIED;
import static android.view.View.MeasureSpec.makeMeasureSpec;

public class MiuiSeekBar extends SeekBar implements SeekBar.OnSeekBarChangeListener {
    public MiuiPreference mMiuiPreference;
    public int mProgress;
    public int mMax;
    public int mMin;
    public int mStep;
    public int mYOffset;
    private int mHintWidth;
    private int mHintStyle;
    public static final int HINT_FIXED = 1;
    public static final int HINT_FOLLOW = 0;
    private PopupWindow mHint;
    private TextView mHintTextView;
    private OnSeekBarChangeListener mInternalListener;
    private OnSeekBarChangeListener mExternalListener;
    private OnSeekBarHintProgressChangeListener mProgressChangeListener;
    private View mPopupView;

    public MiuiSeekBar(Context context) {
        this(context, null);
    }

    public MiuiSeekBar(Context context, AttributeSet attrs) {
        this(context, attrs, com.android.internal.R.attr.seekBarStyle);
    }

    public MiuiSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public MiuiSeekBar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mMiuiPreference = new MiuiPreference(context, attrs);
        setOnSeekBarChangeListener(this);
    }

    public interface OnSeekBarHintProgressChangeListener {
        String onHintTextChanged(MiuiSeekBar seekBarHint, int progress);

        void onMiuiSeekBarProgress(int progress);

    }

    private void initializationHintPopup() {
        String hintText = "";

        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mPopupView = inflater.inflate(mMiuiPreference.LayoutToID("miui_seekbar_hint_layout"), null);
        mPopupView.measure(makeMeasureSpec(0, UNSPECIFIED), makeMeasureSpec(0, UNSPECIFIED));
        mHint = new PopupWindow(mPopupView, mHintWidth, ViewGroup.LayoutParams.WRAP_CONTENT, false);
        mHintTextView = (TextView) mPopupView.findViewById(mMiuiPreference.IDtoID("HintText"));
        mHintTextView.setText(hintText != null ? hintText : String.valueOf(getProgress()));
        mHint.setAnimationStyle(mMiuiPreference.StyleToID("Animation_PopupWindow_ActionMode"));

    }

    private int setSHintStyle(String value) {
        int result = HINT_FOLLOW;
        if (value != null) {
            if (value.equals("fixed")) {
                result = HINT_FIXED;
            }
        }
        return result;
    }

    public void setHintView(View view, SeekBar mSeekBar) {

        initializationHintPopup();

    }

    public void setHintStyle(String style) {

        mHintStyle = setSHintStyle(style);
    }
    /*
    public int getHintStyle(){

        return mHintStyle;
    }*/

    public void setHintWidth(int value) {

        mHintWidth = value;
    }
    /*
    public int getHintWidth(){

        return mHintWidth;
    }*/

    private void showHintPopup() {
        Point offsetPoint = null;
        switch (mHintStyle) {
            case HINT_FOLLOW:
                offsetPoint = getFollowHintOffset();
                break;
            case HINT_FIXED:
                offsetPoint = getFixedHintOffset();
                break;
        }
        mHint.showAtLocation(this, Gravity.NO_GRAVITY, 0, 0);
        mHint.update(this, offsetPoint.x, offsetPoint.y, -1, -1);
    }

    protected Point getFixedHintOffset() {
        //int xOffset = getHorizontalOffset(this.getMax() / 2 )+100;
        int xOffset = this.getWidth() / 2 - mPopupView.getMeasuredWidth();
        int yOffset = getVerticalOffset();
        return new Point(xOffset, yOffset);
    }

    protected Point getFollowHintOffset() {
        int xOffset = getHorizontalOffset(this.getProgress());
        int yOffset = getVerticalOffset();
        return new Point(xOffset, yOffset);
    }

    private int getFollowPosition(int progress) {
        return (int) (progress * (this.getWidth()
                - this.getPaddingLeft()
                - this.getPaddingRight()) / (float) this.getMax()) - 5;
    }

    private int getHorizontalOffset(int progress) {
        return getFollowPosition(progress) - mPopupView.getMeasuredWidth() / 2 + this.getHeight();
    }

    private int getVerticalOffset() {
        return -(this.getHeight() + mPopupView.getMeasuredHeight() + mYOffset);
    }

    private void hideHintPopup() {
        if (mHint.isShowing()) {
            mHint.dismiss();
        }
    }

    public void setOnProgressChangeListener(OnSeekBarHintProgressChangeListener l) {
        mProgressChangeListener = l;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        String popupText = null;
        if (mProgressChangeListener != null) {
            popupText = mProgressChangeListener.onHintTextChanged(this, getProgress() * mStep + mMin);
        }

        if (mExternalListener != null) {
            mExternalListener.onProgressChanged(seekBar, progress, fromUser);
        }

        //mHintTextView.setTextColor(setGradientPercentColor(mCt,progress));
        mHintTextView.setText(popupText != null ? popupText : String.valueOf(progress * mStep + mMin));

        if (mHintStyle == HINT_FOLLOW) {
            Point offsetPoint = getFollowHintOffset();
            mHint.update(this, offsetPoint.x, offsetPoint.y, -1, -1);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        if (mExternalListener != null) {
            mExternalListener.onStartTrackingTouch(seekBar);
        }
        showHintPopup();
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (mExternalListener != null) {
            mExternalListener.onStopTrackingTouch(seekBar);
        }
        mProgressChangeListener.onMiuiSeekBarProgress(getProgress());
        hideHintPopup();
    }
}
