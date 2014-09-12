/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.activity;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.method.ScrollingMovementMethod;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import org.ciasaboark.tacere.R;
import org.ciasaboark.tacere.database.TooltipManager;

public class ChromeHelpPopup {

    protected WindowManager mWindowManager;

    protected Context mContext;
    protected PopupWindow mWindow;
    protected View mView;
    protected Drawable mBackgroundDrawable = null;
    protected ShowListener showListener;
    private TextView mHelpTextView;
    private ImageView mUpImageView;
    private ImageView mDownImageView;
    private View mPopupShadow;
    private Integer mHighlightColor;

    public ChromeHelpPopup(Context context, String text) {
        this(context);

        setText(text);
    }

    public ChromeHelpPopup(Context context) {
        this(context, "", R.layout.popup);

    }

    public ChromeHelpPopup(Context context, String text, int viewResource) {
        mContext = context;
        mWindow = new PopupWindow(context);

        mWindowManager = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);


        LayoutInflater layoutInflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        setContentView(layoutInflater.inflate(viewResource, null));

        mHelpTextView = (TextView) mView.findViewById(R.id.text);
        mUpImageView = (ImageView) mView.findViewById(R.id.arrow_up);
        mDownImageView = (ImageView) mView.findViewById(R.id.arrow_down);
//        mPopupShadow = (View) mView.findViewById(R.id.popup_shadow);

        mHelpTextView.setMovementMethod(ScrollingMovementMethod.getInstance());
        mHelpTextView.setSelected(true);
    }

    public void setContentView(View root) {
        mView = root;

        mWindow.setContentView(root);
    }

    public String getText() {
        CharSequence text = mHelpTextView.getText();
        return text == null ? null : text.toString();
    }

    public void setText(String text) {
        mHelpTextView.setText(text);
    }

    public void setHighlightColor(int color) {
        mHighlightColor = color;
    }

    public void show(View anchor) {
        preShow();

        int[] location = new int[2];

        anchor.getLocationOnScreen(location);

        Rect anchorRect = new Rect(location[0], location[1], location[0]
                + anchor.getWidth(), location[1] + anchor.getHeight());

        mView.measure(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

        int rootHeight = mView.getMeasuredHeight();
        int rootWidth = mView.getMeasuredWidth();

        final int screenWidth = mWindowManager.getDefaultDisplay().getWidth();
        final int screenHeight = mWindowManager.getDefaultDisplay().getHeight();

        int yPos = anchorRect.top - rootHeight;

        boolean onTop = true;

        if (anchorRect.top < screenHeight / 2) {
            yPos = anchorRect.bottom;
            onTop = false;
        }

        int whichArrow, requestedX;

        whichArrow = ((onTop) ? R.id.arrow_down : R.id.arrow_up);
        requestedX = anchorRect.centerX();

        View arrow = whichArrow == R.id.arrow_up ? mUpImageView
                : mDownImageView;
        View hideArrow = whichArrow == R.id.arrow_up ? mDownImageView
                : mUpImageView;

        final int arrowWidth = arrow.getMeasuredWidth();

        Drawable arrowDrawable = arrow.getBackground();

        int desiredColor;
        if (mHighlightColor == null) {
            desiredColor = mContext.getResources().getColor(R.color.accent);
        } else {
            desiredColor = mHighlightColor;
        }

        if (arrowDrawable != null) {
            arrowDrawable.mutate().setColorFilter(desiredColor, PorterDuff.Mode.MULTIPLY);
            arrow.setBackgroundDrawable(arrowDrawable);
        }
        GradientDrawable textBackground = (GradientDrawable) mHelpTextView.getBackground();
        textBackground.setColor(desiredColor);
        textBackground.setStroke(1, desiredColor);


        arrow.setVisibility(View.VISIBLE);


        ViewGroup.MarginLayoutParams param = (ViewGroup.MarginLayoutParams) arrow
                .getLayoutParams();

        hideArrow.setVisibility(View.INVISIBLE);

        int xPos = 0;

        // ETXTREME RIGHT CLIKED
        if (anchorRect.left + rootWidth > screenWidth) {
            xPos = (screenWidth - rootWidth);
        }
        // ETXTREME LEFT CLIKED
        else if (anchorRect.left - (rootWidth / 2) < 0) {
            xPos = anchorRect.left;
        }
        // INBETWEEN
        else {
            xPos = (anchorRect.centerX() - (rootWidth / 2));
        }

        param.leftMargin = (requestedX - xPos) - (arrowWidth / 2);

        if (onTop) {
            mHelpTextView.setMaxHeight(anchorRect.top - anchorRect.height());

        } else {
            mHelpTextView.setMaxHeight(screenHeight - yPos);
        }

        mWindow.showAtLocation(anchor, Gravity.NO_GRAVITY, xPos, yPos);

        mView.setAnimation(AnimationUtils.loadAnimation(mContext,
                R.anim.float_anim));

    }

    protected void preShow() {
        if (mView == null)
            throw new IllegalStateException("view undefined");


        if (showListener != null) {
            showListener.onPreShow();
            showListener.onShow();
        }

        if (mBackgroundDrawable == null)
            mWindow.setBackgroundDrawable(new BitmapDrawable());
        else
            mWindow.setBackgroundDrawable(mBackgroundDrawable);

        mWindow.setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
        mWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        mWindow.setTouchable(true);
        mWindow.setFocusable(true);
        mWindow.setOutsideTouchable(true);

        mWindow.setContentView(mView);
    }

    public void setBackgroundDrawable(Drawable background) {
        mBackgroundDrawable = background;
    }

    public void setContentView(int layoutResID) {
        LayoutInflater inflator = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        setContentView(inflator.inflate(layoutResID, null));
    }


    public void setOnDismissListener(PopupWindow.OnDismissListener listener) {
        mWindow.setOnDismissListener(listener);
    }

    public void dismiss() {
        mWindow.dismiss();
        if (showListener != null) {
            showListener.onDismiss();
        }
        TooltipManager ttm = new TooltipManager(this, mContext);
        ttm.broadcastTooltipDismissedMessage();
    }

    public void setShowListener(ShowListener showListener) {
        this.showListener = showListener;
    }

    public static interface ShowListener {
        void onPreShow();

        void onDismiss();

        void onShow();
    }
}