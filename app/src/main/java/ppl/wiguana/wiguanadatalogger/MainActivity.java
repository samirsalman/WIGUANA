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

    private Location mLastLocation = null;
    private LocationProvider mLocProvider;
    private LocationManager locationManager;


    private enum statesModem { WAITING_FOR_RESET,RESET_RECIEVED,RF_INIZIALIZED};

    StringBuffer sb;


    private ArrayList<String> data;

    private double lat = 0;
    private double lon = 0;
    private double alt = 0;
    private double acc = 0;

    private boolean started;


    Button btStart;
    Button btStop;

    private int rcv_pkt;
    private int lost_pkt;

    private int lastsn;



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
    private TextView display;
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
        data= new ArrayList<String>(1000);
        sb = new StringBuffer();
        started=false;
        sdf = new SimpleDateFormat("-ddMMyyyy-HHmmss");

        scroll = (ScrollView) findViewById(R.id.dataLogCnt);
        display = (TextView) findViewById(R.id.dataLog);
        display.setText("--");
        display.setMovementMethod(new ScrollingMovementMethod());


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
                    String data = editText.getText().toString();
                    if (usbService != null) { // if UsbService was correctly binded, Send data
                        display.append(data);
                        usbService.write(data.getBytes());
                    }
                }
            }
        });*/

        btStart = (Button) findViewById(R.id.btStart);
        btStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!started) {
                    started=true;
                    rcv_pkt=0;
                    lost_pkt=0;
                    lastsn=-1;

                    display.setText("Started\n");
                }
            }
        });
        btStop = (Button) findViewById(R.id.btStop);
        btStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(started) {
                    started = false;
                    if(rcv_pkt>0)saveData();
                    lastsn = -1;
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

            pw.println("FileName: "+tmpfn+"  Pkt:"+rcv_pkt+" Lost:"+lost_pkt);

            for (int i = 0; i < data.size(); i++) {
                String string = data.get(i);


                pw.println(string);
            }
            pw.flush();
            pw.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        display.append("data saved in "+tmpfn);

        data.clear();
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
        startService(UsbService.class, usbConnection, null); // Start UsbService(if it was not started before) and Bind it

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
        if(started){
            int sn,diff;
            String[] strdata = line.split(",");

            if(strdata.length>4){
                sn = Integer.parseInt(strdata[3]);
                if(lastsn==-1){
                    lastsn=sn;
                }else {

                    diff = sn - lastsn;
                    if(diff>1){
                        lost_pkt=lost_pkt+diff-1;
                    }
                    lastsn=sn;
                }
            }
            rcv_pkt++;

            long mils = System.currentTimeMillis();
            String csvline=mils+","+line+","+lat+","+lon+","+alt+","+acc;
            data.add(csvline);
            display.append(""+rcv_pkt+" --- "+csvline+"\n");
            tvNum.setText(""+rcv_pkt);
            tvNum2.setText(""+lost_pkt);
            //tvBer.setText(""+rcv_pkt);

        }

    }


    public void parseMessage(String msg ){
        display.append("P ---\n");

        sb.append(msg);
        int ind = sb.indexOf("\n");
        if(ind>=0) {
            String line = sb.substring(0, ind-1);
            sb.delete(0, ind+1);
            recvLine(line);
            display.append(line + "---");
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
                    mActivity.get().display.append(data);
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
