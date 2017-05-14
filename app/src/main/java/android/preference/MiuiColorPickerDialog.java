package android.preference;


import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

public class MiuiColorPickerDialog extends Dialog implements MiuiColorPickerView.OnColorChangedListener, View.OnClickListener, ViewTreeObserver.OnGlobalLayoutListener {
    public MiuiPreference mMiuiPreference;
    public MiuiColorPickerPreference mMiuiColorPickerPreference;
    private MiuiColorPickerView mColorPicker;

    private MiuiColorPickerPanelView mOldColor;
    private MiuiColorPickerPanelView mNewColor;
    private MiuiColorPickerPanelView mWhite;
    private MiuiColorPickerPanelView mBlack;
    private MiuiColorPickerPanelView mBlue;
    private MiuiColorPickerPanelView mRed;
    private MiuiColorPickerPanelView mGreen;
    private MiuiColorPickerPanelView mYellow;
    private TextView mHexText;
    private ImageView mSetButton;
    private EditText mHexVal;
    private boolean mHexValueEnabled = false;
    private ColorStateList mHexDefaultTextColor;

    private OnColorChangedListener mListener;
    private int mOrientation;
    private View mLayout;


    public MiuiColorPickerDialog(Context context, int initialColor) {
        super(context);
        mMiuiPreference = new MiuiPreference(context);
        initialization(initialColor);
    }

    private void initialization(int color) {
        // To fight color banding.
        getWindow().setFormat(PixelFormat.RGBA_8888);

        setUp(color);

    }

    @Override
    public void onGlobalLayout() {
        if (getContext().getResources().getConfiguration().orientation != mOrientation) {
            final int oldcolor = mOldColor.getColor();
            final int newcolor = mNewColor.getColor();
            mLayout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
            setUp(oldcolor);
            mNewColor.setColor(newcolor);
            mColorPicker.setColor(newcolor);
        }
    }

    public interface OnColorChangedListener {
        public void onColorChanged(int color);
    }


    private void setUp(int color) {

        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mLayout = inflater.inflate(mMiuiPreference.LayoutToID("color_preference_layout"), null);
        mLayout.getViewTreeObserver().addOnGlobalLayoutListener(this);

        mOrientation = getContext().getResources().getConfiguration().orientation;
        setContentView(mLayout);

        //setTitle(R.string.dialog_color_picker);

        mColorPicker = (MiuiColorPickerView) mLayout.findViewById(mMiuiPreference.IDtoID("color_picker_view"));

        mOldColor = (MiuiColorPickerPanelView) mLayout.findViewById(mMiuiPreference.IDtoID("old_color_panel"));
        mNewColor = (MiuiColorPickerPanelView) mLayout.findViewById(mMiuiPreference.IDtoID("new_color_panel"));

        mWhite = (MiuiColorPickerPanelView) mLayout.findViewById(mMiuiPreference.IDtoID("white_panel"));//
        mBlack = (MiuiColorPickerPanelView) mLayout.findViewById(mMiuiPreference.IDtoID("black_panel"));//
        mBlue = (MiuiColorPickerPanelView) mLayout.findViewById(mMiuiPreference.IDtoID("blue_panel"));
        mRed = (MiuiColorPickerPanelView) mLayout.findViewById(mMiuiPreference.IDtoID("red_panel"));
        mGreen = (MiuiColorPickerPanelView) mLayout.findViewById(mMiuiPreference.IDtoID("green_panel"));
        mYellow = (MiuiColorPickerPanelView) mLayout.findViewById(mMiuiPreference.IDtoID("yellow_panel"));


        mHexVal = (EditText) mLayout.findViewById(mMiuiPreference.IDtoID("hex"));
        mHexText = (TextView) mLayout.findViewById(mMiuiPreference.IDtoID("hex_text"));
        mSetButton = (ImageView) mLayout.findViewById(mMiuiPreference.IDtoID("enter"));
        mHexVal.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        mHexDefaultTextColor = mHexVal.getTextColors();

        mHexVal.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    String s = mHexVal.getText().toString();
                    if (s.length() > 5 || s.length() < 10) {
                        try {
                            int c = MiuiColorPickerPreference.convertToColorInt(s.toString());
                            mColorPicker.setColor(c, true);
                            mHexVal.setTextColor(mHexDefaultTextColor);
                        } catch (IllegalArgumentException e) {
                            mHexVal.setTextColor(Color.RED);
                        }
                    } else {
                        mHexVal.setTextColor(Color.RED);
                    }
                    return true;
                }
                return false;
            }
        });

        ((LinearLayout) mOldColor.getParent()).setPadding(
                Math.round(mColorPicker.getDrawingOffset()),
                0,
                Math.round(mColorPicker.getDrawingOffset()),
                0
        );

        mOldColor.setOnClickListener(this);
        mNewColor.setOnClickListener(this);
        mColorPicker.setOnColorChangedListener(this);
        mOldColor.setColor(color);
        mColorPicker.setColor(color, true);
        if (mSetButton != null) {
            mSetButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String tmp = mHexVal.getText().toString();
                    int i = MiuiColorPickerPreference.convertToColorInt(tmp);
                    mColorPicker.setColor(i, true);
                }
            });
        }
        setColorInTemplate(mWhite, Color.WHITE);
        setColorInTemplate(mBlack, Color.BLACK);
        setColorInTemplate(mBlue, Color.BLUE);
        setColorInTemplate(mRed, Color.RED);
        setColorInTemplate(mGreen, Color.GREEN);
        setColorInTemplate(mYellow, Color.YELLOW);

    }

    public void setColorInTemplate(MiuiColorPickerPanelView color_panel, final int color_tempate) {
        if (color_panel != null) {
            color_panel.setColor(color_tempate);
            color_panel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    MiuiColorPickerDialog.this.mColorPicker.setColor(color_tempate, true);


                }
            });
        }

    }

    @Override
    public void onColorChanged(int color) {

        mNewColor.setColor(color);

        if (mHexValueEnabled)
            updateHexValue(color);

    }

    public void setHexValueEnabled(boolean enable) {
        mHexValueEnabled = enable;
        if (enable) {
            mHexVal.setVisibility(View.VISIBLE);
            mHexText.setVisibility(View.VISIBLE);
            mSetButton.setVisibility(View.VISIBLE);
            updateHexLengthFilter();
            updateHexValue(getColor());
        } else {
            mHexVal.setVisibility(View.GONE);
            mHexText.setVisibility(View.GONE);
            mSetButton.setVisibility(View.GONE);
        }
    }

    public boolean getHexValueEnabled() {
        return mHexValueEnabled;
    }

    private void updateHexLengthFilter() {
        if (getAlphaSliderVisible())
            mHexVal.setFilters(new InputFilter[]{new InputFilter.LengthFilter(9)});
        else
            mHexVal.setFilters(new InputFilter[]{new InputFilter.LengthFilter(7)});
    }

    private void updateHexValue(int color) {
        if (getAlphaSliderVisible()) {
            mHexVal.setText(MiuiColorPickerPreference.convertToARGB(color).toUpperCase(Locale.getDefault()));
        } else {
            mHexVal.setText(MiuiColorPickerPreference.convertToRGB(color).toUpperCase(Locale.getDefault()));
        }
        mHexVal.setTextColor(mHexDefaultTextColor);
    }

    public void setAlphaSliderVisible(boolean visible) {
        mColorPicker.setAlphaSliderVisible(visible);
        if (mHexValueEnabled) {
            updateHexLengthFilter();
            updateHexValue(getColor());
        }
    }

    public boolean getAlphaSliderVisible() {
        return mColorPicker.getAlphaSliderVisible();
    }


    public void setOnColorChangedListener(OnColorChangedListener listener) {
        mListener = listener;
    }

    public int getColor() {
        return mColorPicker.getColor();
    }

    public void setMiuiColoPickerPreference(MiuiColorPickerPreference value) {
        this.mMiuiColorPickerPreference = value;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == mMiuiPreference.IDtoID("new_color_panel")) {
            if (mListener != null) {
                int newColor = mNewColor.getColor();
                mMiuiColorPickerPreference.setNewColor(newColor);
                mListener.onColorChanged(newColor);


            }

        }
        dismiss();
    }

    @Override
    public Bundle onSaveInstanceState() {
        Bundle state = super.onSaveInstanceState();
        state.putInt("old_color", mOldColor.getColor());
        state.putInt("new_color", mNewColor.getColor());
        return state;
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mOldColor.setColor(savedInstanceState.getInt("old_color"));
        mColorPicker.setColor(savedInstanceState.getInt("new_color"), true);
    }


}

