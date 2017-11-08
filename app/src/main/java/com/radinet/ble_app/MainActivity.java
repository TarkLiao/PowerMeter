package com.radinet.ble_app;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
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
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.yanzhenjie.zbar.camera.CameraPreview;
import com.yanzhenjie.zbar.camera.QrCodeCallback;

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

public class MainActivity extends AppCompatActivity {

    /**
     * 宣告常數
     **/
    private final int MAX_ENERGE = 999;
    private final int MAX_VOLT = 240;
    private final int MAX_AMP = 50;
    private final int MIN_VALUE = 0;

    private int mClearValue = 0;

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
    private ImageButton Button_Setting_Add;
    private ImageButton Button_Setting_Set;
    private ImageButton Button_Setting_Delete;
    private ImageButton Button_Delete;
    private ImageButton Button_Reset;
    private ImageButton Button_Apply;
    private ImageButton Button_App_Update;

    /**
     * 判定Reset後Device沒反應時的處理
     */
    private Handler mResetHandler;

    /**
     * 宣告Button OnClick後的行為
     **/
    private ButtonPressListener ButtonPressListen;

    /**
     * 宣告ImageView，作為Scan的轉圈圈動畫
     **/
    private ImageView Imageview_scan;
    private AnimationDrawable AnimationDrawable;

    /**
     * 宣告整個layout的主Page
     **/
    private RelativeLayout Layout_Main;

    /**
     * 宣告App本身介面
     **/
    private LinearLayout layout_App;

    /**
     * 宣告標籤頁顯示的內容
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
    private View View_SettingDelete;
    private View View_SettingSet;
    private View View_SettingMenu;
    private View View_Qrcode;

    /**
     * BLE一次傳20byte，共40byte，所以前20byte會先取得Value，後20byte判斷channel0或1，所以先將Value做儲存
     **/
    private byte[] mData;
    private boolean mChannel0 = false;
    private boolean mChannel1 = false;

    /**
     * 宣告顯示在正連線的裝置
     **/
    private TextView Textview_connected;

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
    private RelativeLayout mScanCropView;
    private ImageView mScanLine;
    private ValueAnimator mScanAnimator;
    private CameraPreview mPreviewView;

    /**
     *   宣告QRcode顯示物件
     *   Mac顯示裝置識別,Device Name儲存用戶輸入的名稱,Type裝置屬於30A/50A
     * */
    private TextView TextView_Mac;
    private EditText EditText_Device_Name;
    private TextView TextView_Setting_Type;

    /**
     * Device Type S:單相 D:雙相
     */
    String mDeviceType = "S";

    /**
     * Mac:儲存裝置識別
     * Name:儲存裝置顯示名稱
     * Type:儲存裝置為30/50A
     * Delete:Delete列表的checkbox是否點擊
     */
    List<String> mList_ScanDeviceMac;
    List<String> mList_ScanDeviceName;
    List<String> mList_ScanDeviceType;
    List<Boolean> mlist_DeleteCheckbox;

    /**
     * Setting Set列表(List)
     * Setting Delete列表(checkboxList)
     */
    ListView ListView_Set;
    ListView ListView_SettingDelete;
    ListSetAdapter mAdapterSettingSetResult;
    ListDeleteAdapter mAdapterSettingDeleteResult;

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
    private boolean mReseting = false;


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

    ListView ListView_BLE;
//    ArrayAdapter<String> mAdapterLeScanResult;
    ListSetAdapter mAdapterLeScanResult;


    private Handler mHandler;
    private BluetoothLeService mBluetoothLeService;
    private String mConnectDeviceName;
    private String mCurrentConnect;
    private String mDeviceAddress;
    private String mDeviceMac;
    private BluetoothDevice mBluetoothDevice;

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
    double Value_Watt_30A = 0;
    double Value_Watt_50A = 0;
    double Value_Energy_30A = 0;
    double Value_Energy_50A = 0;


    private static Boolean isPage30A;


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

        /**將宣告與物件做連結，監聽Button_Reset的作用**/
        LinkObjectValue();

        /**QrCode掃描的物件宣告與連結，設定中間框的大小**/
        QrcodeInit();

        /**確認setting檔案是否存在，存在的話直接讀取值，不存在的話create檔案並給予Default**/
        CheckSettingFileAndCheckData();

        /**確認允許scan的Device，檔案存在則讀取，不存在則無**/
        CheckScanDevice();

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



        /**用Adapter設定ListView_BLE需要顯示的內容及模式，根據layout_listitem.xml**/
        //  android.R.layout.simple_list_item_activated_1 只顯示一行文字
        //  mDevice_list    文字顯示的內容
//        mAdapterLeScanResult = new ArrayAdapter<>(this, R.layout.layout_listitem, mDevice_list);
        mAdapterLeScanResult = new ListSetAdapter(this, mDevice_list);
        //將Adapter設定至ListView
        ListView_BLE.setAdapter(mAdapterLeScanResult);
        //監聽ListView_BLE的項目
        ListView_BLE.setOnItemClickListener(scanResultOnItemClickListener);

        /**用Adapter設定ListView_Set需要顯示的內容及模式，根據layout_listitem.xml**/
        mAdapterSettingSetResult = new ListSetAdapter(this, mList_ScanDeviceName);
        ListView_Set.setOnItemClickListener(SettingSetResultOnItemClickListener);
        ListView_Set.setAdapter(mAdapterSettingSetResult);

        /**監聽ListView的項目**/
        mlist_DeleteCheckbox = new ArrayList<>();
        ListView_SettingDelete.setOnItemClickListener(SettingDeleteResultOnItemClickListener);
        /**用ListAdapter設定ListView_SettingDelete需要顯示的內容及模式**/
        mAdapterSettingDeleteResult = new ListDeleteAdapter(this, mList_ScanDeviceName);
        ListView_SettingDelete.setAdapter(mAdapterSettingDeleteResult);

        /**監聽Button_Scan，點擊後開始scan device**/
        Button_Scan.setOnClickListener(new View.OnClickListener() {
            int i;
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

        /**監聽Setting Page "Add" "Set" "Delete" Button的作用**/
        ScanDeviceMenu();

    }

    /**↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓物件行為↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓**/
    /**
     * 建立Button按下後的行為
     **/
    class ButtonPressListener implements View.OnClickListener {
        //  按Scan Value Setting Button後，切換Page
        public void onClick(View v) {
            switch (v.getId()) {
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
                    Layout_Display.addView(View_SettingMenu);
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
     * 切換Setting頁面的LimitValue成30A的設定
     **/
    public void GetSetting30A() {
        LedText_Limit_Energy_Max.setText(Limit_Value_30A.Energy_Max);
        LedText_Limit_Volt_Max.setText(Limit_Value_30A.Volt_Max);
        LedText_Limit_Volt_Min.setText(Limit_Value_30A.Volt_Min);
        LedText_Limit_Amp_Max.setText(Limit_Value_30A.Amp_Max);
    }
    /**
     * 切換Setting頁面的LimitValue成50A的設定
     **/
    public void GetSetting50A() {
        LedText_Limit_Energy_Max.setText(Limit_Value_50A.Energy_Max);
        LedText_Limit_Volt_Max.setText(Limit_Value_50A.Volt_Max);
        LedText_Limit_Volt_Min.setText(Limit_Value_50A.Volt_Min);
        LedText_Limit_Amp_Max.setText(Limit_Value_50A.Amp_Max);
    }

    /**
     * 建立ScanList的項目按下後的行為
     **/
    AdapterView.OnItemClickListener scanResultOnItemClickListener = new AdapterView.OnItemClickListener() {
        int device_index;
        TextView content;
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            //  取得ListView點擊到的項目名稱，並藉由項目名稱去取得List的索引
            //  mDevice_list及mListBluetoothDevice的項目索引值是相同的，所以透過索引值可以直接去取得BluetoothDevice
            content = (TextView) view.findViewById(R.id.check_item_text);
            final String device_name = content.getText().toString();
            device_index = mDevice_list.indexOf(device_name);
            final BluetoothDevice device = mListBluetoothDevice.get(device_index);

            mDeviceAddress = device.getAddress();
            String msg = YuWaMac(device.getName()) + "\n"
                    + mDeviceAddress + "\n"
                    + device.getBluetoothClass().toString() + "\n";

            new AlertDialog.Builder(MainActivity.this)
                    .setTitle(mDevice_list.get(device_index))
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

                            //  如果連線狀態且連接與選擇的是相同Device，則直接跳到value page，且顯示裝置已連線
                            if (mConnected && device.equals(mBluetoothDevice)) {
                                Layout_Display.removeAllViews();
                                if (isPage30A) {
                                    Layout_Display.addView(View_Value_30A);
                                } else {
                                    Layout_Display.addView(View_Value_50A);
                                }
                                if (mToast == null) {
                                    mToast = Toast.makeText(MainActivity.this, mConnectDeviceName + " connected", Toast.LENGTH_SHORT);
                                } else {
                                    mToast.setText(mConnectDeviceName + " connected");
                                }
                                mToast.show();
                                return;
                            }

                            //  判斷連接的是30/50A的device
                            int index = mList_ScanDeviceMac.indexOf(device.getName());
                            if (mList_ScanDeviceType.get(index).equals("S")) {
                                isPage30A = true;
                            } else if (mList_ScanDeviceType.get(index).equals("D")) {
                                isPage30A = false;
                            }
                            LinkObjectValue();

                            //  紀錄連接的DeviceName
                            mConnectDeviceName = mList_ScanDeviceName.get(index);
                            mDeviceMac = mList_ScanDeviceMac.get(index);
                            mBluetoothDevice = device;

                            //  紀錄連接過的Device並進行排列，越後面時間越新
                            WriteConnectedDevice ();

                            //  進行Device連接
                            ConnectDevice (mDeviceAddress);
                        }
                    })
                    .show();
        }
    };

    /**
     * 建立SettingSet的項目按下後的行為
     **/
    AdapterView.OnItemClickListener SettingSetResultOnItemClickListener = new AdapterView.OnItemClickListener() {
        int index;
        TextView content;
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            content = (TextView) view.findViewById(R.id.check_item_text);
            final String device_name = content.getText().toString();
            index = mList_ScanDeviceName.indexOf(device_name);

            //將該項目的裝置識別及修改名稱填到setting頁面
            TextView_Mac.setText(YuWaMac(mList_ScanDeviceMac.get(index).toString()));
            EditText_Device_Name.setText(mList_ScanDeviceName.get(index).toString());

            if (mList_ScanDeviceType.get(index).equals("S")) {
                GetSetting30A();
                TextView_Setting_Type.setText("S");
            } else if (mList_ScanDeviceType.get(index).equals("D")) {
                GetSetting50A();
                TextView_Setting_Type.setText("D");
            }
            LinkObjectValue();

            //因為是settingSet，不一定要修改名稱，所以不focus EditText_Device_Name
            EditText_Device_Name.clearFocus();
            Layout_Display.removeAllViews();
            Layout_Display.addView(View_Setting);
        }
    };

    /**
     * 建立SettingDelete的項目按下後的行為
     **/
    AdapterView.OnItemClickListener SettingDeleteResultOnItemClickListener = new AdapterView.OnItemClickListener() {
        CheckedTextView chkItem;
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            //項目的checkbox點擊效果，儲存各項目的checkbox狀態
            chkItem = (CheckedTextView) view.findViewById(R.id.check_delete_device);
            chkItem.setChecked(!chkItem.isChecked());
            mlist_DeleteCheckbox.set(position, chkItem.isChecked());
        }
    };
    /**
     * 非Qrcode掃瞄時按返回鍵時，等同按HOME
     * Qrcode掃描時，按返回鍵，返回SettingMenu
     **/
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mScanAnimator != null) {
                if (mScanAnimator.isRunning()) {
                    Layout_Main.removeAllViews();
                    Layout_Main.addView(layout_App);
                    QrcodeStopScan();
                    return super.onKeyDown(0, null);
                }
            } else {
                // Show home screen when pressing "back" button,
                //  so that this app won't be closed accidentally
                Intent intentHome = new Intent(Intent.ACTION_MAIN);
                intentHome.addCategory(Intent.CATEGORY_HOME);
                intentHome.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intentHome);

                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
    /**
     * 監聽Setting Page "Add" "Set" "Delete" Button的作用
     * 監聽Apply及Delete Button的作用
     **/
    public void ScanDeviceMenu() {
        //將Delete的checkboxlist全設為false
        for (int i = 0; i < mList_ScanDeviceMac.size(); i++) {
            mlist_DeleteCheckbox.add(false);
        }

        Button_Setting_Add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //當擁有相機權限，進行Qrcode掃描，無權限則要求使用者允許
                if (Build.VERSION.SDK_INT >= 23) {
                    int location_permission = checkSelfPermission(
                            Manifest.permission.CAMERA);
                    if (location_permission != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{
                                Manifest.permission.CAMERA}, REQUEST_CAMREA_PERMISSION);
                    } else {
                        startScanQrCode();
                    }
                } else {
                    startScanQrCode();
                }
            }
        });

        Button_Apply.setOnClickListener(new View.OnClickListener() {
            int index;
            @Override
            public void onClick(View v) {
                EditText_Device_Name.clearFocus();
                if (!EditText_Device_Name.getText().toString().isEmpty()) {
                    if (!mList_ScanDeviceMac.contains(NightbunMac(TextView_Mac.getText().toString()))) {
                        Log.d("test","Mac not exist");
                        mlist_DeleteCheckbox.add(false);
                        mList_ScanDeviceMac.add(NightbunMac(TextView_Mac.getText().toString()));
                        mList_ScanDeviceName.add(EditText_Device_Name.getText().toString());
                        mList_ScanDeviceType.add(mDeviceType);
                    } else {
                        Log.d("test","Mac exist");
                        index = mList_ScanDeviceMac.indexOf(NightbunMac(TextView_Mac.getText().toString()));
                        mList_ScanDeviceName.set(index, EditText_Device_Name.getText().toString());
                        mlist_DeleteCheckbox.set(index, false);
                    }

                    //儲存被允許Scan的Device相關訊息
                    WriteScanDevice();

                    //如果該設備被connect，則修改connectDevice名稱
                    if (NightbunMac(TextView_Mac.getText().toString()).equals(mDeviceMac)) {
                        mConnectDeviceName = mList_ScanDeviceName.get(mList_ScanDeviceMac.indexOf(NightbunMac(TextView_Mac.getText().toString())));
                        mCurrentConnect = mConnectDeviceName;
                    }
                    Layout_Display.removeAllViews();
                    Layout_Display.addView(View_SettingSet);
                    ListView_Set.invalidateViews();
                } else if (EditText_Device_Name.getText().toString().isEmpty()) {
                    EditText_Device_Name.requestFocus();
                }
                SaveSetting();
            }
        });

        Button_Setting_Set.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Layout_Display.removeAllViews();
                Layout_Display.addView(View_SettingSet);
                ListView_Set.invalidateViews();
            }
        });

        Button_Setting_Delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Layout_Display.removeAllViews();
                Layout_Display.addView(View_SettingDelete);
                ListView_SettingDelete.invalidateViews();
            }
        });

        Button_Delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                for (int i = 0; i < mlist_DeleteCheckbox.size(); i++) {
//                    Log.d("test", mList_ScanDeviceName.get(i) + ":" + mlist_DeleteCheckbox.get(i));
//                }
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Delete")
                        .setMessage("Delete Scan Device")
                        .setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .setNeutralButton("Delete", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                for (int i = mlist_DeleteCheckbox.size() - 1; i >= 0; i--) {
                                    if (mlist_DeleteCheckbox.get(i) == true) {
                                        mList_ScanDeviceName.remove(i);
                                        mList_ScanDeviceMac.remove(i);
                                        mList_ScanDeviceType.remove(i);
                                        Layout_Display.removeAllViews();
                                        Layout_Display.addView(View_SettingDelete);
                                        ListView_SettingDelete.invalidateViews();
                                    }
                                }
                                mlist_DeleteCheckbox.clear();
                                for (int i = 0; i < mList_ScanDeviceMac.size(); i++) {
                                    mlist_DeleteCheckbox.add(false);
                                }
                                WriteScanDevice();
                            }
                        })
                        .show();
            }
        });
        Button_App_Update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("App Update")
                        .setMessage("Go to GooglePlay")
                        .setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .setNeutralButton("Go", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            final String appPackageName = "com.radinet.ble_app"; // getPackageName() from Context or Activity object
                            try {
                                startActivity(new Intent(Intent.ACTION_VIEW,
                                        Uri.parse("https://play.google.com/store")));
//                                startActivity(new Intent(Intent.ACTION_VIEW,
//                                        Uri.parse("market://details?id=" + appPackageName)));
                            } catch (android.content.ActivityNotFoundException e) {
                                startActivity(new Intent(Intent.ACTION_VIEW,
                                        Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                            }
                            }
                        })
                        .show();

            }
        });
    }
    /**↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑物件行為↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑**/

//    @Override
//    protected void onPause() {
//        QrcodeStopScan();
//        super.onPause();
//    }
    /**
     * QrCode掃描的物件宣告與連結，設定中間框的大小
     **/
    private void QrcodeInit() {
        LinearLayout Capture_Layout = (LinearLayout)View_Qrcode.findViewById(R.id.capture_layout);
        mPreviewView = (CameraPreview) View_Qrcode.findViewById(R.id.capture_preview);
        mScanCropView = (RelativeLayout) View_Qrcode.findViewById(R.id.capture_crop_view);
        mScanLine = (ImageView) View_Qrcode.findViewById(R.id.capture_scan_line);

        mPreviewView.setScanCallback(resultCallback);

        // 取得螢幕解析度
        DisplayMetrics dm = new DisplayMetrics();
        this.getWindowManager().getDefaultDisplay().getMetrics(dm);
        int vWidth = dm.widthPixels;

        //  設定layout的長寬
        Capture_Layout.setLayoutParams(new LinearLayout.LayoutParams(vWidth, vWidth/2));
        //  設定中間框長寬
        mScanCropView.setLayoutParams(new LinearLayout.LayoutParams(vWidth/2, vWidth/2));
    }

    /**
     * QrCode掃描成功後的callback
     **/
    private QrCodeCallback resultCallback = new QrCodeCallback() {
        @Override
        public void onScanResult(String result) {
            Layout_Main.removeAllViews();
            Layout_Main.addView(layout_App);
            QrcodeStopScan();
            GetQrCodeAction(result);
        }
    };

    /**
     * QrCode停止掃描
     **/
    private void QrcodeStopScan() {
        mScanAnimator.cancel();
        mPreviewView.stop();
    }

    /**
     * QrCode開始掃描
     **/
    private void startScanQrCode() {
        if (mPreviewView.start()) {
            Layout_Main.removeAllViews();
            Layout_Main.addView(View_Qrcode);
            if (mScanAnimator == null) {
                int height = mScanCropView.getMeasuredHeight() - 25;
                mScanAnimator = ObjectAnimator.ofFloat(mScanLine, "translationY", 0F, height).setDuration(3000);
                mScanAnimator.setInterpolator(new LinearInterpolator());
                mScanAnimator.setRepeatCount(ValueAnimator.INFINITE);
                mScanAnimator.setRepeatMode(ValueAnimator.REVERSE);
            }
            mScanAnimator.start();
        } else {
            new android.support.v7.app.AlertDialog.Builder(this)
                    .setTitle("camera_failure")
                    .setMessage("camera_hint")
                    .setCancelable(false)
                    .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .show();
        }
    }

    /**
     * 取得QrCode內容，進行分析
     **/
    public void GetQrCodeAction(final String QrCodeResult) {
        //Tark
        byte[] s = NightbunMac(QrCodeResult).getBytes();

        if (s[2] == 'S') {
            mDeviceType = "S";
            GetSetting30A();
            TextView_Setting_Type.setText("S");
        } else if (s[2] == 'D') {
            mDeviceType = "D";
            GetSetting50A();
            TextView_Setting_Type.setText("D");
        } else {
            if (mToast == null) {
                mToast = Toast.makeText(MainActivity.this, "Wrong format", Toast.LENGTH_LONG);
            } else {
                mToast.setText("Wrong format");
            }
            mToast.show();
            return;
        }
        TextView_Mac.setText(QrCodeResult);
        Layout_Display.removeAllViews();
        Layout_Display.addView(View_Setting);
        EditText_Device_Name.setText("");
        EditText_Device_Name.requestFocus();
    }

    /**
     * 將玖邦的裝置識別轉成永望的
     * @param Mac 要轉的Mac- PMD      000717E206
     * @return 轉完的Mac-    APMD00717E206
     */
    public String YuWaMac (String Mac) {
        //PMD      000717E206
        //     V
        //APMD00717E206
        if (Mac == null) return "error";
        byte[] s = Mac.getBytes();
        byte[] change = new byte[13];
        if (s.length == 19) {//change[0]
            switch(s[9]) {
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    change[0] = (byte)(s[9] + 0x11);
                    break;
                case 'A':
                case 'B':
                case 'C':
                case 'D':
                case 'E':
                case 'F':
                    change[0] = (byte)(s[9] + 0xA);
                    break;
                default:
                    change[0] = 'Z';
                    break;
            }
            for (int i = 0; i < 3; i++) {//change[1-3]
                change[1 + i] = s[i];
            }
            for (int i = 0; i < 9; i++) {//change[4-12]
                change[4 + i] = s[i + 10];
            }
            return new String(change);
        } else {
            return "error";
        }
    }

    /**
     * 將永望的裝置識別轉成玖邦的
     * @param Mac 要轉的Mac- APMD00717E206
     * @return 轉完的Mac-    PMD      000717E206
     */
    public String NightbunMac (String Mac) {
        if (Mac == null) return "error";
        //APMD00717E206
        //     V
        //PMD      000717E206
        byte[] s = Mac.getBytes();
        byte[] change = new byte[19];
        if (s.length == 13) {//change[0]
            for (int i = 0; i < 3; i++) {//change[0-2]
                change[i] = s[i + 1];
            }
            for (int i = 0; i < 6; i++) {//change[3-8]
                change[i + 3] = ' ';
            }
            switch (s[0]) {//change[9]
                case 'A':
                case 'B':
                case 'C':
                case 'D':
                case 'E':
                case 'F':
                case 'G':
                case 'H':
                case 'I':
                case 'J':
                    change[9] = (byte)(s[0] - (byte)0x11);
                    break;
                case 'K':
                case 'L':
                case 'M':
                case 'N':
                case 'O':
                case 'P':
                    change[9] = (byte)(s[0] - (byte)0x0A);
                    break;
            }
            for (int i = 0; i < 9; i++) {//change[10-18]
                change[i + 10] = s[i + 4];
            }
            return new String(change);
        } else {
            return "error";
        }
    }
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
        Handler StartPictureHandler = new Handler();
        StartPictureHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //  過四秒後將圖片刪除
                Background_Imageview.setVisibility(View.GONE);
                Layout_Logo.setVisibility(View.GONE);
                mAppLogoEnd = true;
                //圖片被刪除後，執行自動Scan
                AutoScanRun();
            }
        }, 4000);
    }

    /**
     * 將宣告與物件做連結
     **/
    private void LinkObjectCommon() {

        //  Main與物件進行連結
        Layout_Main = (RelativeLayout) findViewById(R.id.layout_main);
        layout_App = (LinearLayout) findViewById(R.id.layout_app);

        //  Display與物件進行連結
        Layout_Display = (RelativeLayout) findViewById(R.id.display_RelativeLayout);

        //  View與物件做連結
        layoutInflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
        View_Value_30A = layoutInflater.inflate(R.layout.layout_value_30a, null);
        View_Value_50A = layoutInflater.inflate(R.layout.layout_value_50a, null);
        View_Scan = layoutInflater.inflate(R.layout.layout_scan, null);
        View_Setting = layoutInflater.inflate(R.layout.layout_setting, null);
        View_SettingDelete = layoutInflater.inflate(R.layout.layout_settingdelete, null);
        View_SettingMenu = layoutInflater.inflate(R.layout.layout_settingmenu, null);
        View_SettingSet = layoutInflater.inflate(R.layout.layout_settingset, null);
        View_Qrcode = layoutInflater.inflate(R.layout.layout_qr_scan , null);

        // Setting介面物件連結
        EditText_Device_Name = (EditText) View_Setting.findViewById(R.id.EditText_Device_Name);
        TextView_Mac = (TextView) View_Setting.findViewById(R.id.TextView_Mac);
        TextView_Setting_Type = (TextView) View_Setting.findViewById(R.id.TextView_Setting_Type);

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
        Button_Setting_Add = (ImageButton) View_SettingMenu.findViewById(R.id.Button_Scan_Add);
        Button_Setting_Set= (ImageButton) View_SettingMenu.findViewById(R.id.Button_Scan_Set);
        Button_Setting_Delete = (ImageButton) View_SettingMenu.findViewById(R.id.Button_Scan_Delete);
        Button_Delete = (ImageButton) View_SettingDelete.findViewById(R.id.Button_Delete);
        Button_App_Update = (ImageButton) View_SettingMenu.findViewById(R.id.Button_App_Update);
        Button_Apply = (ImageButton) View_Setting.findViewById(R.id.Button_apply);


        //  LedTextView與物件做連結
        LedText_Limit_Energy_Max = (LedTextView) View_Setting.findViewById(R.id.Textview_Energy_Max);
        LedText_Limit_Volt_Max = (LedTextView) View_Setting.findViewById(R.id.Textview_Volt_Max);
        LedText_Limit_Volt_Min = (LedTextView) View_Setting.findViewById(R.id.Textview_Volt_Min);
        LedText_Limit_Amp_Max = (LedTextView) View_Setting.findViewById(R.id.Textview_Amp_Max);

        //  ListView與物件做連結
        ListView_BLE = (ListView) View_Scan.findViewById(R.id.ListView_BLE);
        ListView_SettingDelete = (ListView) View_SettingDelete.findViewById(R.id.ListView_SettingDelete);
        ListView_Set = (ListView) View_SettingSet.findViewById(R.id.ListView_SettingSet);

        //  ImageView與物件做連結
        Imageview_scan = (ImageView)View_Scan.findViewById(R.id.Imageview_animation);
    }
    /**
     * 將宣告與物件做連結，監聽Button_Reset的作用
     **/
    private void LinkObjectValue() {
        if (isPage30A) {
            Button_Reset = (ImageButton) View_Value_30A.findViewById(R.id.Button_Reset_30A);
            Textview_connected = (TextView) View_Value_30A.findViewById(R.id.textview_connected_30A);
            LedText_Value_Energy_30A = (LedTextView) View_Value_30A.findViewById(R.id.Textview_Value_Energy_30a);
            LedText_Value_Watt_30A = (LedTextView) View_Value_30A.findViewById(R.id.Textview_Value_Watt_30a);
            LedText_Value_Volt_30A = (LedTextView) View_Value_30A.findViewById(R.id.Textview_Value_Volt_30a);
            LedText_Value_Amp_30A = (LedTextView) View_Value_30A.findViewById(R.id.Textview_Value_Amp_30a);
        } else {
            Button_Reset = (ImageButton) View_Value_50A.findViewById(R.id.Button_Reset_50A);
            Textview_connected = (TextView) View_Value_50A.findViewById(R.id.textview_connected_50A);
            LedText_Value_Energy_30A = (LedTextView) View_Value_50A.findViewById(R.id.Textview_Value_Energy_30a);
            LedText_Value_Watt_30A = (LedTextView) View_Value_50A.findViewById(R.id.Textview_Value_Watt_30a);
            LedText_Value_Volt_30A = (LedTextView) View_Value_50A.findViewById(R.id.Textview_Value_Volt_50a);
            LedText_Value_Amp_30A = (LedTextView) View_Value_50A.findViewById(R.id.Textview_Value_Amp_50a);
            LedText_Value_Volt_50A = (LedTextView) View_Value_50A.findViewById(R.id.Textview_Value_Volt_30a);
            LedText_Value_Amp_50A = (LedTextView) View_Value_50A.findViewById(R.id.Textview_Value_Amp_30a);
        }
        Button_Reset.setOnClickListener(null);
        Button_Reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mConnected && mBluetoothLeService.mBluetoothGatt != null) {
                    byte[] Data = new byte[]{'R', 'E', 'S', 'E', 't'};
                    boolean status = mBluetoothLeService.mWriteCharacteristric.setValue(Data);
                    Log.d("test", "Status:"+status);
                    status = mBluetoothLeService.mBluetoothGatt.writeCharacteristic(mBluetoothLeService.mWriteCharacteristric);
                    Log.d("test", "Status:"+status);
                    mReseting = true;
                    if (mToast == null) {
                        mToast = Toast.makeText(MainActivity.this, "Start reset", Toast.LENGTH_LONG);
                    } else {
                        mToast.setText("Start reset");
                    }
                    mToast.show();

                    //進行Reset後，八秒沒有將Energy歸零，則顯示Fail
                    mResetHandler = new Handler();
                    mResetHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (mReseting) {
                                if (mToast == null) {
                                    mToast = Toast.makeText(MainActivity.this, "Reset failed", Toast.LENGTH_LONG);
                                } else {
                                    mToast.setText("Reset failed");
                                }
                                mToast.show();
                                mReseting = false;
                            }
                        }
                    }, 8000);
                }
            }
        });
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
    }

    /**
     * 確認允許scan的Device，檔案存在則讀取，不存在則無
     **/
    private void CheckScanDevice() {
        String Filename_ScanMac = "ScanMac.txt";
        File File_ScanMac = new File(getFilesDir().getAbsolutePath() + "/" + Filename_ScanMac);
        String Filename_ScanName = "ScanName.txt";
        File File_ScanName = new File(getFilesDir().getAbsolutePath() + "/" + Filename_ScanName);
        String Filename_ScanType = "ScanType.txt";
        File File_ScanType = new File(getFilesDir().getAbsolutePath() + "/" + Filename_ScanType);
        mList_ScanDeviceMac = new ArrayList<>();
        mList_ScanDeviceName = new ArrayList<>();
        mList_ScanDeviceType = new ArrayList<>();

        if (File_ScanMac.exists() && File_ScanName.exists() && File_ScanType.exists()) {
            Log.d("Test", "File_ScanMac Exists");
            try {
                FileInputStream InputMac = openFileInput(Filename_ScanMac);
                DataInputStream DataInputMac = new DataInputStream(InputMac);
                BufferedReader ReaderMac = new BufferedReader(new InputStreamReader(DataInputMac));
                String ContentMac = ReaderMac.readLine();
                Log.d("test", "Scan Mac:" + ContentMac);
                InputMac.close();

                if (ContentMac == null) {
                    File_ScanMac.delete();
                    return;
                }

                String[] tokenMac = ContentMac.split(",");
                for (String tmp : tokenMac) {
                    mList_ScanDeviceMac.add(tmp);
                }

                FileInputStream InputName = openFileInput(Filename_ScanName);
                DataInputStream DataInputName = new DataInputStream(InputName);
                BufferedReader ReaderName = new BufferedReader(new InputStreamReader(DataInputName));
                String ContentName = ReaderName.readLine();
                Log.d("test", "Scan Name:" + ContentName);
                InputName.close();

                if (ContentName == null) {
                    File_ScanName.delete();
                    return;
                }

                String[] tokenName = ContentName.split(",");
                for (String tmp : tokenName) {
                    mList_ScanDeviceName.add(tmp);
                }

                FileInputStream InputType = openFileInput(Filename_ScanType);
                DataInputStream DataInputType = new DataInputStream(InputType);
                BufferedReader ReaderType = new BufferedReader(new InputStreamReader(DataInputType));
                String ContentType = ReaderType.readLine();
                Log.d("test", "Scan Type:" + ContentType);
                InputType.close();

                if (ContentType == null) {
                    File_ScanType.delete();
                    return;
                }

                String[] tokenType = ContentType.split(",");
                for (String tmp : tokenType) {
                    mList_ScanDeviceType.add(tmp);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            //若檔案單獨存在則全刪除
            if (File_ScanMac.exists()) {
                File_ScanMac.delete();
            }
            if (File_ScanName.exists()) {
                File_ScanName.delete();
            }
            if (File_ScanType.exists()) {
                File_ScanType.delete();
            }
        }
    }

    /**
     * 確認connect過的Device，檔案存在則讀取並儲存DeviceName，不存在則無
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
                mConnected_list.remove(mDeviceAddress);
                mConnected_list.add(mDeviceAddress);
            } else {
                mConnected_list.add(mDeviceAddress);
            }

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
     * 跳出Alarm的Dialog視窗
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
        if (TextView_Setting_Type.getText().toString() == "S") {
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
        } else if (TextView_Setting_Type.getText().toString() == "D") {
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
    private void AutoScanRun() {
        Boolean Bluetooth = mBluetoothAdapter.isEnabled();
        if (mList_ScanDeviceMac.size() > 0) {
            if (Build.VERSION.SDK_INT >= 23) {
                if (Bluetooth && mGpsOpen && !mConnected && mAppLogoEnd) {
                    Layout_Display.removeAllViews();
                    Layout_Display.addView(View_Scan);
                    scanLeDevice(Bluetooth);
                }
            } else {
                if (Bluetooth && !mConnected && mAppLogoEnd) {
                    Layout_Display.removeAllViews();
                    Layout_Display.addView(View_Scan);

                    mScanning = false;

                    mHandler.postDelayed(ScanTimeRun, SCAN_PERIOD);
                    mTimerScan = new Timer(true);
                    //5.0.2須不斷Scan才能掃到裝置
                    mTimerScan.schedule(new Task_ScanContinued(), 0, 250);
                }
            }
        } else {
            Layout_Display.removeAllViews();
            Layout_Display.addView(View_SettingMenu);
        }
    }

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

                    //如果是連線狀態，直接添加當前的設備至scan list
                    if (mConnected && mList_ScanDeviceMac.contains(mBluetoothDevice.getName())) {
                        mScanDeviceNum ++;
                        mListBluetoothDevice.add(mBluetoothDevice);
                        int index = mList_ScanDeviceMac.indexOf(mBluetoothDevice.getName());
                        mDevice_list.add(mList_ScanDeviceName.get(index));
                        mDeviceAdress_list.add(mBluetoothDevice.getAddress().toString());
                    }
                    ListView_BLE.invalidateViews();
                    if (mToast == null) {
                        mToast = Toast.makeText(MainActivity.this, "Scanning", Toast.LENGTH_LONG);
                    } else {
                        mToast.setText("Scanning");
                    }
                    mToast.show();
                    //轉圈圈動畫
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
        if (requestCode == RQS_ENABLE_BLUETOOTH) {

            if(resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(this, "resultCode == Activity.RESULT_CANCELED", Toast.LENGTH_SHORT).show();
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
                    //取得權限，開始QRcode掃描
                    startScanQrCode();
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
     * 進行SDK>=6.0 scan device
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

            //如果是連線狀態，直接添加當前的設備至scan list
            if (mConnected && mList_ScanDeviceMac.contains(mBluetoothDevice.getName())) {
                mScanDeviceNum ++;
                mListBluetoothDevice.add(mBluetoothDevice);
                int index = mList_ScanDeviceMac.indexOf(mBluetoothDevice.getName());
                mDevice_list.add(mList_ScanDeviceName.get(index));
                mDeviceAdress_list.add(mBluetoothDevice.getAddress().toString());
            }
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
            //轉圈圈動畫
            Imageview_scan.setVisibility(View.VISIBLE);
            AnimationDrawable.start();
        } else {
            mScanning = false;
            //隱藏轉圈圈動畫
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
                Toast.makeText(MainActivity.this, "Bluetooth is close", Toast.LENGTH_LONG).show();
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
//            if (device.getName() != null) {
//                //  判斷Device存不存在BluetoothList
//                if (!mListBluetoothDevice.contains(device)) {
//                    mScanDeviceNum ++;
//                    //  將Device存進BluetoothList
//                    mListBluetoothDevice.add(device);
//                    //  將Device名稱存進Device_list
//                    mDevice_list.add(device.getName());
//                    mDeviceAdress_list.add(device.getAddress().toString());
//                    //  重新顯示listView
//                    ListView_BLE.invalidateViews();
//                }
//            }
            int index;


            Log.d("check", "Scan DeviceName:"+device.getName());
            if (device.getName() != null) {
                if (!mList_ScanDeviceMac.contains(device.getName()))
                    return;
                //  判斷Device存不存在BluetoothList
                if (!mListBluetoothDevice.contains(device)) {
                    mScanDeviceNum ++;
                    //  將Device存進BluetoothList
                    mListBluetoothDevice.add(device);
                    //  將Device名稱存進Device_list
                    index = mList_ScanDeviceMac.indexOf(device.getName());
                    mDevice_list.add(mList_ScanDeviceName.get(index));
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
                mBluetoothLeService.mReconnectCount = 0;
                Layout_Display.removeAllViews();
                if (isPage30A) {
                    Log.d("check", "Connect:30A");
                    Layout_Display.addView(View_Value_30A);
                } else {
                    Log.d("check", "Connect:50A");
                    Layout_Display.addView(View_Value_50A);
                }
                mCurrentConnect = mConnectDeviceName;
                Log.d("Paul", "Connected");
                if (mToast == null) {
                    mToast = Toast.makeText(MainActivity.this, mConnectDeviceName + " connected", Toast.LENGTH_SHORT);
                } else {
                    mToast.setText(mConnectDeviceName + " connected");
                }
                mToast.show();
                if (mNotifiyManager != null) {
                    mNotifiyManager.cancel(1);
                }
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {

                if (isPage30A) {
                    mClearValue = 0;
                    mValue_Volt_30A = 0;
                    mValue_Amp_30A = 0;
                    Value_Watt_30A = 0;
                    Value_Energy_30A = 0;
                    mValue_Watt = 0;
                    mValue_Energy = 0;
                } else {
                    mClearValue = 0;
                    mValue_Volt_30A = 0;
                    mValue_Amp_30A = 0;
                    Value_Watt_30A = 0;
                    Value_Energy_30A = 0;
                    mValue_Volt_50A = 0;
                    mValue_Amp_50A = 0;
                    Value_Watt_50A = 0;
                    Value_Energy_50A = 0;
                    mValue_Watt = 0;
                    mValue_Energy = 0;
                }
                mConnected = false;
                mCurrentConnect = "Empty";
                ValueMonitor();
                if (mConnected) {//斷線重連
                    ConnectDevice(mDeviceAddress);
                    Log.d("test", "connect Protocol");
                }

                if (mToast == null) {
                    mToast = Toast.makeText(MainActivity.this, mConnectDeviceName + " disconnected", Toast.LENGTH_SHORT);
                } else {
                    mToast.setText(mConnectDeviceName + " disconnected");
                }
                SystemNotification(1, "Device", mConnectDeviceName + " disconnected");
                mToast.show();
                Log.d("test", "Connect Fail");

            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                //  若傳遞藍牙資訊過來Call function Refresh
                Refresh(intent.getByteArrayExtra(mBluetoothLeService.ACTION_DATA_AVAILABLE));
            }
        }
    };

    /**
     * 將Byte[4]轉成Double
     **/
    private double Byte42double(byte[] data, int sAddr) {
        double[] Index;
        Index = new double[4];

        for (int i = 0; i < 4; i++) {
            Index[i] = data[sAddr + i] & 0x00000000000000FF;
//            Log.d("test", String.valueOf(i) +":" +String.valueOf(Index[i]));
        }
        return ((Index[0] * 16777216) + (Index[1] * 65536) + (Index[2] * 256) + Index[3]);
    }

    /**
     * 進行Refresh，判斷30A/50A裝置，判斷是否Reset成功
     **/
    private void Refresh(byte[] Data) {
        Log.d("check","Get BLE Data");
        if (mReseting && Data != null) {
            if (Data.length == 5) {
                //取的資料RESEt則代表成功送reset指令
                if (Data[0] == 'R' && Data[1] == 'E' && Data[2] == 'S' && Data[3] == 'E' && Data[4] == 'T') {
                    if (mToast == null) {
                        mToast = Toast.makeText(MainActivity.this, "Resetting", Toast.LENGTH_LONG);
                    } else {
                        mToast.setText("Resetting");
                    }
                    mToast.show();
                }
                return;
            }
            if (isPage30A) {//30A判斷值是否歸零
                if (Data.length == 20 && Data[0] == 0x01 && Data[1] == 0x03 && Data[2] == 0x20 &&
                        Data[15] == 0x00 && Data[16] == 0x00 && Data[17] == 0x00 && Data[18] == 0x00) {
                    mReseting = false;
                    if (mToast == null) {
                        mToast = Toast.makeText(MainActivity.this, "Reset success", Toast.LENGTH_LONG);
                    } else {
                        mToast.setText("Reset success");
                    }
                    mToast.show();
                }
            } else {//50A判斷值是否歸零
                if (Data.length == 20) {
                    if (Data[0] == 0x01 && Data[1] == 0x03 && Data[2] == 0x20) {
                        mData = Data;
                    } else if (Data[17] == 0x00 && Data[18] == 0x00 && Data[19] == 0x00) {
                        if (mData.length == 20 && mData[0] == 0x01 && mData[1] == 0x03 && mData[2] == 0x20 &&
                                mData[15] == 0x00 && mData[16] == 0x00 && mData[17] == 0x00 && mData[18] == 0x00) {
                            mChannel0 = true;
                        }
                    } else if (Data[17] == 0x01 && Data[18] == 0x01 && Data[19] == 0x01) {
                        if (mData.length == 20 && mData[0] == 0x01 && mData[1] == 0x03 && mData[2] == 0x20 &&
                                mData[15] == 0x00 && mData[16] == 0x00 && mData[17] == 0x00 && mData[18] == 0x00) {
                            mChannel1 = true;
                        }
                    }
                }
                //50A需要確認channel0跟1都歸零
                if (mChannel0 && mChannel1) {
                    mReseting = false;
                    mChannel0 = false;
                    mChannel1 = false;
                    if (mToast == null) {
                        mToast = Toast.makeText(MainActivity.this, "Reset success", Toast.LENGTH_LONG);
                    } else {
                        mToast.setText("Reset success");
                    }
                    mToast.show();
                }
            }
            return;
        }
        if (Data != null) {
            if (isPage30A) {//30A
                if (Data[0] == 0x01 && Data[1] == 0x03 && Data[2] == 0x20) {
                    Decode_data(Data, 0);
                }
            } else if (!isPage30A && Data.length == 20) {// 50A
                if (Data[0] == 0x01 && Data[1] == 0x03 && Data[2] == 0x20) {
                    mData = Data;
                } else if (Data[17] == 0x00 && Data[18] == 0x00 && Data[19] == 0x00) {
                    if (mData != null) {
                        Decode_data(mData, 0);
                        mData = null;
                    }
                } else if (Data[17] == 0x01 && Data[18] == 0x01 && Data[19] == 0x01) {
                    if (mData != null) {
                        Decode_data(mData, 1);
                        mData = null;
                    }
                }
            }
        }

        ValueMonitor();
    }

    /**
     * 解析從藍牙讀取的資料
     **/
    private void Decode_data(byte[] data, int channel) {

        if (channel == 0) {
            mClearValue++;
            mValue_Volt_30A = Byte42double(data, 3) / 10000;
            mValue_Amp_30A = Byte42double(data, 7) / 10000;
            Value_Watt_30A = Byte42double(data, 11) / 10000;
            Value_Energy_30A = Byte42double(data, 15) / 10000;
        } else {
            mClearValue--;
            mValue_Volt_50A = Byte42double(data, 3) / 10000;
            mValue_Amp_50A = Byte42double(data, 7) / 10000;
            Value_Watt_50A = Byte42double(data, 11) / 10000;
            Value_Energy_50A = Byte42double(data, 15) / 10000;
        }

        if (isPage30A) {
            mClearValue = 0;
            mValue_Watt = Value_Watt_30A;
            mValue_Energy = Value_Energy_30A;
        } else {
            if (mClearValue > 1) {
                mClearValue = 0;
                mValue_Volt_50A = 0;
                mValue_Amp_50A = 0;
                Value_Watt_50A = 0;
                Value_Energy_50A = 0;
            } else if (mClearValue < -1) {
                mClearValue = 0;
                mValue_Volt_30A = 0;
                mValue_Amp_30A = 0;
                Value_Watt_30A = 0;
                Value_Energy_30A = 0;
            }
            mValue_Watt = Value_Watt_30A + Value_Watt_50A;
            mValue_Energy = Value_Energy_30A + Value_Energy_50A;
        }
//        Log.d("print data", "Volt:" + String.valueOf(mValue_Volt_30A) + " Amp:" + String.valueOf(mValue_Amp_30A) + " Watt:" + String.valueOf(mValue_Watt_30A) + "Energy" + String.valueOf(mValue_Energy_30A));
    }

    /**
     *  顯示並監測數據是否Over
     * */
    private void ValueMonitor () {
        Textview_connected.setText(mCurrentConnect);
        int NotifiyId;
        String Tmp_Max;
        String Tmp_Min;
        //顯示幾位
        DecimalFormat DecimalFormat_Common = new DecimalFormat("#");
        //Tark
        DecimalFormat DecimalFormat_Energy = new DecimalFormat("#.0");
//        DecimalFormat DecimalFormat_Energy = new DecimalFormat("#.###");
        DecimalFormat DecimalFormat_Amp = new DecimalFormat("#.0");
        DecimalFormat_Common.setRoundingMode(RoundingMode.HALF_UP);
        DecimalFormat_Energy.setRoundingMode(RoundingMode.HALF_UP);
        DecimalFormat_Amp.setRoundingMode(RoundingMode.HALF_UP);
        //位數不足補零
        DecimalFormat_Common.applyPattern("0");
        DecimalFormat_Energy.applyPattern("0.0");
//        DecimalFormat_Energy.applyPattern("0.000");
        DecimalFormat_Amp.applyPattern("0.0");
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
                if(wl != null) {
                    wl.release();
                    wl = null;
                }
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
            int index;
            while (ConnectedNum > 0) {
                int device_index = mDeviceAdress_list.indexOf(mConnected_list.get(ConnectedNum - 1));
                if (device_index != -1) {
                    final BluetoothDevice device = mListBluetoothDevice.get(device_index);
                    mBluetoothDevice = device;
                    mDeviceAddress = device.getAddress();
                    index = mList_ScanDeviceMac.indexOf(device.getName());
                    Log.d("check", "Auto type:"+mList_ScanDeviceType.get(index));
                    if (mList_ScanDeviceType.get(index).equals("S")) {
                        isPage30A = true;
                    } else if (mList_ScanDeviceType.get(index).equals("D")) {
                        isPage30A = false;
                    }
                    LinkObjectValue();
                    mConnectDeviceName = mList_ScanDeviceName.get(index);
                    mDeviceMac = mList_ScanDeviceMac.get(index);
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
            } else if (mBluetoothLeService.mBluetoothGatt == null) {
                final boolean result = mBluetoothLeService.connect(DeviceAddress);
                Log.d(BluetoothLeService.TAG, "Connect request result=" + result);
            }
        }
    }


    /**
     * 儲存被允許Scan的Device相關訊息
     **/
    private void WriteScanDevice() {
        try {
            FileOutputStream OutputMac = openFileOutput("ScanMac.txt", Context.MODE_PRIVATE);
            for (String tmp : mList_ScanDeviceMac) {
                OutputMac.write((tmp + ",").toString().getBytes());
            }
            OutputMac.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            FileOutputStream OutputName = openFileOutput("ScanName.txt", Context.MODE_PRIVATE);
            for (String tmp : mList_ScanDeviceName) {
                OutputName.write((tmp + ",").toString().getBytes());
            }
            OutputName.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            FileOutputStream OutputType = openFileOutput("ScanType.txt", Context.MODE_PRIVATE);
            for (String tmp : mList_ScanDeviceType) {
                OutputType.write((tmp + ",").toString().getBytes());
            }
            OutputType.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑Function↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑**/
}

