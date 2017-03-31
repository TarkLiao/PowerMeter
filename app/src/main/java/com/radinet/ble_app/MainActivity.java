package com.radinet.ble_app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity {

    /**宣告常數**/
    private final int MAX_ENERGE = 999;
    private final int MAX_VOLT = 240;
    private final int MAX_AMP = 50;
    private final int MIN_VALUE = 0;

    /**宣告各Button**/
    private ImageButton Button_Scan;
    private ImageButton Button_Value;
    private ImageButton Button_Setting;
    private ImageButton Button_Energy_Max_Sub;
    private ImageButton Button_Energy_Max_Add;
    private ImageButton Button_Volt_Max_Sub;
    private ImageButton Button_Volt_Max_Add;
    private ImageButton Button_Volt_Min_Sub;
    private ImageButton Button_Volt_Min_Add;
    private ImageButton Button_Amp_Max_Sub;
    private ImageButton Button_Amp_Max_Add;

    /**宣告Button OnClick後的行為**/
    private ButtonPressListener ButtonPressListen;

    /**宣告Display用的主Page**/
    private RelativeLayout Layout_Display;

    /**宣告顯示在主Page的View(相當於真正顯示的內容)**/
    private LayoutInflater layoutInflater;
    private View View_Scan;
    private View View_Value_30A;
    private View View_Value_50A;
    private View View_Setting;
    private Boolean isPage30A;

    /**宣告Checkbox**/
    private CheckBox Checkbox_Option_30A;
    private CheckBox Checkbox_Option_50A;

    /**宣告CheckBox OnClick後的行為**/
    private CheckBoxChangeListener CheckBoxChangeListen;

    /**宣告暫存layout_setting limit的變數**/
    private Limit Limit_Value_30A;
    private Limit Limit_Value_50A;

    /**宣告顯示setting view的LedTextView**/
    private LedTextView LedText_Limit_Energy_Max;
    private LedTextView LedText_Limit_Volt_Max;
    private LedTextView LedText_Limit_Volt_Min;
    private LedTextView LedText_Limit_Amp_Max;

    /**宣告顯示Value view的LedTextView**/
    private LedTextView LedText_Value_Energy_30A;
    private LedTextView LedText_Value_Watt_30A;
    private LedTextView LedText_Value_Volt_30A;
    private LedTextView LedText_Value_Amp_30A;
    private LedTextView LedText_Value_Energy_50A;
    private LedTextView LedText_Value_Watt_50A;
    private LedTextView LedText_Value_Volt_50A;
    private LedTextView LedText_Value_Amp_50A;

    /**宣告長按加減Button相關變數**/
    private ButtonLongPressListener ButtonLongPressListen;
    int mAddSubMultiple = 1;   //加or減button的倍率(初始是1，當Timecount超過10，倍率調整)
    int mTimeCount = 0;        //長按加or減Button，連續執行加or減的次數
    View View_current;         //當按住加or減button後，透過該參數的getId()取得Button ID

    /**宣告各Value的Alarm flag**/
    Boolean mFlag_AlarmEnergy_30A = false;
    Boolean mFlag_AlarmVolt_30A = false;
    Boolean mFlag_AlarmAmp_30A = false;
    Boolean mFlag_AlarmEnergy_50A = false;
    Boolean mFlag_AlarmVolt_50A = false;
    Boolean mFlag_AlarmAmp_50A = false;

    /**Bluetooth BLE**/
    private static final int RQS_ENABLE_BLUETOOTH = 1;
    private static final int REQUEST_FINE_LOCATION_PERMISSION = 102;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private static final long SCAN_PERIOD = 10000;
    private boolean mScanning;
    private boolean mConnected = false;

    //  搜尋到的BluetoothDevice
    List<BluetoothDevice> mListBluetoothDevice;

    //  搜尋到的BluetoothDevice名稱
    List<String> mDevice_list;

    ArrayAdapter<String> mAdapterLeScanResult;

    ListView ListView_BLE;
    private Handler mHandler;
    private BluetoothLeService mBluetoothLeService;
    private String mConnectDeviceName;
    private String mDeviceAddress;

    private Boolean mFlag_Gps = false;
    private Boolean mFirstOpen = true;
    /**宣告儲存Layout_setting的Default**/
    public class Limit
    {
        String Energy_Max = "100";
        String Volt_Max = "200";
        String Volt_Min = "0";
        String Amp_Max = "30";
    }

    /** 宣告從藍牙讀取到的數據變數 **/
    double mValue_Volt = 0;
    double mValue_Amp = 0;
    double mValue_Watt = 0;
    double mValue_Energy = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /**開啟APP後顯示Logo並等待四秒後關閉**/
        InitAndShowLogo();

        /**判斷30A or 50A**/
        //Tark
//        if () {
            isPage30A = true;
//            Checkbox_Option_50A.setVisibility(View.INVISIBLE);
//        } else {
//            isPage30A = false;
//        }

        /**將宣告與物件做連結**/
        LinkObject();

        /**確認setting檔案是否存在，存在的話直接讀取值，不存在的話create檔案並給予Default**/
        CheckSettingFileAndCheckData();

        /**將Setting restore到textview上**/
        InitSettingView();

        /**  監聽各Button單次按下  **/
        ButtonPressListen = new ButtonPressListener();
        Button_Value.setOnClickListener(ButtonPressListen);
        Button_Setting.setOnClickListener(ButtonPressListen);

        /**監聽各Button被長按**/
        ButtonLongPressListen = new ButtonLongPressListener();
        Button_Energy_Max_Sub.setOnTouchListener(ButtonLongPressListen);
        Button_Energy_Max_Add.setOnTouchListener(ButtonLongPressListen);
        Button_Volt_Max_Sub.setOnTouchListener(ButtonLongPressListen);
        Button_Volt_Max_Add.setOnTouchListener(ButtonLongPressListen);
        Button_Volt_Min_Sub.setOnTouchListener(ButtonLongPressListen);
        Button_Volt_Min_Add.setOnTouchListener(ButtonLongPressListen);
        Button_Amp_Max_Sub.setOnTouchListener(ButtonLongPressListen);
        Button_Amp_Max_Add.setOnTouchListener(ButtonLongPressListen);

        /**監聽各CheckBox被按下**/
        CheckBoxChangeListen = new CheckBoxChangeListener();
        if (isPage30A) {
            Checkbox_Option_30A.setChecked(true);
        } else {
            Checkbox_Option_50A.setChecked(true);
        }
        Checkbox_Option_30A.setOnClickListener(CheckBoxChangeListen);
        Checkbox_Option_50A.setOnClickListener(CheckBoxChangeListen);

        /**Bluetooth BLE**/
        /**檢查裝置是否支援BLE，沒有就直接關閉APP**/
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this,
                    "BLUETOOTH_LE not supported in this device!",
                    Toast.LENGTH_SHORT).show();
            finish();
        }

        /**取得Bluetooth Adapter和Bluetooth Scanner**/
        getBluetoothAdapterAndLeScanner();

        mListBluetoothDevice = new ArrayList<>();
        mDevice_list = new ArrayList<>();
        mHandler = new Handler();

        /**用Adapter設定ListView需要顯示的內容及模式**/
        //  simple_list_item_1 只顯示一行文字
        //  mDevice_list    文字顯示的內容
        mAdapterLeScanResult = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_activated_1, mDevice_list);

        /**將Adapter設定至ListView**/
        ListView_BLE.setAdapter(mAdapterLeScanResult);

        /**監聽ListView的項目**/
        ListView_BLE.setOnItemClickListener(scanResultOnItemClickListener);

        /**監聽Button_Scan，點擊後開始scan device**/
        Button_Scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Layout_Display.removeAllViews();
                Layout_Display.addView(View_Scan);
                /**進行scan device**/
                scanLeDevice(mBluetoothAdapter.isEnabled());
            }
        });


    }

    /**↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓物件行為↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓**/
    /**  建立Button按下後的行為  **/
    class ButtonPressListener implements View.OnClickListener{
        //  按Scan Value Setting Button後，切換Page
        public void onClick(View v){
            switch(v.getId()){
                case R.id.Button_Scan:
                    //  先清除所有View
                    Layout_Display.removeAllViews();
                    Layout_Display.addView(View_Scan);
                    break;
                case R.id.Button_Value:
                    //  先清除所有View
                    Layout_Display.removeAllViews();
                    if (isPage30A) {
                        Layout_Display.addView(View_Value_30A);
                    } else {
                        Layout_Display.addView(View_Value_50A);
                    }
                    break;
                case R.id.Button_Setting:
                    //  先清除所有View
                    Layout_Display.removeAllViews();
                    Layout_Display.addView(View_Setting);
                    break;

            }
        }
    }

    /** 監聽長按Button按鍵的狀態  (ButtonLongPressListener Task_ButtonLongPress LongPressHandler為一組)**/
    public class ButtonLongPressListener implements View.OnTouchListener{
        Timer timer;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {

                case MotionEvent.ACTION_DOWN:
                    Log.d("test", "Button按下");
                    timer = new Timer(true);
                    View_current = v;
                    //  第一次執行時delay 0秒，後續每0.1秒執行handle的Task一次
                    timer.schedule(new Task_ButtonLongPress(), 0, 100);
                    break;

                case MotionEvent.ACTION_MOVE:
                    float Button_posX = event.getRawX() - event.getX();
                    float Button_posY = event.getRawY() - event.getY();
                    float Length_posX = Button_Energy_Max_Sub.getWidth();
                    float Length_posY = Button_Energy_Max_Sub.getHeight();
                    if (timer != null && ((event.getRawX() < Button_posX) ||
                            (event.getRawY() < Button_posY) ||
                            (event.getRawX() > (Button_posX + Length_posX)) ||
                            (event.getRawY() > (Button_posY + Length_posY)))) {
                        mTimeCount = 0;
                        mAddSubMultiple = 1;
                        timer.cancel();
                        timer.purge();
                        timer = null;
                        SaveSetting();
                    }
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    mTimeCount = 0;
                    mAddSubMultiple = 1;
                    if (timer != null) {
                        timer.cancel();
                        timer.purge();
                        timer = null;
                        SaveSetting();
                    }
                    break;
            }
            //只要当down返回true时候，系统将不把本次事件记录为点击事件，也就不会触发onClick或者onLongClick事件了
            return true;
        }
    }

    /** 透過handle 處理長按Button的行為 (ButtonLongPressListener Task_ButtonLongPress LongPressHandler為一組)**/
    class Task_ButtonLongPress extends TimerTask {
        public void run() {
            LongPressHandler.sendEmptyMessage(0);
        }
    }

    /** 按加or減Button時的行為   (ButtonLongPressListener Task_ButtonLongPress LongPressHandler為一組)**/
    private Handler LongPressHandler = new Handler() {
        int tmp;
        // 接收到消息后处理
        public void handleMessage(Message msg) {
            //  長按button時，每執行一次加or減，Timecount加1，當Timecount到達11時，加or減的倍數變成10
            mTimeCount ++;
            if (mTimeCount == 11) {
                mAddSubMultiple *= 10;
            }

            //  各種加or減按鍵執行時的行為
            switch(View_current.getId()) {
                case R.id.Button_Energy_Max_Sub:
                    tmp = Integer.parseInt(LedText_Limit_Energy_Max.getText().toString());
                    tmp -= mAddSubMultiple;
                    if (tmp < MIN_VALUE) {
                        tmp = MIN_VALUE;
                    }
                    LedText_Limit_Energy_Max.setText(Integer.toString(tmp));
                    break;
                case R.id.Button_Energy_Max_Add:
                    tmp = Integer.parseInt(LedText_Limit_Energy_Max.getText().toString());
                    tmp += mAddSubMultiple;
                    if (tmp > MAX_ENERGE) {
                        tmp = MAX_ENERGE;
                    }
                    LedText_Limit_Energy_Max.setText(Integer.toString(tmp));
                    break;
                case R.id.Button_Volt_Max_Sub:
                    tmp = Integer.parseInt(LedText_Limit_Volt_Max.getText().toString());
                    tmp -= mAddSubMultiple;
                    if (Integer.parseInt(LedText_Limit_Volt_Min.getText().toString()) > tmp) {
                        tmp = Integer.parseInt(LedText_Limit_Volt_Min.getText().toString());
                    }
                    if (tmp < MIN_VALUE) {
                        tmp = MIN_VALUE;
                    }
                    LedText_Limit_Volt_Max.setText(Integer.toString(tmp));
                    break;
                case R.id.Button_Volt_Max_Add:
                    tmp = Integer.parseInt(LedText_Limit_Volt_Max.getText().toString());
                    tmp += mAddSubMultiple;
                    if (tmp > MAX_VOLT) {
                        tmp = MAX_VOLT;
                    }
                    LedText_Limit_Volt_Max.setText(Integer.toString(tmp));
                    break;
                case R.id.Button_Volt_Min_Sub:
                    tmp = Integer.parseInt(LedText_Limit_Volt_Min.getText().toString());
                    tmp -= mAddSubMultiple;
                    if (tmp < MIN_VALUE) {
                        tmp = MIN_VALUE;
                    }
                    LedText_Limit_Volt_Min.setText(Integer.toString(tmp));
                    break;
                case R.id.Button_Volt_Min_Add:
                    tmp = Integer.parseInt(LedText_Limit_Volt_Min.getText().toString());
                    tmp += mAddSubMultiple;
                    if (Integer.parseInt(LedText_Limit_Volt_Max.getText().toString()) < tmp) {
                        tmp = Integer.parseInt(LedText_Limit_Volt_Max.getText().toString());
                    }
                    if (tmp > MAX_VOLT) {
                        tmp = MAX_VOLT;
                    }
                    LedText_Limit_Volt_Min.setText(Integer.toString(tmp));
                    break;
                case R.id.Button_Amp_Max_Sub:
                    tmp = Integer.parseInt(LedText_Limit_Amp_Max.getText().toString());
                    tmp -= mAddSubMultiple;
                    if (tmp < MIN_VALUE) {
                        tmp = MIN_VALUE;
                    }
                    LedText_Limit_Amp_Max.setText(Integer.toString(tmp));
                    break;
                case R.id.Button_Amp_Max_Add:
                    tmp = Integer.parseInt(LedText_Limit_Amp_Max.getText().toString());
                    tmp += mAddSubMultiple;
                    if (tmp > MAX_AMP) {
                        tmp = MAX_AMP;
                    }
                    LedText_Limit_Amp_Max.setText(Integer.toString(tmp));
                    break;
            }
            super.handleMessage(msg);
        }
    };

    /**  建立CheckBox按下後的行為   **/
    public class CheckBoxChangeListener implements View.OnClickListener {
        public void onClick(View v) {
            //  當checkbox切換時，將textview的Value存進setting，並將另一個setting的value set進textview
            switch (((CheckBox)v).getId()) {
                case R.id.CheckBox_30a:
                    Checkbox_Option_30A.setChecked(true);
                    Limit_Value_50A.Energy_Max = LedText_Limit_Energy_Max.getText().toString();
                    Limit_Value_50A.Volt_Max = LedText_Limit_Volt_Max.getText().toString();
                    Limit_Value_50A.Volt_Min = LedText_Limit_Volt_Min.getText().toString();
                    Limit_Value_50A.Amp_Max = LedText_Limit_Amp_Max.getText().toString();
                    Checkbox_Option_50A.setChecked(false);
                    LedText_Limit_Energy_Max.setText(Limit_Value_30A.Energy_Max);
                    LedText_Limit_Volt_Max.setText(Limit_Value_30A.Volt_Max);
                    LedText_Limit_Volt_Min.setText(Limit_Value_30A.Volt_Min);
                    LedText_Limit_Amp_Max.setText(Limit_Value_30A.Amp_Max);
                    break;
                case R.id.CheckBox_50a:
                    Checkbox_Option_30A.setChecked(false);
                    Limit_Value_30A.Energy_Max = LedText_Limit_Energy_Max.getText().toString();
                    Limit_Value_30A.Volt_Max = LedText_Limit_Volt_Max.getText().toString();
                    Limit_Value_30A.Volt_Min = LedText_Limit_Volt_Min.getText().toString();
                    Limit_Value_30A.Amp_Max = LedText_Limit_Amp_Max.getText().toString();
                    Checkbox_Option_50A.setChecked(true);
                    LedText_Limit_Energy_Max.setText(Limit_Value_50A.Energy_Max);
                    LedText_Limit_Volt_Max.setText(Limit_Value_50A.Volt_Max);
                    LedText_Limit_Volt_Min.setText(Limit_Value_50A.Volt_Min);
                    LedText_Limit_Amp_Max.setText(Limit_Value_50A.Amp_Max);
                    break;
            }
        }
    }

    /**建立ListView的項目按下後的行為  **/
    AdapterView.OnItemClickListener scanResultOnItemClickListener = new AdapterView.OnItemClickListener(){
        int device_index;
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            //  取得ListView點擊到的項目名稱，並藉由項目名稱去取得List的索引
            //  mDevice_list及mListBluetoothDevice的項目索引值是相同的，所以透過索引值可以直接去取得BluetoothDevice
            final String device_name = (String) parent.getItemAtPosition(position);
            device_index = mDevice_list.indexOf(device_name);
            final BluetoothDevice device = mListBluetoothDevice.get(device_index);

            mDeviceAddress = device.getAddress();
            String msg = mDeviceAddress + "\n"
                    + device.getBluetoothClass().toString() + "\n";

            new AlertDialog.Builder(MainActivity.this)
                    .setTitle(device.getName())
                    .setMessage(msg)
                    .setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    })
                    .setNeutralButton("CONNECT", new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //  進行Connect後，停止Scan
                            if (mScanning) {
                                mBluetoothLeScanner.stopScan(ScanCallback);
//                                Toast.makeText(MainActivity.this, "Scan finish", Toast.LENGTH_LONG).show();
                                mScanning = false;
                            }

                            //  紀錄連接的DeviceName
                            mConnectDeviceName = device.getName();

                            if (mBluetoothLeService == null) {
                                //  透過 Intent 方式帶出另一個畫面 DeviceControlActivity
                                //  同時把藍芽位址資料也傳過去，創建畫面的同時也建立 BluetoothLeService 類別 (衍生自 service 類別)
                                //  這裡利用一個技巧，讓服務在背景執行，然後將畫面與服務 bind 在一起
                                Intent gattServiceIntent = new Intent(MainActivity.this, BluetoothLeService.class);
                                bindService(gattServiceIntent, ServiceConnection, BIND_AUTO_CREATE);

                                //  藍芽服務有任何訊息要通知 UI 畫面，向系統註冊 callback 函式用來處理服務的訊息
                                registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

                            } else {
                                if (mBluetoothLeService.mBluetoothGatt != null && !mDeviceAddress.equals(mBluetoothLeService.mBluetoothDeviceAddress)) {
                                    //  如果連接的裝置跟上次不同，則將Gatt釋放掉
                                    mBluetoothLeService.mBluetoothGatt.disconnect();
                                    mBluetoothLeService.mBluetoothGatt.close();
                                    final boolean result = mBluetoothLeService.connect(mDeviceAddress);
                                    Log.d(BluetoothLeService.TAG, "Connect request result=" + result);
                                } else if (mBluetoothLeService.mBluetoothGatt != null && mDeviceAddress.equals(mBluetoothLeService.mBluetoothDeviceAddress)) {
                                    //  如果連接的裝置跟上次相同，則重連
                                    final boolean result = mBluetoothLeService.connect(mDeviceAddress);
                                    Log.d(BluetoothLeService.TAG, "Connect request result=" + result);
                                }
                            }
                        }
                    })
                    .show();
        }
    };

    /**按返回鍵時，等同按HOME**/
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK)
        {
            // Show home screen when pressing "back" button,
            //  so that this app won't be closed accidentally
            Intent intentHome = new Intent(Intent.ACTION_MAIN);
            intentHome.addCategory(Intent.CATEGORY_HOME);
            intentHome.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intentHome);

            return true;
        }

        return super.onKeyDown(keyCode, event);
    }
    /**↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑物件行為↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑**/
    /**↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓Function↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓**/
    /**開啟APP後顯示Logo並等待四秒後關閉**/
    private void InitAndShowLogo() {
        final ImageView Start_Imageview = (ImageView) findViewById(R.id.Imageview_Start);
        final ImageView Background_Imageview = (ImageView) findViewById(R.id.Imageview_Background);
        Background_Imageview.bringToFront();
        Start_Imageview.bringToFront();

        Handler StartPicturehandler = new Handler();
        StartPicturehandler.postDelayed(new Runnable(){
            @Override
            public void run() {
                //  過四秒後將圖片刪除
                Background_Imageview.setVisibility(View.GONE);
                Start_Imageview.setVisibility(View.GONE);
            }}, 4000);
    }

    /**將宣告與物件做連結**/
    private void LinkObject() {
        //  Display與物件進行連結
        Layout_Display = (RelativeLayout)findViewById(R.id.display_RelativeLayout);

        //  View與物件做連結
        layoutInflater = (LayoutInflater)this.getSystemService(LAYOUT_INFLATER_SERVICE);
        View_Value_30A = layoutInflater.inflate(R.layout.layout_value_30a, null);
        View_Value_50A = layoutInflater.inflate(R.layout.layout_value_50a, null);
        View_Scan = layoutInflater.inflate(R.layout.layout_scan, null);
        View_Setting = layoutInflater.inflate(R.layout.layout_setting, null);

        //  Button與物件做連結
        Button_Scan = (ImageButton)findViewById(R.id.Button_Scan);
        Button_Value = (ImageButton)findViewById(R.id.Button_Value);
        Button_Setting = (ImageButton)findViewById(R.id.Button_Setting);
        Button_Energy_Max_Sub = (ImageButton)View_Setting.findViewById(R.id.Button_Energy_Max_Sub);
        Button_Energy_Max_Add = (ImageButton)View_Setting.findViewById(R.id.Button_Energy_Max_Add);
        Button_Volt_Max_Sub = (ImageButton)View_Setting.findViewById(R.id.Button_Volt_Max_Sub);
        Button_Volt_Max_Add = (ImageButton)View_Setting.findViewById(R.id.Button_Volt_Max_Add);
        Button_Volt_Min_Sub = (ImageButton)View_Setting.findViewById(R.id.Button_Volt_Min_Sub);
        Button_Volt_Min_Add = (ImageButton)View_Setting.findViewById(R.id.Button_Volt_Min_Add);
        Button_Amp_Max_Sub = (ImageButton)View_Setting.findViewById(R.id.Button_Amp_Max_Sub);
        Button_Amp_Max_Add = (ImageButton)View_Setting.findViewById(R.id.Button_Amp_Max_Add);

        // check與物件做連結，非activity_main的物件，所以需要該layout的View
        Checkbox_Option_30A = (CheckBox)View_Setting.findViewById(R.id.CheckBox_30a);
        Checkbox_Option_50A = (CheckBox)View_Setting.findViewById(R.id.CheckBox_50a);

        //  LedTextView與物件做連結
        LedText_Limit_Energy_Max = (LedTextView)View_Setting.findViewById(R.id.Textview_Energy_Max);
        LedText_Limit_Volt_Max = (LedTextView)View_Setting.findViewById(R.id.Textview_Volt_Max);
        LedText_Limit_Volt_Min = (LedTextView)View_Setting.findViewById(R.id.Textview_Volt_Min);
        LedText_Limit_Amp_Max = (LedTextView)View_Setting.findViewById(R.id.Textview_Amp_Max);
        if (isPage30A) {
            LedText_Value_Energy_30A = (LedTextView)View_Value_30A.findViewById(R.id.Textview_Value_Energy_30a);
            LedText_Value_Watt_30A = (LedTextView)View_Value_30A.findViewById(R.id.Textview_Value_Watt_30a);
            LedText_Value_Volt_30A = (LedTextView)View_Value_30A.findViewById(R.id.Textview_Value_Volt_30a);
            LedText_Value_Amp_30A = (LedTextView)View_Value_30A.findViewById(R.id.Textview_Value_Amp_30a);
        } else {
            LedText_Value_Energy_30A = (LedTextView)View_Value_50A.findViewById(R.id.Textview_Value_Energy_30a);
            LedText_Value_Watt_30A = (LedTextView)View_Value_50A.findViewById(R.id.Textview_Value_Watt_30a);
            LedText_Value_Volt_30A = (LedTextView)View_Value_50A.findViewById(R.id.Textview_Value_Volt_30a);
            LedText_Value_Amp_30A = (LedTextView)View_Value_50A.findViewById(R.id.Textview_Value_Amp_30a);
            LedText_Value_Energy_50A = (LedTextView)View_Value_50A.findViewById(R.id.Textview_Value_Energy_50a);
            LedText_Value_Watt_50A = (LedTextView)View_Value_50A.findViewById(R.id.Textview_Value_Watt_50a);
            LedText_Value_Volt_50A = (LedTextView)View_Value_50A.findViewById(R.id.Textview_Value_Volt_50a);
            LedText_Value_Amp_50A = (LedTextView)View_Value_50A.findViewById(R.id.Textview_Value_Amp_50a);
        }

        //  ListView與物件做連結
        ListView_BLE = (ListView)View_Scan.findViewById(R.id.ListView_BLE);
    }

    /**確認setting檔案是否存在，存在的話直接讀取值，不存在的話create檔案並給予Default**/
    private void CheckSettingFileAndCheckData() {
        String Content;
        String Filename_30A = "File_30A.txt";
        File File_30A = new File(getFilesDir().getAbsolutePath() + "/" + Filename_30A);
        Limit_Value_30A = new Limit();

        if (File_30A.exists()) {
            Log.d("Test", "File_30A Exists");
            try {
                FileInputStream Input = openFileInput(Filename_30A);
                DataInputStream DataInput = new DataInputStream(Input);
                BufferedReader Reader = new BufferedReader(new InputStreamReader(DataInput));
                Content = Reader.readLine();
                Log.d("test", "Content:" + Content);
                Input.close();

                String[] token = Content.split(" ");

                if (token.length != 4) {
                    File_30A.delete();
                } else {
                    Limit_Value_30A.Energy_Max = token[0];
                    Limit_Value_30A.Volt_Max = token[1];
                    Limit_Value_30A.Volt_Min = token[2];
                    Limit_Value_30A.Amp_Max = token[3];
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (!File_30A.exists()) {
            Log.d("Test", "File_30A Not Exists");
            try {
                FileOutputStream Output = openFileOutput(Filename_30A, Context.MODE_PRIVATE);
                Output.write((Limit_Value_30A.Energy_Max + " " +
                        Limit_Value_30A.Volt_Max + " " +
                        Limit_Value_30A.Volt_Min + " " +
                        Limit_Value_30A.Amp_Max).toString().getBytes());
                Output.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
//        File_30A.delete();

        String Filename_50A = "File_50A.txt";
        File File_50A = new File(getFilesDir().getAbsolutePath() + "/" + Filename_50A);
        Limit_Value_50A = new Limit();

        if (File_50A.exists()) {
            Log.d("Test", "File_50A Exists");
            try {
                FileInputStream Input = openFileInput(Filename_50A);
                DataInputStream DataInput = new DataInputStream(Input);
                BufferedReader Reader = new BufferedReader(new InputStreamReader(DataInput));
                Content = Reader.readLine();
                Log.d("test", "Content:" + Content);
                Input.close();
                String[] token = Content.split(" ");
                if (token.length != 4) {
                    File_50A.delete();
                } else {
                    Limit_Value_50A.Energy_Max = token[0];
                    Limit_Value_50A.Volt_Max = token[1];
                    Limit_Value_50A.Volt_Min = token[2];
                    Limit_Value_50A.Amp_Max = token[3];
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (!File_50A.exists()) {
            Log.d("Test", "File_50A Not Exists");
            try {
                FileOutputStream Output = openFileOutput(Filename_50A, Context.MODE_PRIVATE);
                Output.write((Limit_Value_50A.Energy_Max + " " +
                        Limit_Value_50A.Volt_Max + " " +
                        Limit_Value_50A.Volt_Min + " " +
                        Limit_Value_50A.Amp_Max).toString().getBytes());
                Output.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
//        File_50A.delete();
    }

    /**將Setting 傳到textview上**/
    private void InitSettingView() {
        if (isPage30A) {
            LedText_Limit_Energy_Max.setText(Limit_Value_30A.Energy_Max);
            LedText_Limit_Volt_Max.setText(Limit_Value_30A.Volt_Max);
            LedText_Limit_Volt_Min.setText(Limit_Value_30A.Volt_Min);
            LedText_Limit_Amp_Max.setText(Limit_Value_30A.Amp_Max);
        } else {
            LedText_Limit_Energy_Max.setText(Limit_Value_50A.Energy_Max);
            LedText_Limit_Volt_Max.setText(Limit_Value_50A.Volt_Max);
            LedText_Limit_Volt_Min.setText(Limit_Value_50A.Volt_Min);
            LedText_Limit_Amp_Max.setText(Limit_Value_50A.Amp_Max);
        }
    }

    /** 跳出Dialog視窗
     *  String AlarmString: Dialog的視窗內容
     * **/
    private void AlarmDialog (String AlarmString) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Warning")
                .setMessage(AlarmString)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
        AlertDialog about_dialog = builder.create();
        about_dialog.show();
    }

    /**更新limit value且輸出至檔案**/
    private void SaveSetting() {
        if (Checkbox_Option_30A.isChecked()) {
            try {
                FileOutputStream Output = openFileOutput("File_30A.txt", Context.MODE_PRIVATE);
                Output.write((LedText_Limit_Energy_Max.getText().toString() + " " +
                        LedText_Limit_Volt_Max.getText().toString() + " " +
                        LedText_Limit_Volt_Min.getText().toString() + " " +
                        LedText_Limit_Amp_Max.getText().toString()).toString().getBytes());
                Output.close();
                Limit_Value_30A.Energy_Max = LedText_Limit_Energy_Max.getText().toString();
                Limit_Value_30A.Volt_Max = LedText_Limit_Volt_Max.getText().toString();
                Limit_Value_30A.Volt_Min = LedText_Limit_Volt_Min.getText().toString();
                Limit_Value_30A.Amp_Max = LedText_Limit_Amp_Max.getText().toString();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (Checkbox_Option_50A.isChecked()) {
            try {
                FileOutputStream Output = openFileOutput("File_50A.txt", Context.MODE_PRIVATE);
                Output.write((LedText_Limit_Energy_Max.getText().toString() + " " +
                        LedText_Limit_Volt_Max.getText().toString() + " " +
                        LedText_Limit_Volt_Min.getText().toString() + " " +
                        LedText_Limit_Amp_Max.getText().toString()).toString().getBytes());
                Output.close();
                Limit_Value_50A.Energy_Max = LedText_Limit_Energy_Max.getText().toString();
                Limit_Value_50A.Volt_Max = LedText_Limit_Volt_Max.getText().toString();
                Limit_Value_50A.Volt_Min = LedText_Limit_Volt_Min.getText().toString();
                Limit_Value_50A.Amp_Max = LedText_Limit_Amp_Max.getText().toString();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**Bluetooth BLE**/
    /**Get Service**/
    private final ServiceConnection ServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            //  取得mBluetoothLeService
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();

            //  取得mBluetoothManager 及 mBluetoothAdapter
            if (!mBluetoothLeService.initialize()) {
                Log.e("Paul", "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    /**1.藍牙若沒開，詢問是否開啟
     * 2.GPS沒開，詢問是否跳到設定頁面**/
    @Override
    protected void onResume() {

        /**Bluetooth未開啟的話，要求使用者開啟**/
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, RQS_ENABLE_BLUETOOTH);
        }

        if (Build.VERSION.SDK_INT >= 23) {
            int location_permission = checkSelfPermission(
                    Manifest.permission.ACCESS_COARSE_LOCATION);
            //  要求存取GPS權限
            if (location_permission != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                                Manifest.permission.ACCESS_COARSE_LOCATION},
                        REQUEST_FINE_LOCATION_PERMISSION);
            }
            /**若GPS未開，則詢問是否跳轉到設定頁面**/
            LocationManager locationManager = (LocationManager) this.getSystemService(
                    Context.LOCATION_SERVICE);
            if (!locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
                if (!mFlag_Gps) {
                    if (mFirstOpen) {
                        mFlag_Gps = true;
                    }
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("GPS")
                            .setMessage("Gps is disable, change to setting page to enable it?\n" +
                                    "if you already enabled, confirm that it is not network mode.")
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Toast.makeText(MainActivity.this, "Gps is disable", Toast.LENGTH_SHORT).show();
                                    finish();
                                }
                            })
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //  跳到(GPS)定位系統的設定頁面
                                    Intent enableGpsintent = new Intent(
                                            Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                    startActivityForResult(enableGpsintent, 0);
                                }
                            })
                            .show();
                }

            }
            if (mBluetoothAdapter.isEnabled() &&
                    locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) &&
                    !mConnected) {
                /**添加layout_Value的View進Display**/
                Layout_Display.removeAllViews();
                Layout_Display.addView(View_Scan);
                scanLeDevice(mBluetoothAdapter.isEnabled());
                mFlag_Gps = false;
                mFirstOpen = false;
            }
            super.onResume();
        }
    }

    /**對應startActivityForResult**/
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //  startActivityForResult未開啟藍牙，則結束APP
        if (requestCode == RQS_ENABLE_BLUETOOTH && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        //  重新Get，因為未開啟藍牙，則OnCreate時無法正確Get Scanner
        getBluetoothAdapterAndLeScanner();

        if (mBluetoothLeScanner == null) {
            Toast.makeText(this, "mBluetoothLeScanner==null", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    /**當存取權限允許或被拒**/
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch(requestCode) {
            case REQUEST_FINE_LOCATION_PERMISSION:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //取得權限
                } else {
                    //使用者拒絕權限
                    finish();
                }
        }
        return;
    }

    /**取得Bluetooth Adapter和Bluetooth Scanner**/
    private void getBluetoothAdapterAndLeScanner() {
        // Get BluetoothAdapter and BluetoothLeScanner.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "mBluetoothAdapter == Null", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        }
        mScanning = false;
    }

    /**進行scan device**/
    private void scanLeDevice(final boolean enable) {
        if (enable) {//  當Adapter是開啟的
            if (!mScanning) {//  當目前沒有在Scan
                //  清除List
                mListBluetoothDevice.clear();
                mDevice_list.clear();

                //  重新顯示listView
                ListView_BLE.invalidateViews();

                // 停止Scan，當Scan時間超過SCAN_PERIOD
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mBluetoothLeScanner.stopScan(ScanCallback);
                        if (mScanning) {
//                            Toast.makeText(MainActivity.this,
//                                    "Scan timeout",
//                                    Toast.LENGTH_LONG).show();
                            mScanning = false;
                        }
                    }
                }, SCAN_PERIOD);
//                Toast.makeText(MainActivity.this, "Scanning", Toast.LENGTH_LONG).show();
                mBluetoothLeScanner.startScan(ScanCallback);
                mScanning = true;
            }
        } else {
            mBluetoothLeScanner.stopScan(ScanCallback);
            mScanning = false;
        }
    }

    /**設定Scan CallBack**/
    private ScanCallback ScanCallback = new ScanCallback() {
        //  Return Scan到的設備
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            addBluetoothDevice(result.getDevice());
        }

        //  Return所有Scan到的設備
        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            for(ScanResult result : results){
                addBluetoothDevice(result.getDevice());
            }
        }

        //  Scan失敗
        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Toast.makeText(MainActivity.this,
                    "onScanFailed: " + String.valueOf(errorCode),
                    Toast.LENGTH_LONG).show();
        }

        private void addBluetoothDevice(BluetoothDevice device){
            if(device.getName() != null) {
                //  判斷Device存不存在BluetoothList
                if (!mListBluetoothDevice.contains(device)) {
                    //  將Device存進BluetoothList
                    mListBluetoothDevice.add(device);
                    //  將Device名稱存進Device_list
                    mDevice_list.add(device.getName().toString());
                    //  重新顯示listView
                    ListView_BLE.invalidateViews();
                }
            }
        }
    };

    /**註冊GattServer會發生CallBack的事件**/
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    /**接收GattServer傳來的訊息**/
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        String action;
        @Override
        public void onReceive(Context context, Intent intent) {
            action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                Layout_Display.removeAllViews();
                if (isPage30A) {
                    Layout_Display.addView(View_Value_30A);
                } else {
                    Layout_Display.addView(View_Value_50A);
                }
                Log.d("Paul", "Connected");
                Toast.makeText(MainActivity.this, mConnectDeviceName + " Connected", Toast.LENGTH_SHORT).show();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                Log.d("Paul", "Connect Fail");
                Toast.makeText(MainActivity.this, mConnectDeviceName + " disconnected", Toast.LENGTH_SHORT).show();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                //  若傳遞藍牙資訊過來Call function Refresh
                Refresh(intent.getByteArrayExtra(mBluetoothLeService.ACTION_DATA_AVAILABLE));
            }
        }
    };

    /**解析從藍牙讀取的資料**/
    private void Decode_data(byte[] data) {
        mValue_Volt = Byte42double(data, 3) / 10000;
        mValue_Amp = Byte42double(data, 7) / 10000;
        mValue_Watt = Byte42double(data, 11) / 10000;
        mValue_Energy = Byte42double(data, 15) / 10000;
//        Log.d("print data", "Volt:" + String.valueOf(mValue_Volt) + " Amp:" + String.valueOf(mValue_Amp) + " Watt:" + String.valueOf(mValue_Watt) + "Energy" + String.valueOf(mValue_Energy));
    }

    /**將Byte[4]轉成Double**/
    private double Byte42double(byte[] data, int sAddr) {
        double[] mIndex;
        mIndex = new double[4];

        for(int i = 0; i < 4; i++) {
            mIndex[i] = data[sAddr + i] & 0x00000000000000FF;
//            Log.d("test", String.valueOf(i) +":" +String.valueOf(mIndex[i]));
        }
        return((mIndex[0] * 16777216) + (mIndex[1] * 65536) + (mIndex[2] * 256) + mIndex[3]);
    }

    /**進行Refresh的行為**/
    private void Refresh(byte[] Data) {
        //顯示幾位
        DecimalFormat DecimalFormat = new DecimalFormat("#.##");
        //位數不足補零
        DecimalFormat.applyPattern("0.00");
        if (Data != null && Data[0] == 0x01 && Data[1] == 0x03 && Data[2] == 0x20) {
            Log.d("test", "NeedData");
            Decode_data(Data);
//                        Log.d("test", "success");
//                        //StringBuffer buffer = new StringBuffer("0x");
//                        int i;
//                        int c1 = 0;
//                        //Log.d("test", "1:");
//                        for (byte b : Data) {
//                            i = b & 0xff;
//                            //buffer.append(Integer.toHexString(i));
//                            Log.d("test", "read data:"+ String.valueOf(c1)+":" + Integer.toHexString(i));
//                            c1++;
//                        }
        }

        Vibrator vb = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        //  當30A的Energy超出setting範圍，改變字體顏色跳出Dialog視窗及震動
        if (mValue_Energy > Integer.parseInt(Limit_Value_30A.Energy_Max)) {
            if (!mFlag_AlarmEnergy_30A) {
                mFlag_AlarmEnergy_30A = true;
                AlarmDialog("Over Energy scope for 30A!");
                vb.vibrate(2000); //震動
            }
            LedText_Value_Energy_30A.setTextColor(Color.rgb(255, 0, 0));
        } else {
            mFlag_AlarmEnergy_30A = false;
            LedText_Value_Energy_30A.setTextColor(Color.rgb(255, 255, 255));
        }
        LedText_Value_Energy_30A.setText(DecimalFormat.format(mValue_Energy));

        LedText_Value_Watt_30A.setText(DecimalFormat.format(mValue_Watt));

        //  當30A的Volt超出setting範圍，改變字體顏色、跳出Dialog視窗及震動
        if (mValue_Volt > Integer.parseInt(Limit_Value_30A.Volt_Max) ||
                mValue_Volt < Integer.parseInt(Limit_Value_30A.Volt_Min)) {
            if (!mFlag_AlarmVolt_30A) {
                mFlag_AlarmVolt_30A = true;
                AlarmDialog("Over Volt scope for 30A!");
                vb.vibrate(2000); //震動
            }
            LedText_Value_Volt_30A.setTextColor(Color.rgb(255, 0, 0));
        } else {
            mFlag_AlarmVolt_30A = false;
            LedText_Value_Volt_30A.setTextColor(Color.rgb(255, 255, 255));
        }
        LedText_Value_Volt_30A.setText(DecimalFormat.format(mValue_Volt));

        //  當30A的Amp超出setting範圍，改變字體顏色、跳出Dialog視窗及震動
        if (mValue_Amp > Integer.parseInt(Limit_Value_30A.Amp_Max)) {
            if (!mFlag_AlarmAmp_30A) {
                mFlag_AlarmAmp_30A = true;
                AlarmDialog("Over Amp scope for 30A!");
                vb.vibrate(2000); //震動
            }
            LedText_Value_Amp_30A.setTextColor(Color.rgb(255, 0, 0));
        } else {
            mFlag_AlarmAmp_30A = false;
            LedText_Value_Amp_30A.setTextColor(Color.rgb(255, 255, 255));
        }
        LedText_Value_Amp_30A.setText(DecimalFormat.format(mValue_Amp));

        if (!isPage30A) {
            //  當50A的Energy超出setting範圍，改變字體顏色、跳出Dialog視窗及震動
            if (mValue_Energy > Integer.parseInt(Limit_Value_50A.Energy_Max)) {
                if (!mFlag_AlarmEnergy_50A) {
                    mFlag_AlarmEnergy_50A = true;
                    AlarmDialog("Over Energy scope for 50A!");
                    vb.vibrate(2000); //震動
                }
                LedText_Value_Energy_50A.setTextColor(Color.rgb(255, 0, 0));
            } else {
                mFlag_AlarmEnergy_50A = false;
                LedText_Value_Energy_50A.setTextColor(Color.rgb(255, 255, 255));
            }
            LedText_Value_Energy_50A.setText(DecimalFormat.format(mValue_Energy));

            LedText_Value_Watt_50A.setText(DecimalFormat.format(mValue_Watt));

            //  當50A的Volt超出setting範圍，改變字體顏色、跳出Dialog視窗及震動
            if (mValue_Volt > Integer.parseInt(Limit_Value_50A.Volt_Max) ||
                    mValue_Volt < Integer.parseInt(Limit_Value_50A.Volt_Min)) {
                if (!mFlag_AlarmVolt_50A) {
                    mFlag_AlarmVolt_50A = true;
                    AlarmDialog("Over Volt scope for 50A!");
                    vb.vibrate(2000); //震動
                }
                LedText_Value_Volt_50A.setTextColor(Color.rgb(255, 0, 0));
            } else {
                mFlag_AlarmVolt_50A = false;
                LedText_Value_Volt_50A.setTextColor(Color.rgb(255, 255, 255));
            }
            LedText_Value_Volt_50A.setText(DecimalFormat.format(mValue_Volt));

            //  當50A的Amp超出setting範圍，改變字體顏色、跳出Dialog視窗及震動
            if (mValue_Amp > Integer.parseInt(Limit_Value_50A.Amp_Max)) {
                if (!mFlag_AlarmAmp_50A) {
                    mFlag_AlarmAmp_50A = true;
                    AlarmDialog("Over Amp scope for 50A!");
                    vb.vibrate(2000); //震動
                }
                LedText_Value_Amp_50A.setTextColor(Color.rgb(255, 0, 0));
            } else {
                mFlag_AlarmAmp_50A = false;
                LedText_Value_Amp_50A.setTextColor(Color.rgb(255, 255, 255));
            }
            LedText_Value_Amp_50A.setText(DecimalFormat.format(mValue_Amp));
        }
    }
    /**↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑Function↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑**/
}
