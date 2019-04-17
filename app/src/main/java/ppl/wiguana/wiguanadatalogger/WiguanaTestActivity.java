package ppl.wiguana.wiguanadatalogger;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;

public class WiguanaTestActivity extends AppCompatActivity {


    private static final int INIT_MODEM=1;
    private static final int SEND_PACKET=2;

    private Location mLastLocation = null;
    private LocationProvider mLocProvider;
    private LocationManager locationManager;


    StringBuffer messageBuffer;


    private ArrayList<CSVLine> relevationData;

    private double lat = 0;
    private double lon = 0;
    private double alt = 0;
    private double acc = 0;

    private boolean startAutoRelevation;


    private Button startAutoRelevationButton;
    private Button stopAutoRelevationButton;
    private Button sendPackageButton;


    private int recevedPackages;
    private int lostPackages;

    private int lastSentPackage;

    private UsbService usbService;


    private final static int NOT_CONNECTED = 0;
    private final static int WAIT_FOR_MODEM = 1;
    private final static int INITIALIZATION = 2;
    private final static int READY = 3;
    private final static int TRANSMITTING = 4;
    private final static int RECEIVING = 5;


    private int modemState = NOT_CONNECTED;



    /*
     * Notifications from UsbService will be received here.
     */
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case UsbService.ACTION_USB_PERMISSION_GRANTED: // USB PERMISSION GRANTED
                    Toast.makeText(context, "USB Ready", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_PERMISSION_NOT_GRANTED: // USB PERMISSION NOT GRANTED
                    Toast.makeText(context, "USB Permission not granted", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_NO_USB: // NO USB CONNECTED
                    Toast.makeText(context, "No USB connected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_DISCONNECTED: // USB DISCONNECTED
                    Toast.makeText(context, "USB disconnected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_NOT_SUPPORTED: // USB NOT SUPPORTED
                    Toast.makeText(context, "USB device not supported", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    private TextView display;
    private TextView tvNum;
    private TextView tvNum2;
    private TextView tvBer;
    private TextView displayGpS;
    private EditText editText;
    private MyHandler mHandler;

    private ScrollView scroll;

    private ScrollView logScrollWrap;
    private TextView logDisplay;
    private TextView stateDispaly;



    private final ServiceConnection usbConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            usbService = ((UsbService.UsbBinder) arg1).getService();
            usbService.setHandler(mHandler);
            // Try to init the modem in 1 sec
            //mHandler.sendMessageDelayed(mHandler.obtainMessage(INIT_MODEM),1000);
            modemState = WAIT_FOR_MODEM;
            stateDispaly.setText("WAIT_FOR_MODEM");
            stateDispaly.invalidate();

        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            usbService = null;
        }
    };


    private LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            mLastLocation = location;
            if (mLastLocation != null) {
                lat = mLastLocation.getLatitude();
                lon = mLastLocation.getLongitude();
                alt = mLastLocation.getAltitude();
                acc = mLastLocation.getAccuracy();
                displayGpS.setText("GPS Data Lat:"+lat+" Lon:"+lon+" Alt:"+alt+" Acc:"+acc);
            }
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {}

        public void onProviderEnabled(String provider) {}

        public void onProviderDisabled(String provider) {}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHandler = new MyHandler(this);

        relevationData = new ArrayList<CSVLine>();
        messageBuffer = new StringBuffer();
        startAutoRelevation =false;
        sdf = new SimpleDateFormat("-ddMMyyyy-HHmmss");

        scroll = (ScrollView) findViewById(R.id.dataLogCnt);
        display = (TextView) findViewById(R.id.dataLog);
        display.setText("--");
        display.setMovementMethod(new ScrollingMovementMethod());


        //All relevationData received from the usb are logged here is log is activated
        logScrollWrap = (ScrollView) findViewById(R.id.usbLogWrap);
        logDisplay = (TextView) findViewById(R.id.usbLog);
        logDisplay.setText("--");
        logDisplay.setMovementMethod(new ScrollingMovementMethod());

        stateDispaly = (TextView) findViewById(R.id.stateDispaly);
        stateDispaly.setText("START");

        displayGpS = (TextView) findViewById(R.id.gpsStatus);
        tvNum = (TextView) findViewById(R.id.tvNum);
        tvNum2 = (TextView) findViewById(R.id.tvNum2);
        tvBer = (TextView) findViewById(R.id.tvBer);


        startAutoRelevationButton = (Button) findViewById(R.id.btStart);
        startAutoRelevationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!startAutoRelevation) {
                    startAutoRelevation =true;
                    recevedPackages =0;
                    lostPackages =0;
                    lastSentPackage =-1;

                    mHandler.sendMessage(mHandler.obtainMessage(1));

                    display.setText("Started\n");
                }



            }
        });
        stopAutoRelevationButton = (Button) findViewById(R.id.btStop);
        stopAutoRelevationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(startAutoRelevation) {
                    startAutoRelevation = false;
                    if(recevedPackages >0)saveData();
                    lastSentPackage = -1;
                }
            }
        });


        sendPackageButton = (Button) findViewById(R.id.btSend);
        sendPackageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendProbePacket();
            }
        });

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

    }

    private SimpleDateFormat sdf;

    private String fileName="WData";
    private String dirName="WDATA";


    private String getFilename(){
        long mils = System.currentTimeMillis();
        return fileName+sdf.format(new Date(mils))+".csv";
    }

    private void saveData(){
        PrintWriter pw;
        File outFile;

        String tmpfn="";
        try {
            File dir =new File(android.os.Environment.getExternalStorageDirectory(), dirName);
            if(!dir.exists())
            {
                dir.mkdirs();
            }
            tmpfn= getFilename();

            outFile = new File(dir+File.separator+tmpfn);

            FileOutputStream fOut = null;
            fOut = new FileOutputStream(outFile);
            pw = new PrintWriter(fOut);

            pw.println("FileName: "+tmpfn+"  Pkt:"+ recevedPackages +" Lost:"+ lostPackages);

            for (int i = 0; i < relevationData.size(); i++) {
                CSVLine csvLine = relevationData.get(i);


                pw.println(csvLine);
            }
            pw.flush();
            pw.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        display.append("relevationData saved in "+tmpfn);

        relevationData.clear();
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onStart() {
        super.onStart();
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 0, locationListener);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        setFilters();  // Start listening notifications from UsbService
        startService(UsbService.class, usbConnection, null); // Start UsbService(if it was not startAutoRelevation before) and Bind it

    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mUsbReceiver);
        unbindService(usbConnection);
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            locationManager.removeUpdates(locationListener);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras) {
        if (!UsbService.SERVICE_CONNECTED) {
            Intent startService = new Intent(this, service);
            if (extras != null && !extras.isEmpty()) {
                Set<String> keys = extras.keySet();
                for (String key : keys) {
                    String extra = extras.getString(key);
                    startService.putExtra(key, extra);
                }
            }
            startService(startService);
        }
        Intent bindingIntent = new Intent(this, service);
        bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void setFilters() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED);
        filter.addAction(UsbService.ACTION_NO_USB);
        filter.addAction(UsbService.ACTION_USB_DISCONNECTED);
        filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED);
        filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED);
        registerReceiver(mUsbReceiver, filter);
    }


    public void recvLine(String line){
        if(startAutoRelevation){
            int sn,diff;
            String[] strdata = line.split(",");

            if(strdata.length>4){
                sn = Integer.parseInt(strdata[3]);
                if(lastSentPackage ==-1){
                    lastSentPackage =sn;
                }else {

                    diff = sn - lastSentPackage;
                    if(diff>1){
                        lostPackages = lostPackages +diff-1;
                    }
                    lastSentPackage =sn;
                }
            }
            recevedPackages++;

            long mils = System.currentTimeMillis();
            CSVLine csvline=mils+","+line+","+lat+","+lon+","+alt+","+acc;
            relevationData.add(csvline);
            display.append(""+ recevedPackages +" --- "+csvline+"\n");
            tvNum.setText(""+ recevedPackages);
            tvNum2.setText(""+ lostPackages);
            //tvBer.setText(""+recevedPackages);

        }

    }



    public void parseMessage(String msg ){

        messageBuffer.append(msg);
        int ind = messageBuffer.indexOf("\r");

        if(ind>=0) {
            // received a command
            String line = messageBuffer.substring(0, ind);
            messageBuffer.delete(0, ind+1);

            String strippedline=line.replaceAll("\n"," -- ");
            display.append("RCV>"+strippedline+"\n");
            //parseResp(line);
            modemFSM(line);
        }
    }


    /*
     * This handler will be passed to UsbService. Data received from serial port is displayed through this handler
     */
    private static class MyHandler extends Handler {
        private final WeakReference<WiguanaTestActivity> mActivity;

        public MyHandler(WiguanaTestActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {

                case UsbService.MESSAGE_FROM_SERIAL_PORT: //0
                    String data = (String) msg.obj;
                    //mActivity.get().display.append("-I-"+relevationData);
                    mActivity.get().logDisplay.append(data);
                    mActivity.get().parseMessage(data);
                    break;

                case 1: //1 start test

                    break;
                case 2: //1 sendPacket

                    break;
            }
        }
    }





    private int okCnt;
    private boolean readRssi=false;

    private void modemFSM(String msg){
        if(modemState==WAIT_FOR_MODEM){
            // Wait for reset
            if(msg.contains("Reset")){
                okCnt=0;
                modemState= INITIALIZATION;
                stateDispaly.setText("INITIALIZATION S:"+okCnt);
                stateDispaly.invalidate();

                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        usbService.write("AT+i 02\n".getBytes());
                    }
                },500);
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        usbService.write("ATPBM=1\n".getBytes());
                    }
                },1000);
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        usbService.write("ATPPW=-10\n".getBytes());
                    }
                },1500);
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        usbService.write("ATPRO=20000000\n".getBytes());
                    }
                },2000);

            }
        }

        if(modemState== INITIALIZATION){
            if(msg.contains("OK")){
                okCnt++;
                stateDispaly.setText("INITIALIZATION S:"+okCnt);
                stateDispaly.invalidate();

            }

            if(okCnt>3){
                modemState=READY;
                stateDispaly.setText("READY");
            }
        }

        if(modemState==RECEIVING){

            if(msg.contains("OK") ){
                okCnt++;

                if(msg.contains("RX")){
                    // pacchetto ricevuto chiedo RSSI
                    usbService.write("ATPLR?\n".getBytes()); // leggo RSSI
                }
                if(okCnt>2) {
                    stateDispaly.setText("READY");
                    stateDispaly.invalidate();
                    modemState = READY;
                }

            }

            if(msg.contains("Error") ){
                stateDispaly.setText("READY");
                stateDispaly.invalidate();
                modemState = READY;
            }
        }

    }

    int pkcount =0;
    
    private void sendProbePacket(){
        if(modemState==READY) {
            pkcount++;

            String cntstr = ByteUtil.byteArrayToString(ByteUtil.uint32BufferBE(pkcount));

            String payload = "1003"+cntstr+"0A01";
            usbService.write("ATPTA=00\n".getBytes());
            usbService.write(("AT+TX "+payload+"\n").getBytes());
            usbService.write("AT+RX\n".getBytes());
            modemState=RECEIVING;
            stateDispaly.setText("RECEIVING");
            okCnt=0;

        }
    }


    private void makeScroll(final int go){
        scroll.post(new Runnable() {
            public void run() {
                scroll.scrollTo(0, go);
            }
        });

    }
}
