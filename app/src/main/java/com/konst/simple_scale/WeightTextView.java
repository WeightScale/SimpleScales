package com.konst.simple_scale;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.util.AttributeSet;
import android.widget.ProgressBar;

/*
 * Created by Kostya on 18.11.2014.
 */
public class WeightTextView extends ProgressBar {
    private String text = "";
    private int textColor = Color.WHITE;
    //private float textSize = getResources().getDimension(R.dimen.text_big);
    private final Paint textPaint;
    private final Rect bounds;

    public WeightTextView(Context context) {
        super(context);
        textPaint = new Paint();
        bounds = new Rect();
        textPaint.setAntiAlias(true);
        //textPaint.setTextSize(textSize);
    }

    public WeightTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        textPaint = new Paint();
        bounds = new Rect();
        textPaint.setAntiAlias(true);
        //textPaint.setTextSize(textSize);
    }

    public synchronized void updateProgress(int progress, int color, float size) {
        SpannableStringBuilder kg = new SpannableStringBuilder(getResources().getString(R.string.scales_kg));
        SpannableStringBuilder weight = new SpannableStringBuilder(String.valueOf(progress));
        kg.setSpan(new AbsoluteSizeSpan(getResources().getDimensionPixelSize(R.dimen.text_large_xx)),0,kg.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        weight.setSpan(new AbsoluteSizeSpan(getResources().getDimensionPixelSize(R.dimen.text_big)),0,weight.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        CharSequence finalText = TextUtils.concat(weight, " ", kg);
        //setText(progress + getResources().getString(R.string.scales_kg));
        setText(finalText.toString());
        //textSize = size;
        textColor = color;
        drawableStateChanged();
    }

    public synchronized void updateProgress(String progress, int color, float size) {
        setText(progress);
        //textSize = size;
        textColor = color;
        drawableStateChanged();
    }

    @Override
    protected synchronized void onDraw( Canvas canvas) {
        super.onDraw(canvas);
        textPaint.setColor(textColor);
        //textPaint.setTextSize(textSize);
        //Rect bounds = new Rect();
        textPaint.getTextBounds(text, 0, text.length(), bounds);
        int x = getWidth() - bounds.right - (int) getResources().getDimension(R.dimen.padding);
        int y = getHeight() / 2 - bounds.centerY();
        canvas.drawText(text, x, y, textPaint);
    }

    private void setText(String text) {
        this.text = text;
        postInvalidate();
    }

    private void setTextColor(int color) {
        textColor = color;
        postInvalidate();
    }

}
