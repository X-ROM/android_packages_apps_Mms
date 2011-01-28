
package com.android.mms.templates;

import java.util.ArrayList;

import android.app.Activity;
import android.gesture.Gesture;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.mms.R;

public class TemplateEditor extends Activity {

    private class GesturesProcessor implements GestureOverlayView.OnGestureListener {

        public void onGesture(GestureOverlayView overlay, MotionEvent event) {
        }

        public void onGestureCancelled(GestureOverlayView overlay, MotionEvent event) {
        }

        public void onGestureEnded(GestureOverlayView overlay, MotionEvent event) {
            mGesture = overlay.getGesture();
            if (mGesture.getLength() < LENGTH_THRESHOLD) {
                overlay.clear(false);
            }
        }

        public void onGestureStarted(GestureOverlayView overlay, MotionEvent event) {
            mGesture = null;
        }
    }

    public static final String KEY_DISPLAY_TYPE = "display_type";

    public static final String KEY_TEMPLATE_ID = "template_id";

    public static final int DISPLAY_TYPE_NEW_TEMPLATE = 1;

    public static final int DISPLAY_TYPE_EDIT_TEMPLATE = 2;

    private static final String KEY_GESTURE = "gesture";

    private static final String LOG_TAG = TemplateEditor.class.getCanonicalName();

    private static final float LENGTH_THRESHOLD = 120.0f;

    private Gesture mGesture;

    private Button mSaveButton;

    private EditText mTemplateTextField;

    private long mCurrentTemplateEditingId = -1;

    private boolean editingMode = false;

    private void initViews() {

        final GestureOverlayView overlay = (GestureOverlayView) findViewById(R.id.gestures_overlay);
        overlay.addOnGestureListener(new GesturesProcessor());

        mTemplateTextField = (EditText) findViewById(R.id.template_text);

        mSaveButton = (Button) findViewById(R.id.done);

        mSaveButton.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                saveTemplate();
            }
        });

        if (editingMode) {

            paintGesture(overlay);
            loadTemplateFromDb();
            setTitle(R.string.template_edit_title);
        }

    }

    private Gesture loadGestureIfExists(String name) {
        final GestureLibrary store = TemplateGesturesLibrary.getStore(this);

        Log.i(LOG_TAG, "store is: " + (store == null ? "null" : "not null"));

        for (String s : store.getGestureEntries()) {
            Log.d(LOG_TAG, s);

        }

        final ArrayList<Gesture> gestures = store.getGestures(name);

        Log.i(LOG_TAG, "gesture is: " + (gestures == null ? "null" : "not null"));

        if (gestures != null && gestures.size() > 0) {
            return gestures.get(0);
        } else {
            return null;
        }
    }

    private void loadTemplateFromDb() {
        final TemplatesDb db = new TemplatesDb(this);
        db.open();
        final String messageText = db.getTemplateTextFromId(mCurrentTemplateEditingId);
        db.close();
        mTemplateTextField.setText(messageText);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.template_editor);

        final Bundle bundle = getIntent().getExtras();
        processActivityIntent(bundle);

        initViews();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        mGesture = savedInstanceState.getParcelable(KEY_GESTURE);
        if (mGesture != null) {
            final GestureOverlayView overlay = (GestureOverlayView) findViewById(R.id.gestures_overlay);
            overlay.post(new Runnable() {
                public void run() {
                    overlay.setGesture(mGesture);
                }
            });
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mGesture != null) {
            outState.putParcelable(KEY_GESTURE, mGesture);
        }
    }

    private void paintGesture(final GestureOverlayView overlay) {
        mGesture = loadGestureIfExists(String.valueOf(mCurrentTemplateEditingId));
        if (mGesture != null) {
            overlay.post(new Runnable() {
                public void run() {
                    overlay.setGesture(mGesture);
                }
            });
        }
    }

    private void processActivityIntent(Bundle bundle) {
        if (bundle != null) {
            final int displayType = bundle.getInt(KEY_DISPLAY_TYPE, DISPLAY_TYPE_NEW_TEMPLATE);

            if (displayType == DISPLAY_TYPE_EDIT_TEMPLATE) {
                editingMode = true;

                mCurrentTemplateEditingId = bundle.getLong(KEY_TEMPLATE_ID, Long.MIN_VALUE);

                if (mCurrentTemplateEditingId == Long.MIN_VALUE) {
                    throw new IllegalArgumentException(
                            "In editing mode you have to pass the message id");
                }
            }
        }
    }

    protected void saveTemplate() {

        final String templateText = mTemplateTextField.getText().toString();
        long messageId;

        if (templateText.trim().length() == 0) {
            Toast.makeText(this, R.string.template_empty_text, Toast.LENGTH_SHORT).show();
            return;
        } else {
            final TemplatesDb db = new TemplatesDb(this);
            db.open();

            if (editingMode) {
                messageId = mCurrentTemplateEditingId;
                db.updateTemplate(messageId, templateText);
            } else {
                messageId = db.insertTemplate(templateText);
            }

            db.close();
        }

        final GestureLibrary store = TemplateGesturesLibrary.getStore(this);

        if (editingMode) {
            store.removeEntry(String.valueOf(messageId));
        }

        if (mGesture != null) {
            store.addGesture(String.valueOf(messageId), mGesture);
            store.save();
        }

        setResult(RESULT_OK);
        finish();
    }

}
