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
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
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

public class MainActivity extends AppCompatActivity {

    //Oggetti per gestire la posizione GPS
    private Location mLastLocation = null;
    private LocationProvider mLocProvider;
    private LocationManager locationManager;


    private enum modemState { WAITING_FOR_RESET,RESET_RECIEVED,RF_INIZIALIZED}; //enum che descrive lo stato del modem

    private StringBuffer messageBuffer; //compone il messaggio da scrivere sulla textView ID=dataLog


    private ArrayList<CSVLine> relevationData; //ArrayList che contiene i dati sulle rilevazioni effettuate(LATITUDINE,LONGITUDINE,ecc..)

    //Variabili GPS
    private double gpsLatidude = 0;
    private double gpsLongitude = 0;
    private double gpsAltitude = 0;
    private double gpsAccuracy = 0;

    private boolean startAutoRelevation;


    private Button startAutoRelevationButton;
    private Button stopAutoRelevationButton;

    private int recevedPackage; //count receved packages
    private int lostPackage; //count losted packages

    private int lastPackageSent;



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

    private UsbService usbService;
    private TextView dataLog;
    private TextView tvNum;
    private TextView tvNum2;
    private TextView tvBer;
    private TextView displayGpS;
    private EditText editText;
    private MyHandler mHandler;

    private ScrollView scroll;
    private final ServiceConnection usbConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            usbService = ((UsbService.UsbBinder) arg1).getService();
            usbService.setHandler(mHandler);
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
                gpsLatidude = mLastLocation.getLatitude();
                gpsLongitude = mLastLocation.getLongitude();
                gpsAltitude = mLastLocation.getAltitude();
                gpsAccuracy = mLastLocation.getAccuracy();
                displayGpS.setText("GPS Data Lat:"+ gpsLatidude +" Lon:"+ gpsLongitude +" Alt:"+ gpsAltitude +" Acc:"+ gpsAccuracy);
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
        dataLog = (TextView) findViewById(R.id.dataLog);
        dataLog.setText("--");
        dataLog.setMovementMethod(new ScrollingMovementMethod());


        displayGpS = (TextView) findViewById(R.id.gpsStatus);
        tvNum = (TextView) findViewById(R.id.tvNum);
        tvNum2 = (TextView) findViewById(R.id.tvNum2);
        tvBer = (TextView) findViewById(R.id.tvBer);
        //editText = (EditText) findViewById(R.id.editText1);
        //Button sendButton = (Button) findViewById(R.id.buttonSend);
        /*sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!editText.getText().toString().equals("")) {
                    String relevationData = editText.getText().toString();
                    if (usbService != null) { // if UsbService was correctly binded, Send relevationData
                        dataLog.append(relevationData);
                        usbService.write(relevationData.getBytes());
                    }
                }
            }
        });*/

        startAutoRelevationButton = (Button) findViewById(R.id.btStart);
        startAutoRelevationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!startAutoRelevation) {
                    startAutoRelevation =true;
                    recevedPackage =0;
                    lostPackage =0;
                    lastPackageSent =-1;

                    dataLog.setText("Started\n");
                }
            }
        });
        stopAutoRelevationButton = (Button) findViewById(R.id.btStop);
        stopAutoRelevationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(startAutoRelevation) {
                    startAutoRelevation = false;
                    if(recevedPackage >0)saveData();
                    lastPackageSent = -1;
                }
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

            pw.println("FileName: "+tmpfn+"  Pkt:"+ recevedPackage +" Lost:"+ lostPackage);

            for (int i = 0; i < relevationData.size(); i++) {
                CSVLine csvLine= relevationData.get(i);
                pw.println(csvLine.toString());
            }
            pw.flush();
            pw.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        dataLog.append("relevationData saved in "+tmpfn);

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
                if(lastPackageSent ==-1){
                    lastPackageSent =sn;
                }else {

                    diff = sn - lastPackageSent;
                    if(diff>1){
                        lostPackage = lostPackage +diff-1;
                    }
                    lastPackageSent =sn;
                }
            }
            recevedPackage++;

            long mils = System.currentTimeMillis();
            CSVLine csvline=new CSVLine(mils,line,gpsLatidude,gpsLongitude ,gpsAltitude ,gpsAccuracy);
            relevationData.add(csvline);
            dataLog.append(""+ recevedPackage +" --- "+csvline.toString());
            tvNum.setText(""+ recevedPackage);
            tvNum2.setText(""+ lostPackage);
            //tvBer.setText(""+recevedPackage);

        }

    }


    public void parseMessage(String msg ){
        dataLog.append("P ---\n");

        messageBuffer.append(msg);
        int ind = messageBuffer.indexOf("\n");
        if(ind>=0) {
            String line = messageBuffer.substring(0, ind-1);
            messageBuffer.delete(0, ind+1);
            recvLine(line);
            dataLog.append(line + "---");
        }
    }


    /*
     * This handler will be passed to UsbService. Data received from serial port is displayed through this handler
     */
    private static class MyHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;

        public MyHandler(MainActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UsbService.MESSAGE_FROM_SERIAL_PORT:
                    String data = (String) msg.obj;
                    mActivity.get().parseMessage(data);
                    mActivity.get().dataLog.append(data);
                    break;
            }
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
