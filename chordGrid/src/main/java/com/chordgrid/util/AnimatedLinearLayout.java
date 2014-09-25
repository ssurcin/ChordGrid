package com.chordgrid.util;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;

import com.chordgrid.R;

public class AnimatedLinearLayout extends LinearLayout {

    private Context context;
    private Animation inAnimation;
    private Animation outAnimation;

    public AnimatedLinearLayout(Context context) {
        super(context);
        this.context = context;
        initAnimations();
    }

    public AnimatedLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        initAnimations();
    }

    public AnimatedLinearLayout(Context context, AttributeSet attrs,
                                int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
        initAnimations();
    }

    private void initAnimations() {
        if (isInEditMode())
            return;
        inAnimation = (Animation) AnimationUtils.loadAnimation(context,
                R.anim.in_animation);
        outAnimation = (Animation) AnimationUtils.loadAnimation(context,
                R.anim.out_animation);
    }

    public void show() {
        if (isVisible())
            return;
        show(!isInEditMode());
    }

    public void show(boolean withAnimation) {
        if (withAnimation)
            this.startAnimation(inAnimation);
        this.setVisibility(View.VISIBLE);
    }

    public void hide() {
        if (!isVisible())
            return;
        hide(!isInEditMode());
    }

    public void hide(boolean withAnimation) {
        if (withAnimation)
            this.startAnimation(outAnimation);
        this.setVisibility(View.GONE);
    }

    public boolean isVisible() {
        return (this.getVisibility() == View.VISIBLE);
    }

    public void overrideDefaultInAnimation(Animation inAnimation) {
        this.inAnimation = inAnimation;
    }

    public void overrideDefaultOutAnimation(Animation outAnimation) {
        this.outAnimation = outAnimation;
    }
}
