package com.radinet.ble_app;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.util.AttributeSet;

//AppCompatTextView AppCompatCheckedTextView AppCompatAutoCompleteTextView
public class LedTextView extends android.support.v7.widget.AppCompatTextView
{

    public LedTextView(Context context)
    {
        super(context);
        init(context);
    }

    public LedTextView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init(context);
    }

    public LedTextView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context)
    {
        //字体资源放入assets文件夹中
        AssetManager am=context.getAssets();
        Typeface font=Typeface.createFromAsset(am, "fonts/digital-7-italic.ttf");
        setTypeface(font);
    }

}