package com.uniscope.edittextdelete;


import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Selection;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.lang.reflect.Field;

/**
 * author：majun
 * date：2018/7/18 13:34
 * email:xl040301@126.com
 * description：editText添加删除按钮显示
 */
public class EditTextClear extends AppCompatEditText {
    private static final boolean LOG_DBG = false;
    private static final String LOG_TAG = "EditTextClear";
    private int mArea;
    private Context mContext;
    private boolean mDeletable = false;
    private Drawable mDeleteNormal;
    private Drawable mDeletePressed;
    private int mDrawablePadding;
    private int mDrawableSizeRight;
    private boolean mForceFinishDetach = false;
    private MyTextWatcher mTextWatcher = null;
    private OnPasswordDeletedListener mPasswordDeleteListener = null;
    private boolean mQuickDelete = false;
    boolean mShouldHandleDelete = false;
    private OnTextDeletedListener mTextDeleteListener = null;

    private int maxLength = 0;

    public EditTextClear(Context context) {
        this(context, null);
    }

    public EditTextClear(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.editTextStyle);
    }

    public EditTextClear(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        //添加属性quickDelete以确定是否打开delete按钮
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.EditTextClear, 0, 0);
        boolean fastDeleteable = false;
        if (array != null) {
            fastDeleteable = array.getBoolean(0, true);
            array.recycle();
        }
        setFastDeletable(fastDeleteable);
        mDeleteNormal = context.getDrawable(R.drawable.color_searchview_text_edit_delete_normal);
        if (mDeleteNormal != null) {
            mArea = mDeleteNormal.getIntrinsicWidth();
            mDeleteNormal.setBounds(0, 0, mArea, mArea);
        }
        mDeletePressed = context.getDrawable(R.drawable.color_searchview_text_edit_delete_pressed);
        if (mDeletePressed != null) {
            mDeletePressed.setBounds(0, 0, mArea, mArea);
        }
        maxLength = getMaxLength();
    }


    public interface OnPasswordDeletedListener {
        boolean onPasswordDeleted();
    }


    public interface OnTextDeletedListener {
        boolean onTextDeleted();
    }


    private class MyTextWatcher implements TextWatcher {

        public MyTextWatcher() {
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (maxLength > 0) {
                //当在xml中设置：android:maxLength="10"并超过maxLength时，给出toast提示语。
                if (s.length() == maxLength) {
                    Toast.makeText(mContext, R.string.toast_input_exceeded, Toast.LENGTH_LONG).show();
                }
            }

        }

        @Override
        public void afterTextChanged(Editable s) {
            updateDeletableStatus(hasFocus());
        }
    }

    private void updateDeletableStatus(boolean foucus) {
        if (TextUtils.isEmpty(getText().toString())) {
            setCompoundDrawables(null, null, null, null);
            mDeletable = false;
        } else if (foucus) {
            if (mDeleteNormal != null && !mDeletable) {
                setCompoundDrawables(null, null, mDeleteNormal, null);
                mDeletable = true;
            }
        } else if (mDeletable) {
            setCompoundDrawables(null, null, null, null);
            mDeletable = false;
        }
    }

    public void setFastDeletable(boolean quickDelete) {
        if (mQuickDelete != quickDelete) {
            mQuickDelete = quickDelete;
            if (mQuickDelete) {
                if (mTextWatcher == null) {
                    mTextWatcher = new MyTextWatcher();
                    addTextChangedListener(mTextWatcher);
                }
                mDrawablePadding = mContext.getResources().getDimensionPixelSize(R.dimen.edit_text_drawable_padding);
                setCompoundDrawablePadding(mDrawablePadding);
            }
        }
    }

    public boolean isFastDeletable() {
        return mQuickDelete;
    }

    public void setOnTextDeletedListener(OnTextDeletedListener textDeleteListener) {
        mTextDeleteListener = textDeleteListener;
    }

    public void setTextDeletedListener(OnPasswordDeletedListener passwordDeletedListener) {
        mPasswordDeleteListener = passwordDeletedListener;
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
        if (mQuickDelete) {
            updateDeletableStatus(focused);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (!mQuickDelete || keyCode != KeyEvent.KEYCODE_DEL) {
            return super.onKeyDown(keyCode, event);
        }
        super.onKeyDown(keyCode, event);
        if (mPasswordDeleteListener != null) {
            mPasswordDeleteListener.onPasswordDeleted();
        }
        return true;
    }

    private void onFastDelete() {
        getText().delete(0, getText().length());
        setText("");
    }

    private boolean isEmpty(String currentText) {
        if (currentText == null) {
            return false;
        }
        return TextUtils.isEmpty(currentText);
    }

    public void setCompoundDrawables(Drawable left, Drawable top, Drawable right, Drawable bottom) {
        setCompoundDrawablesRelative(left, top, right, bottom);
        if (right != null) {
            mDrawableSizeRight = right.getBounds().width();
        } else {
            mDrawableSizeRight = 0;
        }
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        super.setText(text, type);
        Selection.setSelection(getText(), length());
    }

    public void forceFinishDetach() {
        mForceFinishDetach = true;
    }

    public void dispatchStartTemporaryDetach() {
        if (mForceFinishDetach) {
            onStartTemporaryDetach();
        } else {
            super.dispatchStartTemporaryDetach();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mQuickDelete && !isEmpty(getText().toString()) && hasFocus()) {
            int deltX = getWidth() - getPaddingRight() - mDrawableSizeRight;
            if (getWidth() >= (mDrawableSizeRight + getPaddingRight() + getPaddingLeft())) {
                int x = (int) event.getX();
                int y = (int) event.getY();
                boolean b1 = x < getLeft() + getPaddingLeft() + mDrawableSizeRight ? true : false;
                boolean b2 = x > deltX ? true : false;
                boolean touchOnQuickDelete = getLayoutDirection() == View.LAYOUT_DIRECTION_RTL
                        ? b1 : b2;
                int action = event.getAction();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        if (touchOnQuickDelete && mDeletable) {
                            mShouldHandleDelete = true;
                            if (mDeletePressed != null) {
                                setCompoundDrawables(null, null, mDeletePressed, null);
                            }
                            return true;
                        }

                    case MotionEvent.ACTION_UP:
                        if (touchOnQuickDelete && mDeletable && mShouldHandleDelete) {
                            if (mDeleteNormal != null) {
                                setCompoundDrawables(null, null, mDeleteNormal, null);
                            }
                            if (mTextDeleteListener == null || !mTextDeleteListener.onTextDeleted()) {
                                onFastDelete();
                                mShouldHandleDelete = false;
                                return true;
                            }
                        }

                    case MotionEvent.ACTION_MOVE:
                        if ((x < deltX || y < 0 || y > getHeight())) {
                            if (mDeleteNormal != null) {
                                setCompoundDrawables(null, null, mDeleteNormal, null);
                            }
                        }
                        break;

                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_OUTSIDE:
                        if (mDeleteNormal != null) {
                            setCompoundDrawables(null, null, mDeleteNormal, null);
                        }
                        break;

                    default:
                        break;

                }
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean isSuggestionsEnabled() {
        return false;
    }

    private int getMaxLength() {
        int length = 0;
        try {
            InputFilter[] inputFilters = getFilters();
            for (InputFilter filter : inputFilters) {
                Class<?> c = filter.getClass();
                if (c.getName().equals("android.text.InputFilter$LengthFilter")) {
                    Field[] f = c.getDeclaredFields();
                    for (Field field : f) {
                        if (field.getName().equals("mMax")) {
                            field.setAccessible(true);
                            length = (Integer) field.get(filter);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return length;
    }
}
