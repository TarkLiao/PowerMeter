package com.radinet.ble_app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
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

//import com.google.zxing.BarcodeFormat;
//import com.google.zxing.EncodeHintType;
//import com.google.zxing.MultiFormatWriter;
//import com.google.zxing.WriterException;
//import com.google.zxing.common.BitMatrix;
import com.google.zxing.integration.android.*;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
//import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
//
//import java.util.EnumMap;
//import java.util.Map;

public class MainActivity extends AppCompatActivity {

    /**
     * 宣告常數
     **/
    private final int MAX_ENERGE = 999;
    private final int MAX_VOLT = 240;
    private final int MAX_AMP = 50;
    private final int MIN_VALUE = 0;

    /**
     * 宣告各Button
     **/
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
    private Button Button_Qrcode_Scan;

    /**
     * 宣告Button OnClick後的行為
     **/
    private ButtonPressListener ButtonPressListen;

    /**
     * 宣告ImageView，作為Scan的轉圈圈動畫
     */
    private ImageView Imageview_scan;
    private AnimationDrawable AnimationDrawable;

    /**
     * 宣告Display用的主Page
     **/
    private RelativeLayout Layout_Display;

    /**
     * 宣告顯示在主Page的View(相當於真正顯示的內容)
     **/
    private LayoutInflater layoutInflater;
    private View View_Scan;
    private View View_Value_30A;
    private View View_Value_50A;
    private View View_Setting;
    private Boolean isPage30A;

    /**
     * 宣告顯示在正連線的裝置
     **/
    private TextView Textview_connected;

    /**
     * 宣告Checkbox
     **/
    private CheckBox Checkbox_Option_30A;
    private CheckBox Checkbox_Option_50A;

    /**
     * 宣告CheckBox OnClick後的行為
     **/
    private CheckBoxChangeListener CheckBoxChangeListen;

    /**
     * 宣告暫存layout_setting limit的變數
     **/
    private Limit Limit_Value_30A;
    private Limit Limit_Value_50A;

    /**
     * 宣告顯示setting view的LedTextView
     **/
    private LedTextView LedText_Limit_Energy_Max;
    private LedTextView LedText_Limit_Volt_Max;
    private LedTextView LedText_Limit_Volt_Min;
    private LedTextView LedText_Limit_Amp_Max;

    /**
     * 宣告顯示Value view的LedTextView
     **/
    private LedTextView LedText_Value_Energy_30A;
    private LedTextView LedText_Value_Watt_30A;
    private LedTextView LedText_Value_Volt_30A;
    private LedTextView LedText_Value_Amp_30A;
    private LedTextView LedText_Value_Volt_50A;
    private LedTextView LedText_Value_Amp_50A;

    /**
     * 宣告長按加減Button相關變數
     **/
    private ButtonLongPressListener ButtonLongPressListen;
    int mAddSubMultiple = 1;   //加or減button的倍率(初始是1，當Timecount超過10，倍率調整)
    int mTimeCount = 0;        //長按加or減Button，連續執行加or減的次數
    View View_current;         //當按住加or減button後，透過該參數的getId()取得Button ID

    /**
     * 宣告各Value的Alarm flag
     **/
    Boolean mFlag_AlarmEnergy_30A = false;
    Boolean mFlag_AlarmVolt_30A = false;
    Boolean mFlag_AlarmAmp_30A = false;
    Boolean mFlag_AlarmEnergy_50A = false;
    Boolean mFlag_AlarmVolt_50A = false;
    Boolean mFlag_AlarmAmp_50A = false;

    /**
     * 相機
     */
    private static final int REQUEST_CAMREA_PERMISSION = 10;

    /**
     * Bluetooth BLE
     **/
    private static final int RQS_ENABLE_BLUETOOTH = 1;
    private static final int REQUEST_FINE_LOCATION_PERMISSION = 102;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private static final long SCAN_PERIOD = 5000;
    private boolean mScanning;
    private boolean mConnected = false;

    //  搜尋到的BluetoothDevice
    List<BluetoothDevice> mListBluetoothDevice;

    //  搜尋到的BluetoothDevice名稱
    List<String> mDevice_list;

    //  搜尋到的BluetoothDevice adress
    List<String> mDeviceAdress_list;

    //  用作儲存已連接過的Bluetooth Device Address
    List<String> mConnected_list;

    //Scan到的Device數量
    private int mScanDeviceNum;

    ArrayAdapter<String> mAdapterLeScanResult;

    ListView ListView_BLE;
    private Handler mHandler;
    private BluetoothLeService mBluetoothLeService;
    private String mConnectDeviceName;
    private String mDeviceAddress;

    /**
     * 宣告從藍牙讀取到的數據變數
     **/
    double mValue_Volt_30A = 0;
    double mValue_Amp_30A = 0;
    double mValue_Watt = 0;
    double mValue_Energy = 0;

    double mValue_Volt_50A = 0;
    double mValue_Amp_50A = 0;

    //判斷開啟APP等四秒的Logo結束沒
    private Boolean mAppLogoEnd = false;

    //判斷GPS是否開啟
    private Boolean mGpsOpen = false;

    //開啟APP自動Scan
    private static Timer AutoScanTimer;

    //Toast訊息用，解決訊息會重疊的情況
    private Toast mToast = null;

    //系統通知用(通知匣)
    NotificationManager mNotifiyManager;

    //點亮螢幕用
    PowerManager.WakeLock wl;

    //Android 5.0.2 需不斷Scan才能掃到所有裝置
    Timer mTimerScan;

    /**
     * 宣告儲存Layout_setting的Default
     **/
    public class Limit {
        String Energy_Max = "100";
        String Volt_Max = "200";
        String Volt_Min = "0";
        String Amp_Max = "30";
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /**開啟APP後顯示Logo並等待四秒後關閉**/
        InitAndShowLogo();

        /**設定顯示30介面**/
        isPage30A = true;

        /**將宣告與Common物件做連結**/
        LinkObjectCommon();

        /**將Value的textview與物件做連結**/
        LinkObjectValue();

        /**確認setting檔案是否存在，存在的話直接讀取值，不存在的話create檔案並給予Default**/
        CheckSettingFileAndCheckData();

        /**確認connect過的Device，存在讀取DeviceName，不存在則無**/
        CheckConnectedData();

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

        //  設置Scan動畫，屬性設定不可視
        Imageview_scan.setImageResource(R.drawable.scan_animation);
        AnimationDrawable = (AnimationDrawable) Imageview_scan.getDrawable();
        Imageview_scan.setVisibility(View.INVISIBLE);

        /**取得Bluetooth Adapter和Bluetooth Scanner**/
        getBluetoothAdapterAndLeScanner();

        mListBluetoothDevice = new ArrayList<>();
        mDevice_list = new ArrayList<>();
        mDeviceAdress_list = new ArrayList<>();
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
                if (Build.VERSION.SDK_INT >= 23) {
                    scanLeDevice(mBluetoothAdapter.isEnabled());
                } else {
                    if (mTimerScan != null) {
                        mTimerScan.cancel();
                        mTimerScan = null;
                    }
                    if (mScanning) {
                        mHandler.removeCallbacks(ScanTimeRun);
                    }
                    mHandler.postDelayed(ScanTimeRun, SCAN_PERIOD);
                    mScanning = false;
                    Imageview_scan.setVisibility(View.INVISIBLE);
                    AnimationDrawable.stop();
                    mTimerScan = new Timer(true);
                    //5.0.2須不斷Scan才能掃到裝置
                    mTimerScan.schedule(new Task_ScanContinued(), 0, 500);
                }
            }
        });

        Button_Qrcode_Scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= 23) {
                    int location_permission = checkSelfPermission(
                            Manifest.permission.CAMERA);
                    if (location_permission != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{
                                Manifest.permission.CAMERA}, REQUEST_CAMREA_PERMISSION);
                    } else {
                        IntentIntegrator scanIntegrator = new IntentIntegrator(MainActivity.this);
                        scanIntegrator.initiateScan();
                    }
                } else {
                    IntentIntegrator scanIntegrator = new IntentIntegrator(MainActivity.this);
                    scanIntegrator.initiateScan();
                }
            }
        });
    }

    /**↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓物件行為↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓**/
    /**
     * 建立Button按下後的行為
     **/
    class ButtonPressListener implements View.OnClickListener {
        //  按Scan Value Setting Button後，切換Page
        public void onClick(View v) {
            switch (v.getId()) {
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

    /**
     * 監聽長按Button按鍵的狀態  (ButtonLongPressListener Task_ButtonLongPress LongPressHandler為一組)
     **/
    public class ButtonLongPressListener implements View.OnTouchListener {
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
            //只要當down返回true時候，系統將不把本次事件紀錄為點擊事件，也就不會觸發onClick或者onLongClick事件了
            return true;
        }
    }

    /**
     * 透過handle 處理長按Button的行為 (ButtonLongPressListener Task_ButtonLongPress LongPressHandler為一組)
     **/
    class Task_ButtonLongPress extends TimerTask {
        public void run() {
            LongPressHandler.sendEmptyMessage(0);
        }
    }

    /**
     * 按加or減Button時的行為   (ButtonLongPressListener Task_ButtonLongPress LongPressHandler為一組)
     **/
    private Handler LongPressHandler = new Handler() {
        int tmp;

        // 接收到消息後處理
        public void handleMessage(Message msg) {
            //  長按button時，每執行一次加or減，Timecount加1，當Timecount到達11時，加or減的倍數變成10
            mTimeCount++;
            if (mTimeCount == 11) {
                mAddSubMultiple *= 10;
            }

            //  各種加or減按鍵執行時的行為
            switch (View_current.getId()) {
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

    /**
     * 建立CheckBox按下後的行為
     **/
    public class CheckBoxChangeListener implements View.OnClickListener {
        public void onClick(View v) {
            //  當checkbox切換時，將textview的Value存進setting，並將另一個setting的value set進textview
            switch (((CheckBox) v).getId()) {
                case R.id.CheckBox_30a:
                    isPage30A = true;
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
                    isPage30A = false;
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
            LinkObjectValue();
        }
    }

    /**
     * 建立ListView的項目按下後的行為
     **/
    AdapterView.OnItemClickListener scanResultOnItemClickListener = new AdapterView.OnItemClickListener() {
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
                            if (mScanning && mBluetoothAdapter.isEnabled()) {
                                mBluetoothLeScanner.stopScan(ScanCallback);
                                mHandler.removeCallbacks(ScanTimeRun);
                                mScanning = false;
                                Imageview_scan.setVisibility(View.INVISIBLE);
                                AnimationDrawable.stop();
                                if (mTimerScan != null) {
                                    mTimerScan.cancel();
                                    mTimerScan = null;
                                }
                            }

                            //  紀錄連接的DeviceName
                            mConnectDeviceName = device.getName();

                            // 紀錄連接過的Device並進行排列，越後面時間越新
                            WriteConnectedDevice ();

                            //  進行Device連接
                            ConnectDevice (mDeviceAddress);
                        }
                    })
                    .show();
        }
    };

    /**
     * 按返回鍵時，等同按HOME
     **/
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK) {
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
    /**
     * 開啟APP後顯示Logo並等待四秒後關閉
     **/
    private void InitAndShowLogo() {
        final ImageView Background_Imageview = (ImageView) findViewById(R.id.Imageview_Background);
        final LinearLayout Layout_Logo = (LinearLayout) findViewById(R.id.Layout_Logo);
        Background_Imageview.bringToFront();
        Layout_Logo.bringToFront();

        TextView Logo_tv = (TextView) findViewById(R.id.Textview_Logo);
        Logo_tv.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/anton.ttf"));
        Handler StartPicturehandler = new Handler();
        StartPicturehandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //  過四秒後將圖片刪除
                Background_Imageview.setVisibility(View.GONE);
                Layout_Logo.setVisibility(View.GONE);
                mAppLogoEnd = true;

                //圖片被刪除後，執行自動Scan
                AutoScanTimer = new Timer(true);
                AutoScanTimer.schedule(new Task_AutoScan(), 0, 100);
            }
        }, 4000);
    }

    /**
     * 將宣告與物件做連結
     **/
    private void LinkObjectCommon() {
        //  Display與物件進行連結
        Layout_Display = (RelativeLayout) findViewById(R.id.display_RelativeLayout);

        //  View與物件做連結
        layoutInflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
        View_Value_30A = layoutInflater.inflate(R.layout.layout_value_30a, null);
        View_Value_50A = layoutInflater.inflate(R.layout.layout_value_50a, null);
        View_Scan = layoutInflater.inflate(R.layout.layout_scan, null);
        View_Setting = layoutInflater.inflate(R.layout.layout_setting, null);

        //  Button與物件做連結
        Button_Scan = (ImageButton) findViewById(R.id.Button_Scan);
        Button_Value = (ImageButton) findViewById(R.id.Button_Value);
        Button_Setting = (ImageButton) findViewById(R.id.Button_Setting);
        Button_Energy_Max_Sub = (ImageButton) View_Setting.findViewById(R.id.Button_Energy_Max_Sub);
        Button_Energy_Max_Add = (ImageButton) View_Setting.findViewById(R.id.Button_Energy_Max_Add);
        Button_Volt_Max_Sub = (ImageButton) View_Setting.findViewById(R.id.Button_Volt_Max_Sub);
        Button_Volt_Max_Add = (ImageButton) View_Setting.findViewById(R.id.Button_Volt_Max_Add);
        Button_Volt_Min_Sub = (ImageButton) View_Setting.findViewById(R.id.Button_Volt_Min_Sub);
        Button_Volt_Min_Add = (ImageButton) View_Setting.findViewById(R.id.Button_Volt_Min_Add);
        Button_Amp_Max_Sub = (ImageButton) View_Setting.findViewById(R.id.Button_Amp_Max_Sub);
        Button_Amp_Max_Add = (ImageButton) View_Setting.findViewById(R.id.Button_Amp_Max_Add);
        Button_Qrcode_Scan = (Button) View_Setting.findViewById(R.id.Button_Add_device);

        // check與物件做連結，非activity_main的物件，所以需要該layout的View
        Checkbox_Option_30A = (CheckBox) View_Setting.findViewById(R.id.CheckBox_30a);
        Checkbox_Option_50A = (CheckBox) View_Setting.findViewById(R.id.CheckBox_50a);

        //  LedTextView與物件做連結
        LedText_Limit_Energy_Max = (LedTextView) View_Setting.findViewById(R.id.Textview_Energy_Max);
        LedText_Limit_Volt_Max = (LedTextView) View_Setting.findViewById(R.id.Textview_Volt_Max);
        LedText_Limit_Volt_Min = (LedTextView) View_Setting.findViewById(R.id.Textview_Volt_Min);
        LedText_Limit_Amp_Max = (LedTextView) View_Setting.findViewById(R.id.Textview_Amp_Max);

        //  ListView與物件做連結
        ListView_BLE = (ListView) View_Scan.findViewById(R.id.ListView_BLE);

        //  ImageView與物件做連結
        Imageview_scan = (ImageView)View_Scan.findViewById(R.id.Imageview_animation);
    }
    /**
     * 將宣告與物件做連結
     **/
    private void LinkObjectValue() {
        if (isPage30A) {
            Textview_connected = (TextView) View_Value_30A.findViewById(R.id.textview_connected_30A);
            LedText_Value_Energy_30A = (LedTextView) View_Value_30A.findViewById(R.id.Textview_Value_Energy_30a);
            LedText_Value_Watt_30A = (LedTextView) View_Value_30A.findViewById(R.id.Textview_Value_Watt_30a);
            LedText_Value_Volt_30A = (LedTextView) View_Value_30A.findViewById(R.id.Textview_Value_Volt_30a);
            LedText_Value_Amp_30A = (LedTextView) View_Value_30A.findViewById(R.id.Textview_Value_Amp_30a);
        } else {
            Textview_connected = (TextView) View_Value_30A.findViewById(R.id.textview_connected_50A);
            LedText_Value_Energy_30A = (LedTextView) View_Value_50A.findViewById(R.id.Textview_Value_Energy_30a);
            LedText_Value_Watt_30A = (LedTextView) View_Value_50A.findViewById(R.id.Textview_Value_Watt_30a);
            LedText_Value_Volt_30A = (LedTextView) View_Value_50A.findViewById(R.id.Textview_Value_Volt_30a);
            LedText_Value_Amp_30A = (LedTextView) View_Value_50A.findViewById(R.id.Textview_Value_Amp_30a);
            LedText_Value_Volt_50A = (LedTextView) View_Value_50A.findViewById(R.id.Textview_Value_Volt_50a);
            LedText_Value_Amp_50A = (LedTextView) View_Value_50A.findViewById(R.id.Textview_Value_Amp_50a);
        }
    }
    /**
     * 確認setting檔案是否存在，存在的話直接讀取值，不存在的話create檔案並給予Default
     **/
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

    /**
     * 確認connect過的Device，存在則讀取並儲存DeviceName，不存在則無
     **/
    private void CheckConnectedData() {
        String Content;
        String Filename_Connected = "Connected.txt";
        File File_Connected = new File(getFilesDir().getAbsolutePath() + "/" + Filename_Connected);
        mConnected_list = new ArrayList<>();

        if (File_Connected.exists()) {
            Log.d("Test", "File_Connected Exists");
            try {
                //讀取File_Connected，存進mConnected_list
                FileInputStream Input = openFileInput(Filename_Connected);
                DataInputStream DataInput = new DataInputStream(Input);
                BufferedReader Reader = new BufferedReader(new InputStreamReader(DataInput));
                Content = Reader.readLine();
                Log.d("test", "Connect Content:" + Content);
                Input.close();

                String[] token = Content.split(",");
                for (String tmp : token) {
                    mConnected_list.add(tmp);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 將Setting 傳到textview上
     **/
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

    /**
     * 紀錄連接過的Device並進行排列，越後面時間越新
     */
    private void WriteConnectedDevice () {
        String Filename_Connected = "Connected.txt";
        File File_Connected = new File(getFilesDir().getAbsolutePath() + "/" + Filename_Connected);

        if (!File_Connected.exists()) {
            //如果File_Connected不存在，則直接將連接的裝置名稱存進檔案
            Log.d("Test", "File_Connected Not Exists");
            try {
                FileOutputStream Output = openFileOutput(Filename_Connected, Context.MODE_PRIVATE);
                Output.write(mDeviceAddress.toString().getBytes());
                Output.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            // 若檔案存在，代表mConnected_list有資料
            // 連線的Device若有在mConnected_list內，先刪掉再加入，則會擺在最後面(latest)
            // 若mConnected_list沒有，則直接擺最後面
            Log.d("Test", "File_Connected Exists");
            if (mConnected_list.contains(mDeviceAddress)) {
                Log.d("test", String.valueOf(mConnected_list.indexOf(mDeviceAddress)));
                mConnected_list.remove(mDeviceAddress);
                mConnected_list.add(mDeviceAddress);
                Log.d("test2", String.valueOf(mConnected_list.indexOf(mDeviceAddress)));
            } else {
                mConnected_list.add(mDeviceAddress);
            }
            Log.d ("test3", String.valueOf(mConnected_list.size()));

            //  將排列好順序的mConnected_list寫入檔案
            try {
                FileOutputStream Output = openFileOutput(Filename_Connected, Context.MODE_PRIVATE);
                for (String tmp : mConnected_list) {
                    Output.write((tmp + ",").toString().getBytes());
                }
                Output.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 跳出Dialog視窗
     * String AlarmString: Dialog的視窗內容
     **/
    private void AlarmDialog(final int NotifyId, String AlarmString) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Warning")
                .setMessage(AlarmString)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mNotifiyManager.cancel(NotifyId);
                    }
                });
        AlertDialog about_dialog = builder.create();
        about_dialog.show();
    }

    /**
     * 更新limit value且輸出至檔案
     **/
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
    /**
     * Get Service
     **/
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

    /**
     * 1.藍牙若沒開，詢問是否開啟
     * 2.詢問是否可以存取位置權限
     * 3.GPS沒開，詢問是否跳到設定頁面
     **/
    @Override
    protected void onResume() {

        /**Bluetooth未開啟的話，要求使用者開啟**/
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, RQS_ENABLE_BLUETOOTH);
        } else {

            if (Build.VERSION.SDK_INT >= 23) {
                int location_permission = checkSelfPermission(
                        Manifest.permission.ACCESS_COARSE_LOCATION);

                /**要求存取GPS權限，請求結束後會再跑一次Resume**/
                if (location_permission != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{
                                    Manifest.permission.ACCESS_COARSE_LOCATION},
                            REQUEST_FINE_LOCATION_PERMISSION);
                } else {
                    LocationManager locationManager = (LocationManager) this.getSystemService(
                            Context.LOCATION_SERVICE);

                    /**若GPS未開，則詢問是否跳轉到設定頁面**/
                    mGpsOpen = locationManager.isProviderEnabled(
                            android.location.LocationManager.GPS_PROVIDER);

                    if (!mGpsOpen) {
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
            }
        }
        super.onResume();
    }

    /**
     * 透過handle 處理自動Scan的行為
     **/
    class Task_AutoScan extends TimerTask {
        public void run() {
            AutoScanHandle.sendEmptyMessage(0);
        }
    }

    /**
     * 當Bluetooth、GPS、未連線且LOGO結束，自動進行Scan
     **/
    private Handler AutoScanHandle = new Handler() {
        Boolean Bluetooth;

        public void handleMessage(Message msg) {
            Bluetooth = mBluetoothAdapter.isEnabled();
            if (Build.VERSION.SDK_INT >= 23) {
                if (Bluetooth && mGpsOpen && !mConnected && mAppLogoEnd) {
                    AutoScanTimer.cancel();
                    AutoScanTimer = null;
                    /**添加layout_Value的View進Display**/
                    Layout_Display.removeAllViews();
                    Layout_Display.addView(View_Scan);
                    scanLeDevice(Bluetooth);
                }
            } else {
                if (Bluetooth && !mConnected && mAppLogoEnd) {
                    AutoScanTimer.cancel();
                    AutoScanTimer = null;
                    /**添加layout_Value的View進Display**/
                    Layout_Display.removeAllViews();
                    Layout_Display.addView(View_Scan);

                    mScanning = false;
                    Imageview_scan.setVisibility(View.INVISIBLE);
                    AnimationDrawable.stop();

                    mHandler.postDelayed(ScanTimeRun, SCAN_PERIOD);
                    mTimerScan = new Timer(true);
                    //5.0.2須不斷Scan才能掃到裝置
                    mTimerScan.schedule(new Task_ScanContinued(), 0, 500);
                }
            }
            super.handleMessage(msg);
        }
    };

    /**
     * 透過handle，處理持續Scan的行為(5.0.2)
     **/
    class Task_ScanContinued extends TimerTask {
        public void run() {
            ScanContinuedHandler.sendEmptyMessage(0);
        }
    }

    /**
     * 開始Scan後，不停Stop後Start，但不清除搜尋到的列表，FOR Android 5.0.2
     * 若不是Scan中，則清除列表且印出Scanning訊息
     **/
    private Handler ScanContinuedHandler = new Handler() {
        Boolean Bluetooth;
        public void handleMessage(Message msg) {
            Bluetooth = mBluetoothAdapter.isEnabled();
            if (Bluetooth) {
                if (mScanning) {
                    mBluetoothLeScanner.stopScan(ScanCallback);
                    mBluetoothLeScanner.startScan(ScanCallback);
                    ListView_BLE.invalidateViews();
                } else {
                    mScanDeviceNum = 0;
                    //  清除List
                    mListBluetoothDevice.clear();
                    mDevice_list.clear();
                    mDeviceAdress_list.clear();
                    ListView_BLE.invalidateViews();
                    if (mToast == null) {
                        mToast = Toast.makeText(MainActivity.this, "Scanning", Toast.LENGTH_LONG);
                    } else {
                        mToast.setText("Scanning");
                    }
                    mToast.show();
                    Imageview_scan.setVisibility(View.VISIBLE);
                    AnimationDrawable.start();
                }
                mScanning = true;
            } else {
                if (mTimerScan != null) {
                    mTimerScan.cancel();
                    mTimerScan = null;
                }
                mScanning = false;
                Imageview_scan.setVisibility(View.INVISIBLE);
                AnimationDrawable.stop();
            }
            super.handleMessage(msg);
        }
    };

    /**
     * 對應startActivityForResult
     **/
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

    /**
     * 當存取權限允許或被拒
     **/
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_FINE_LOCATION_PERMISSION:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //取得權限
                } else {
                    //使用者拒絕權限
                    finish();
                }
                break;
            case REQUEST_CAMREA_PERMISSION:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //取得權限
                    IntentIntegrator scanIntegrator = new IntentIntegrator(MainActivity.this);
                    scanIntegrator.initiateScan();
                } else {
                    //使用者拒絕權限

                }
                break;
        }
        return;
    }

    /**
     * 取得Bluetooth Adapter和Bluetooth Scanner
     **/
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
        if (Imageview_scan.VISIBLE == View.VISIBLE) {
            Imageview_scan.setVisibility(View.INVISIBLE);
            AnimationDrawable.stop();
        }
    }

    /**
     * 進行scan device
     **/
    private void scanLeDevice(final boolean enable) {
        mScanDeviceNum = 0;
        if (enable) {//  當Adapter是開啟的
            if (mScanning) {//  當Scanning，關閉Scan，取消正在postDelayed的ScanCallback
                mBluetoothLeScanner.stopScan(ScanCallback);
                mHandler.removeCallbacks(ScanTimeRun);
            }
            //  清除List
            mListBluetoothDevice.clear();
            mDevice_list.clear();
            mDeviceAdress_list.clear();

            //  重新顯示listView
            ListView_BLE.invalidateViews();

            // 當Scan時間超過SCAN_PERIOD，執行Runable_ScanTime
            mHandler.postDelayed(ScanTimeRun, SCAN_PERIOD);
            mBluetoothLeScanner.startScan(ScanCallback);

            if (mToast == null) {
                mToast = Toast.makeText(MainActivity.this, "Scanning", Toast.LENGTH_LONG);
            } else {
                mToast.setText("Scanning");
            }
            mToast.show();
            mScanning = true;
            Imageview_scan.setVisibility(View.VISIBLE);
            AnimationDrawable.start();
        } else {
            mScanning = false;
            Imageview_scan.setVisibility(View.INVISIBLE);
            AnimationDrawable.stop();
        }
    }

    /**
     * Scan Device finish時間
     **/
    private Runnable ScanTimeRun = new Runnable() {
        Boolean Bluetooth;
        @Override
        public void run() {
            Bluetooth = mBluetoothAdapter.isEnabled();
            if (mTimerScan != null) {
                mTimerScan.cancel();
                mTimerScan = null;
            }
            if (Bluetooth) {
                mBluetoothLeScanner.stopScan(ScanCallback);

                //Scan到的裝置數量不是0
                if (mScanDeviceNum != 0) {
                    //scan finish 自動連線
                    //條件 1.未Connect 2.有連接過的Device且存在mConnected_list中
                    AutoConnect();
                    if (mToast == null) {
                        mToast = Toast.makeText(MainActivity.this, "Scan finish", Toast.LENGTH_LONG);
                    } else {
                        mToast.setText("Scan finish");
                    }
                } else {
                    //Device not found
                    if (mToast == null) {
                        mToast = Toast.makeText(MainActivity.this, "Device not found", Toast.LENGTH_LONG);
                    } else {
                        mToast.setText("Device not found");
                    }
                }
                mToast.show();
            } else {
                Toast.makeText(MainActivity.this,
                        "Bluetooth is close",
                        Toast.LENGTH_LONG).show();
            }
            mScanning = false;
            Imageview_scan.setVisibility(View.INVISIBLE);
            AnimationDrawable.stop();
        }
    };

    /**
     * 設定Scan CallBack
     **/
    private ScanCallback ScanCallback = new ScanCallback() {
        //  Return Scan到的設備
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            addBluetoothDevice(result.getDevice());
        }

//        //  Return所有Scan到的設備
//        @Override
//        public void onBatchScanResults(List<ScanResult> results) {
//            super.onBatchScanResults(results);
//            for (ScanResult result : results) {
//                addBluetoothDevice(result.getDevice());
//            }
//        }

        //  Scan失敗
        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Toast.makeText(MainActivity.this,
                    "onScanFailed: " + String.valueOf(errorCode),
                    Toast.LENGTH_LONG).show();
        }

        private void addBluetoothDevice(BluetoothDevice device) {
            if (device.getName() != null) {
                //  判斷Device存不存在BluetoothList
                if (!mListBluetoothDevice.contains(device)) {
                    mScanDeviceNum ++;
                    //  將Device存進BluetoothList
                    mListBluetoothDevice.add(device);
                    //  將Device名稱存進Device_list
                    mDevice_list.add(device.getName().toString());
                    mDeviceAdress_list.add(device.getAddress().toString());
                    //  重新顯示listView
                    ListView_BLE.invalidateViews();
                }
            }
        }
    };

    /**
     * 註冊GattServer會發生CallBack的事件
     **/
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    /**
     * 接收GattServer傳來的訊息
     **/
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        String action;

        @Override
        public void onReceive(Context context, Intent intent) {
            action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                Textview_connected.setText(mConnectDeviceName);
                Layout_Display.removeAllViews();
                if (isPage30A) {
                    Layout_Display.addView(View_Value_30A);
                } else {
                    Layout_Display.addView(View_Value_50A);
                }
                Log.d("Paul", "Connected");
                if (mToast == null) {
                    mToast = Toast.makeText(MainActivity.this, mConnectDeviceName + " connected", Toast.LENGTH_SHORT);
                } else {
                    mToast.setText(mConnectDeviceName + " connected");
                }
                mToast.show();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                Textview_connected.setText("Empty");
                Log.d("Paul", "Connect Fail");
                if (mToast == null) {
                    mToast = Toast.makeText(MainActivity.this, mConnectDeviceName + " disconnected", Toast.LENGTH_SHORT);
                } else {
                    mToast.setText(mConnectDeviceName + " disconnected");
                }
                SystemNotification(1, "Device", mConnectDeviceName + " disconnected");
                mToast.show();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                //  若傳遞藍牙資訊過來Call function Refresh
                Refresh(intent.getByteArrayExtra(mBluetoothLeService.ACTION_DATA_AVAILABLE));
            }
        }
    };

    /**
     * 解析從藍牙讀取的資料
     **/
    private void Decode_data(byte[] data) {
        double Value_Watt_30A;
        double Value_Watt_50A;
        double Value_Energy_30A;
        double Value_Energy_50A;

        mValue_Volt_30A = Byte42double(data, 3) / 10000;
        mValue_Amp_30A = Byte42double(data, 7) / 10000;
        Value_Watt_30A = Byte42double(data, 11) / 10000;
        Value_Energy_30A = Byte42double(data, 15) / 10000;

        Value_Watt_50A = 0;
        Value_Energy_50A = 0;
        mValue_Volt_50A = 0;
        mValue_Amp_50A = 0;

        if (isPage30A) {
            mValue_Watt = Value_Watt_30A;
            mValue_Energy = Value_Energy_30A;
        } else {
            mValue_Watt = Value_Watt_30A + Value_Watt_50A;
            mValue_Energy = Value_Energy_30A + Value_Energy_50A;
        }
//        Log.d("print data", "Volt:" + String.valueOf(mValue_Volt_30A) + " Amp:" + String.valueOf(mValue_Amp_30A) + " Watt:" + String.valueOf(mValue_Watt_30A) + "Energy" + String.valueOf(mValue_Energy_30A));
    }

    /**
     * 將Byte[4]轉成Double
     **/
    private double Byte42double(byte[] data, int sAddr) {
        double[] mIndex;
        mIndex = new double[4];

        for (int i = 0; i < 4; i++) {
            mIndex[i] = data[sAddr + i] & 0x00000000000000FF;
//            Log.d("test", String.valueOf(i) +":" +String.valueOf(mIndex[i]));
        }
        return ((mIndex[0] * 16777216) + (mIndex[1] * 65536) + (mIndex[2] * 256) + mIndex[3]);
    }

    /**
     * 進行Refresh的行為
     **/
    private void Refresh(byte[] Data) {
        int NotifiyId;
        String Tmp_Max;
        String Tmp_Min;
        //顯示幾位
        DecimalFormat DecimalFormat_Common = new DecimalFormat("#");
        DecimalFormat DecimalFormat_Energy = new DecimalFormat("#.0");
        DecimalFormat DecimalFormat_Amp = new DecimalFormat("#.0");
        DecimalFormat_Common.setRoundingMode(RoundingMode.HALF_UP);
        DecimalFormat_Energy.setRoundingMode(RoundingMode.HALF_UP);
        DecimalFormat_Amp.setRoundingMode(RoundingMode.HALF_UP);
        //位數不足補零
        DecimalFormat_Common.applyPattern("0");
        DecimalFormat_Energy.applyPattern("0.0");
        DecimalFormat_Amp.applyPattern("0.0");

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

        //  當Energy超出setting範圍，改變字體顏色跳出Dialog視窗及震動
        NotifiyId = 2;
        if (isPage30A && Double.parseDouble(DecimalFormat_Energy.format(mValue_Energy)) >
                Integer.parseInt(Limit_Value_30A.Energy_Max)) {
            if (!mFlag_AlarmEnergy_30A) {
                mFlag_AlarmEnergy_30A = true;
                AlarmDialog(NotifiyId, "Over Energy scope!");
                vb.vibrate(2000); //震動
                SystemNotification(NotifiyId, "Alarm", "Over Energy scope!");
            }
            LedText_Value_Energy_30A.setTextColor(Color.rgb(255, 0, 0));
        } else if (!isPage30A && Double.parseDouble(DecimalFormat_Energy.format(mValue_Energy)) >
                Integer.parseInt(Limit_Value_50A.Energy_Max)){
            if (!mFlag_AlarmEnergy_50A) {
                mFlag_AlarmEnergy_50A = true;
                AlarmDialog(NotifiyId, "Over Energy scope!");
                SystemNotification(NotifiyId, "Alarm", "Over Energy scope!");
            }
            LedText_Value_Energy_30A.setTextColor(Color.rgb(255, 0, 0));
        } else {
            mFlag_AlarmEnergy_30A = false;
            mFlag_AlarmEnergy_50A = false;
            LedText_Value_Energy_30A.setTextColor(Color.rgb(255, 255, 255));
        }
        LedText_Value_Energy_30A.setText(DecimalFormat_Energy.format(mValue_Energy));

        //  Watt
        LedText_Value_Watt_30A.setText(DecimalFormat_Common.format(mValue_Watt));

        //  根據當前是30A/50A設定，當Volt超出setting範圍，改變字體顏色、跳出Dialog視窗及震動
        NotifiyId = 3;
        if (isPage30A) {
            Tmp_Max = Limit_Value_30A.Volt_Max;
            Tmp_Min = Limit_Value_30A.Volt_Min;
        } else {
            Tmp_Max = Limit_Value_50A.Volt_Max;
            Tmp_Min = Limit_Value_50A.Volt_Min;
        }
        if (Double.parseDouble(DecimalFormat_Common.format(mValue_Volt_30A)) >
                Integer.parseInt(Tmp_Max) ||
                Double.parseDouble(DecimalFormat_Common.format(mValue_Volt_30A)) <
                        Integer.parseInt(Tmp_Min)) {
            if (!mFlag_AlarmVolt_30A) {
                mFlag_AlarmVolt_30A = true;
                AlarmDialog(NotifiyId, "Over Volt scope for 30A!");
                SystemNotification(NotifiyId, "Alarm", "Over Volt scope for 30A!");
            }
            LedText_Value_Volt_30A.setTextColor(Color.rgb(255, 0, 0));
        } else {
            mFlag_AlarmVolt_30A = false;
            LedText_Value_Volt_30A.setTextColor(Color.rgb(255, 255, 255));
        }
        LedText_Value_Volt_30A.setText(DecimalFormat_Common.format(mValue_Volt_30A));

//        if (isPage30A && (Double.parseDouble(DecimalFormat_Common.format(mValue_Volt_30A)) >
//                Integer.parseInt(Limit_Value_30A.Volt_Max) ||
//                Double.parseDouble(DecimalFormat_Common.format(mValue_Volt_30A)) <
//                Integer.parseInt(Limit_Value_30A.Volt_Min))) {
//            if (!mFlag_AlarmVolt_30A) {
//                mFlag_AlarmVolt_30A = true;
//                AlarmDialog(NotifiyId, "Over Volt scope for 30A!");
//                vb.vibrate(2000); //震動
//                SystemNotification(NotifiyId, "Alarm", "Over Volt scope for 30A!");
//            }
//            LedText_Value_Volt_30A.setTextColor(Color.rgb(255, 0, 0));
//        } else if (!isPage30A && (Double.parseDouble(DecimalFormat_Common.format(mValue_Volt_30A)) >
//                Integer.parseInt(Limit_Value_50A.Volt_Max) ||
//                Double.parseDouble(DecimalFormat_Common.format(mValue_Volt_30A)) <
//                Integer.parseInt(Limit_Value_50A.Volt_Min))) {
//            if (!mFlag_AlarmVolt_30A) {
//                mFlag_AlarmVolt_30A = true;
//                AlarmDialog(NotifiyId, "Over Volt scope for 30A!");
//                vb.vibrate(2000); //震動
//                SystemNotification(NotifiyId, "Alarm", "Over Volt scope for 30A!");
//            }
//            LedText_Value_Volt_30A.setTextColor(Color.rgb(255, 0, 0));
//        } else {
//            mFlag_AlarmVolt_30A = false;
//            LedText_Value_Volt_30A.setTextColor(Color.rgb(255, 255, 255));
//        }
//        LedText_Value_Volt_30A.setText(DecimalFormat_Common.format(mValue_Volt_30A));

        //   根據當前是30A/50A設定，當Amp超出setting範圍，改變字體顏色、跳出Dialog視窗及震動
        NotifiyId = 4;
        if (isPage30A) {
            Tmp_Max = Limit_Value_30A.Amp_Max;
        } else {
            Tmp_Max = Limit_Value_50A.Amp_Max;
        }
        if (Double.parseDouble(DecimalFormat_Amp.format(mValue_Amp_30A)) >
                Integer.parseInt(Tmp_Max)) {
            if (!mFlag_AlarmAmp_30A) {
                mFlag_AlarmAmp_30A = true;
                AlarmDialog(NotifiyId, "Over Amp scope for 30A!");
                SystemNotification(NotifiyId, "Alarm", "Over Amp scope for 30A!");
            }
            LedText_Value_Amp_30A.setTextColor(Color.rgb(255, 0, 0));
        } else {
            mFlag_AlarmAmp_30A = false;
            LedText_Value_Amp_30A.setTextColor(Color.rgb(255, 255, 255));
        }
        LedText_Value_Amp_30A.setText(DecimalFormat_Amp.format(mValue_Amp_30A));


        if (!isPage30A) {
            //  當50A的Volt超出setting範圍，改變字體顏色、跳出Dialog視窗及震動
            NotifiyId = 5;
            if (Double.parseDouble(DecimalFormat_Common.format(mValue_Volt_50A)) >
                    Integer.parseInt(Limit_Value_50A.Volt_Max) ||
                    Double.parseDouble(DecimalFormat_Common.format(mValue_Volt_50A)) <
                    Integer.parseInt(Limit_Value_50A.Volt_Min)) {
                if (!mFlag_AlarmVolt_50A) {
                    mFlag_AlarmVolt_50A = true;
                    AlarmDialog(NotifiyId, "Over Volt scope for 50A!");
                    SystemNotification(NotifiyId, "Alarm", "Over Volt scope for 50A!");
                }
                LedText_Value_Volt_50A.setTextColor(Color.rgb(255, 0, 0));
            } else {
                mFlag_AlarmVolt_50A = false;
                LedText_Value_Volt_50A.setTextColor(Color.rgb(255, 255, 255));
            }
            LedText_Value_Volt_50A.setText(DecimalFormat_Common.format(mValue_Volt_50A));

            //  當50A的Amp超出setting範圍，改變字體顏色、跳出Dialog視窗及震動
            NotifiyId = 6;
            if (Double.parseDouble(DecimalFormat_Amp.format(mValue_Amp_50A)) > Integer.parseInt(Limit_Value_50A.Amp_Max)) {
                if (!mFlag_AlarmAmp_50A) {
                    mFlag_AlarmAmp_50A = true;
                    AlarmDialog(NotifiyId, "Over Amp scope for 50A!");
                    SystemNotification(NotifiyId, "Alarm", "Over Amp scope for 50A!");
                }
                LedText_Value_Amp_50A.setTextColor(Color.rgb(255, 0, 0));
            } else {
                mFlag_AlarmAmp_50A = false;
                LedText_Value_Amp_50A.setTextColor(Color.rgb(255, 255, 255));
            }
            LedText_Value_Amp_50A.setText(DecimalFormat_Amp.format(mValue_Amp_50A));
        }
    }

    /**
     * 顯示系統通知，音效
     * @param notifyID  進行通知的ID，同ID可以覆蓋
     * @param Title     要顯示的標題
     * @param Content   要顯示的內容
     */
    private void SystemNotification (int notifyID, String Title, String Content) {
        // 點擊通知後是否要自動移除掉通知
        final boolean autoCancel = true;

        // PendingIntent的Request Code
        final int requestCode = notifyID;

        // 目前Activity的Intent
        final Intent intent = getIntent();

        // ONE_SHOT：PendingIntent只使用一次；CANCEL_CURRENT：PendingIntent執行前會先結束掉之前的；NO_CREATE：沿用先前的PendingIntent，不建立新的PendingIntent；UPDATE_CURRENT：更新先前PendingIntent所帶的額外資料，並繼續沿用
        final int flags = PendingIntent.FLAG_CANCEL_CURRENT;

        // 取得PendingIntent
        final PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), requestCode, intent, flags);

        // 取得系統的通知服務
        mNotifiyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // 建立通知
        final Notification notification = new Notification.Builder(getApplicationContext())
                .setSmallIcon(R.drawable.small_value)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.large_value))
                .setContentTitle(Title)
                .setContentText(Content)
                .setContentIntent(pendingIntent)
                .setAutoCancel(autoCancel)
                .setPriority(Notification.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_SOUND)
                .build();

        // 發送通知
        mNotifiyManager.notify(notifyID, notification);

//        Window window = this.getWindow();
//        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
////        window.addFlags(WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
//        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        //點亮螢幕
        PowerManager pm=(PowerManager) getSystemService(Context.POWER_SERVICE);
        //獲取PowerManager.WakeLock對象,後面的參數|表示同時傳入兩個值,最後的是LogCat裡用的Tag
        wl = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_DIM_WAKE_LOCK , "bright");
        wl.acquire();
        mHandler.postDelayed(new Runnable(){
            @Override
            public void run() {
                wl.release();
            }}, 2000);
    }

    /**
     * 若未Connect，且有連接過的Device，則自動連線
     * 從mConnected_list最優先的開始判斷是否有Scan到，
     * 如果有則連線，連線後將裝置優先度提高，如果沒有則往mConnected_list優先度低一階的找，直到找完
     */
    private void AutoConnect () {
        if (!mConnected && !mConnected_list.isEmpty()) {
            int ConnectedNum = mConnected_list.size();
            while (ConnectedNum > 0) {
                int device_index = mDeviceAdress_list.indexOf(mConnected_list.get(ConnectedNum - 1));
                if (device_index != -1) {
                    final BluetoothDevice device = mListBluetoothDevice.get(device_index);
                    mDeviceAddress = device.getAddress();
                    mConnectDeviceName = device.getName();

                    //紀錄連接過的Device並進行排列，越後面時間越新
                    WriteConnectedDevice();

                    //進行Device連接
                    ConnectDevice(mDeviceAddress);
                    break;
                }
                ConnectedNum--;
            }
        }
    }

    /**
     * 進行Device連接
     * @param DeviceAddress 要連接的Device Address
     **/
    private void ConnectDevice (String DeviceAddress) {
        if (mBluetoothLeService == null) {
            //  透過 Intent 方式帶出另一個畫面 DeviceControlActivity
            //  同時把藍芽位址資料也傳過去，創建畫面的同時也建立 BluetoothLeService 類別 (衍生自 service 類別)
            //  這裡利用一個技巧，讓服務在背景執行，然後將畫面與服務 bind 在一起
            Intent gattServiceIntent = new Intent(MainActivity.this, BluetoothLeService.class);
            bindService(gattServiceIntent, ServiceConnection, BIND_AUTO_CREATE);

            //  藍芽服務有任何訊息要通知 UI 畫面，向系統註冊 callback 函式用來處理服務的訊息
            registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

        } else {
            if (mBluetoothLeService.mBluetoothGatt != null && !DeviceAddress.equals(mBluetoothLeService.mBluetoothDeviceAddress)) {
                //  如果連接的裝置跟上次不同，則將Gatt釋放掉
                mBluetoothLeService.mBluetoothGatt.disconnect();
                mBluetoothLeService.mBluetoothGatt.close();
                final boolean result = mBluetoothLeService.connect(DeviceAddress);
                Log.d(BluetoothLeService.TAG, "Connect request result=" + result);
            } else if (mBluetoothLeService.mBluetoothGatt != null && DeviceAddress.equals(mBluetoothLeService.mBluetoothDeviceAddress)) {
                //  如果連接的裝置跟上次相同，則重連
                final boolean result = mBluetoothLeService.connect(DeviceAddress);
                Log.d(BluetoothLeService.TAG, "Connect request result=" + result);
            }
        }
    }

    /**
     * 生成QRcode
     **/
//    public void QrCodeCreate(View v)
//    {
//        // QR code 的內容
//        String QRCodeContent = "QR code test";
//        // QR code 寬度
//        int QRCodeWidth = 200;
//        // QR code 高度
//        int QRCodeHeight = 200;
//        // QR code 內容編碼
//        Map<EncodeHintType, Object> hints = new EnumMap<EncodeHintType, Object>(EncodeHintType.class);
//        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
//
//        MultiFormatWriter writer = new MultiFormatWriter();
//        try
//        {
//            // 容錯率姑且可以將它想像成解析度，分為 4 級：L(7%)，M(15%)，Q(25%)，H(30%)
//            // 設定 QR code 容錯率為 H
//            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
//
//            // 建立 QR code 的資料矩陣
//            BitMatrix result = writer.encode(QRCodeContent, BarcodeFormat.QR_CODE, QRCodeWidth, QRCodeHeight, hints);
//            // ZXing 還可以生成其他形式條碼，如：BarcodeFormat.CODE_39、BarcodeFormat.CODE_93、BarcodeFormat.CODE_128、BarcodeFormat.EAN_8、BarcodeFormat.EAN_13...
//
//            //建立點陣圖
//            Bitmap bitmap = Bitmap.createBitmap(QRCodeWidth, QRCodeHeight, Bitmap.Config.ARGB_8888);
//            // 將 QR code 資料矩陣繪製到點陣圖上
//            for (int y = 0; y<QRCodeHeight; y++)
//            {
//                for (int x = 0;x<QRCodeWidth; x++)
//                {
//                    bitmap.setPixel(x, y, result.get(x, y) ? Color.BLACK : Color.WHITE);
//                }
//            }
//
//            ImageView imgView = (ImageView) findViewById(R.id.imageView_QRcode);
//            // 設定為 QR code 影像
//            imgView.setImageBitmap(bitmap);
//        }
//        catch (WriterException e)
//        {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//
//    }

    /**
     * 掃描QRcode
     */
//    public void onActivityResult(int requestCode, int resultCode, Intent intent){
//        IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
//        if(scanningResult!=null){
//            String scanContent=scanningResult.getContents();
//            String scanFormat=scanningResult.getFormatName();
//            scan_content.setText(scanContent);
//            scan_format.setText(scanFormat);
//        }else{
//            Toast.makeText(getApplicationContext(),"nothing",Toast.LENGTH_SHORT).show();
//        }
//    }
    /**↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑Function↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑**/
}