package com.radinet.ble_app;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import android.widget.TextView;

import java.util.List;

public class ListDeleteAdapter extends BaseAdapter
{
    private Activity activity;
    private List<String> mList;

    private static LayoutInflater inflater = null;

    public ListDeleteAdapter(Activity a, List<String> list)
    {
        activity = a;
        mList = list;
        inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public int getCount()
    {
        return mList.size();
    }

    public Object getItem(int position)
    {
        return position;
    }

    public long getItemId(int position)
    {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent)
    {
        View vi = convertView;
        if(convertView==null)
        {
            vi = inflater.inflate(R.layout.layout_settingdelete_item, null);
        }

        TextView chkIndex = (TextView) vi.findViewById(R.id.check_index);
//        CheckBox chkBshow = (CheckBox) vi.findViewById(R.id.check_delete_device);
        CheckedTextView chkBshow = (CheckedTextView) vi.findViewById(R.id.check_delete_device);
        chkBshow.setCheckMarkDrawable(R.drawable.select_checkbox);
        TextView chkConent = (TextView) vi.findViewById(R.id.check_item_text);
        chkConent.setText(mList.get(position).toString());
//        chkBshow.setText(mList.get(position).toString());
        chkIndex.setText((position+1) + ":");
        chkBshow.setChecked(false);
        return vi;
    }
}