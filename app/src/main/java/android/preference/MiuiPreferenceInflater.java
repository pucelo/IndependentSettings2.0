package android.preference;

import com.android.internal.util.XmlUtils;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;

class MiuiPreferenceInflater extends GenericInflater<MiuiPreference, MiuiPreferenceGroup> {
    private static final String TAG = "MiuiPreferenceInflater";
    private static final String INTENT_TAG_NAME = "intent";
    private static final String EXTRA_TAG_NAME = "extra";
    private MiuiPreferenceManager mPreferenceManager;

    public MiuiPreferenceInflater(Context context, MiuiPreferenceManager preferenceManager) {
        super(context);
        init(preferenceManager);
    }

    MiuiPreferenceInflater(GenericInflater<MiuiPreference, MiuiPreferenceGroup> original, MiuiPreferenceManager preferenceManager, Context newContext) {
        super(original, newContext);
        init(preferenceManager);
    }

    @Override
    public GenericInflater<MiuiPreference, MiuiPreferenceGroup> cloneInContext(Context newContext) {
        return new MiuiPreferenceInflater(this, mPreferenceManager, newContext);
    }

    private void init(MiuiPreferenceManager preferenceManager) {
        mPreferenceManager = preferenceManager;
        setDefaultPackage("android.preference.");
    }

    @Override
    protected boolean onCreateCustomFromTag(XmlPullParser parser, MiuiPreference parentPreference,
                                            AttributeSet attrs) throws XmlPullParserException {
        final String tag = parser.getName();

        if (tag.equals(INTENT_TAG_NAME)) {
            Intent intent = null;

            try {
                intent = Intent.parseIntent(getContext().getResources(), parser, attrs);
            } catch (IOException e) {
                XmlPullParserException ex = new XmlPullParserException(
                        "Error parsing preference");
                ex.initCause(e);
                throw ex;
            }

            if (intent != null) {
                parentPreference.setIntent(intent);
            }

            return true;
        } else if (tag.equals(EXTRA_TAG_NAME)) {
            getContext().getResources().parseBundleExtra(EXTRA_TAG_NAME, attrs,
                    parentPreference.getExtras());
            try {
                XmlUtils.skipCurrentTag(parser);
            } catch (IOException e) {
                XmlPullParserException ex = new XmlPullParserException(
                        "Error parsing preference");
                ex.initCause(e);
                throw ex;
            }
            return true;
        }

        return false;
    }

    @Override
    protected MiuiPreferenceGroup onMergeRoots(MiuiPreferenceGroup givenRoot, boolean attachToGivenRoot,
                                           MiuiPreferenceGroup xmlRoot) {
        // If we were given a Preferences, use it as the root (ignoring the root
        // Preferences from the XML file).
        if (givenRoot == null) {
            xmlRoot.onAttachedToHierarchy(mPreferenceManager);
            return xmlRoot;
        } else {
            return givenRoot;
        }
    }
}
