package com.radinet.ble_app;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Tark on 2017/10/12.
 */

public class ListSetAdapter  extends BaseAdapter
{
    private Activity activity;
    private List<String> mList = new ArrayList<>();

    private static LayoutInflater inflater = null;

    public ListSetAdapter(Activity a, List<String> list)
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
            vi = inflater.inflate(R.layout.layout_settingset_item, null);
        }

        TextView chkConent = (TextView) vi.findViewById(R.id.check_item_text);
        chkConent.setText(mList.get(position).toString());
        return vi;
    }
}
