package ppl.wiguana.wiguanadatalogger;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
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


	private static final int INIT_MODEM = 1;
	private static final int SEND_PACKET = 2;
	private Location mLastLocation = null;
	private LocationProvider mLocProvider;
	private LocationManager locationManager;
	StringBuffer messageBuffer;
	private ArrayList<CSVLine> relevationData;
	private double gpsLatitude = 0;
	private double gpsLongitude = 0;
	private double gpsAltitude = 0;
	private double gpsAccuracy = 0;
	private boolean startAutoRelevation;
	private Button startAutoRelevationButton;
	private Button stopAutoRelevationButton;
	private Button sendPackageButton;
	private int receivedPackages;
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
	private SimpleDateFormat sdf;
	private String fileName = "WData";
	private String dirName = "WDATA";


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

	private TextView relevationLog;
	private TextView receivedPackagesTv;
	private TextView lostPackagesTv;
	// private TextView tvBer;
	private TextView gpsDataTv;
	private MyHandler mHandler;
	private ScrollView scroll;
	private ScrollView logScrollWrap;
	private TextView usbLogTv;
	private TextView deviceState;


	private final ServiceConnection usbConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName arg0, IBinder arg1) {
			usbService = ((UsbService.UsbBinder) arg1).getService();
			usbService.setHandler(mHandler);
			// Try to init the modem in 1 sec
			//mHandler.sendMessageDelayed(mHandler.obtainMessage(INIT_MODEM),1000);
			modemState = WAIT_FOR_MODEM;
			deviceState.setText("Attendo segnale dal modem");
			deviceState.invalidate();

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
				gpsLatitude = mLastLocation.getLatitude();
				gpsLongitude = mLastLocation.getLongitude();
				gpsAltitude = mLastLocation.getAltitude();
				gpsAccuracy = mLastLocation.getAccuracy();
				gpsDataTv.setText("GPS Data Lat:" + gpsLatitude + " Lon:" + gpsLongitude + " Alt:" + gpsAltitude + " Acc:" + gpsAccuracy);
			}
		}


		public void onStatusChanged(String provider, int status, Bundle extras) {
		}

		public void onProviderEnabled(String provider) {
		}

		public void onProviderDisabled(String provider) {
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		checkLocationPermission();//check for permission and require permissions
		mHandler = new MyHandler(this);
		relevationData = new ArrayList<CSVLine>();
		messageBuffer = new StringBuffer();
		startAutoRelevation = false;
		sdf = new SimpleDateFormat("-ddMMyyyy-HHmmss");
		scroll = (ScrollView) findViewById(R.id.dataLogCnt);
		relevationLog = (TextView) findViewById(R.id.relevationLog);
		relevationLog.setMovementMethod(new ScrollingMovementMethod());
		//All relevationData received from the usb are logged here is log is activated
		logScrollWrap = (ScrollView) findViewById(R.id.usbLogWrap);
		usbLogTv = (TextView) findViewById(R.id.usbLog);
		usbLogTv.setText("--");
		usbLogTv.setMovementMethod(new ScrollingMovementMethod());
		deviceState = (TextView) findViewById(R.id.deviceState);
		deviceState.setText("START");
		gpsDataTv = (TextView) findViewById(R.id.gpsData);
		receivedPackagesTv = (TextView) findViewById(R.id.receivedPackages);
		lostPackagesTv = (TextView) findViewById(R.id.lostPackages);
		//tvBer = (TextView) findViewById(R.id.tvBer);
		startAutoRelevationButton = (Button) findViewById(R.id.btStart);
		stopAutoRelevationButton = (Button) findViewById(R.id.btStop);
		sendPackageButton = (Button) findViewById(R.id.btSend);

	}




	private String getFilename() {
		long mils = System.currentTimeMillis();
		return fileName + sdf.format(new Date(mils)) + ".csv";
	}

	private void saveData() {
		PrintWriter pw;
		File outFile;

		String tmpfn = "";
		try {
			File dir = new File(android.os.Environment.getExternalStorageDirectory(), dirName);
			if (!dir.exists()) {
				dir.mkdirs();
			}
			tmpfn = getFilename();

			outFile = new File(dir + File.separator + tmpfn);

			FileOutputStream fOut = null;
			fOut = new FileOutputStream(outFile);
			pw = new PrintWriter(fOut);

			pw.println("FileName: " + tmpfn + "  Pkt:" + receivedPackages + " Lost:" + lostPackages);

			for (int i = 0; i < relevationData.size(); i++) {
				CSVLine csvLine = relevationData.get(i);


				pw.println(csvLine);
			}
			pw.flush();
			pw.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		relevationLog.append("relevationData saved in " + tmpfn);

		relevationData.clear();
	}

	@Override
	protected void onStart() {
		super.onStart();

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
		} catch (Exception e) {
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


	public void recvLine(String line) {
		if (startAutoRelevation) {
			int sn, diff;
			String[] strdata = line.split(",");

			if (strdata.length > 4) {
				sn = Integer.parseInt(strdata[3]);
				if (lastSentPackage == -1) {
					lastSentPackage = sn;
				} else {

					diff = sn - lastSentPackage;
					if (diff > 1) {
						lostPackages = lostPackages + diff - 1;
					}
					lastSentPackage = sn;
				}
			}
			receivedPackages++;

			long mils = System.currentTimeMillis();
			CSVLine csvline = new CSVLine(mils, line, gpsLatitude, gpsLongitude, gpsAltitude, gpsAccuracy);
			relevationData.add(csvline);
			relevationLog.append("" + receivedPackages + " --- " + csvline);
			receivedPackagesTv.setText("" + receivedPackages);
			lostPackagesTv.setText("" + lostPackages);
			//tvBer.setText(""+receivedPackages);

		}

	}


	public void parseMessage(String msg) {

		messageBuffer.append(msg);
		int ind = messageBuffer.indexOf("\r");

		if (ind >= 0) {
			// received a command
			String line = messageBuffer.substring(0, ind);
			messageBuffer.delete(0, ind + 1);

			String strippedline = line.replaceAll("\n", " -- ");
			relevationLog.append("RCV>" + strippedline + "\n");
			//parseResp(line);
			modemFSM(line);
		}
	}

	public void startAutoRel(View view) {

		if (!startAutoRelevation) {
			startAutoRelevation = true;
			receivedPackages = 0;
			lostPackages = 0;
			lastSentPackage = -1;

			mHandler.sendMessage(mHandler.obtainMessage(1));

			relevationLog.setText("Started\n");
		}
	}

	public void stopAutoRel(View view) {
		if (startAutoRelevation) {
			startAutoRelevation = false;
			if (receivedPackages > 0) saveData();
			lastSentPackage = -1;
		}
	}

	public void sendPackages(View view) {

		if (modemState == READY) {
			packagesCounter++;

			String cntstr = ByteUtil.byteArrayToString(ByteUtil.uint32BufferBE(packagesCounter));

			String payload = "1003" + cntstr + "0A01";
			usbService.write("ATPTA=00\n".getBytes());
			usbService.write(("AT+TX " + payload + "\n").getBytes());
			usbService.write("AT+RX\n".getBytes());
			modemState = RECEIVING;
			deviceState.setText("RECEIVING");
			okCnt = 0;

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
					String receivedUSBData = (String) msg.obj;
					//mActivity.get().relevationLog.append("-I-"+relevationData);
					mActivity.get().usbLogTv.append(receivedUSBData);
					mActivity.get().parseMessage(receivedUSBData);
					break;

				case 1: //1 start test

					break;
				case 2: //1 sendPacket

					break;
			}
		}
	}


	private int okCnt;

	private void modemFSM(String msg) {
		if (modemState == WAIT_FOR_MODEM) {
			// Wait for reset
			if (msg.contains("Reset")) {
				okCnt = 0;
				modemState = INITIALIZATION;
				deviceState.setText("INITIALIZATION S:" + okCnt);
				deviceState.invalidate();

				mHandler.postDelayed(new Runnable() {
					@Override
					public void run() {
						usbService.write("AT+i 02\n".getBytes());
					}
				}, 500);
				mHandler.postDelayed(new Runnable() {
					@Override
					public void run() {
						usbService.write("ATPBM=1\n".getBytes());
					}
				}, 1000);
				mHandler.postDelayed(new Runnable() {
					@Override
					public void run() {
						usbService.write("ATPPW=-10\n".getBytes());
					}
				}, 1500);
				mHandler.postDelayed(new Runnable() {
					@Override
					public void run() {
						usbService.write("ATPRO=20000000\n".getBytes());
					}
				}, 2000);

			}
		}

		if (modemState == INITIALIZATION) {
			if (msg.contains("OK")) {
				okCnt++;
				deviceState.setText("INITIALIZATION S:" + okCnt);
				deviceState.invalidate();
			}

			if (okCnt > 3) {
				modemState = READY;
				deviceState.setText("READY");
			}
		}

		if (modemState == RECEIVING) {

			if (msg.contains("OK")) {
				okCnt++;

				if (msg.contains("RX")) {
					// pacchetto ricevuto chiedo RSSI
					usbService.write("ATPLR?\n".getBytes()); // leggo RSSI
				}
				if (okCnt > 2) {
					deviceState.setText("READY");
					deviceState.invalidate();
					modemState = READY;
				}

			}

			if (msg.contains("Error")) {
				deviceState.setText("READY");
				deviceState.invalidate();
				modemState = READY;
			}
		}

	}

	private int packagesCounter = 0;

	public void checkLocationPermission() {
		//Creo finestra di dialogo per richiedere il permesso GPS
		ActivityCompat.requestPermissions(WiguanaTestActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
	}

	@SuppressLint("MissingPermission")
	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		//Questa funzione verifica i permessi alla conferma

		switch(requestCode){
			case 1:
					//scorro la lista dei permessi che posseggo e verifico se il mio permesso si trova tra essi, in caso positivo cerco la posizione altrimenti richiedo il permesso
					for (int i = 0; i < permissions.length; i++) {
						String permission = permissions[i];
						int grantResult = grantResults[i];

						if (permission.equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
							if (grantResult == PackageManager.PERMISSION_GRANTED) {
								if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
									buildAlertMessageNoGps();
								}else {
									locationManager
										.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100, 0, locationListener);
								}
							} else {
								ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
							}
						}
					}


		}



	}


		@SuppressLint("MissingPermission")
		private void buildAlertMessageNoGps(){

		//metodo che crea la dialog per l'attivazione del GPS
			final AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("Il tuo GPS risulta disattivo, per far funzionare l'applicazione è richiesto il segnale GPS, attivarlo?")
				.setIcon(R.drawable.gps)
				.setCancelable(false)
				.setPositiveButton("Si", new DialogInterface.OnClickListener() {
					@SuppressLint("MissingPermission")
					public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
						startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS),01);
						}
				})
				.setNegativeButton("No", new DialogInterface.OnClickListener() {
					public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
						dialog.cancel();
					}
				});
			final AlertDialog alert = builder.create();
			alert.show();
		}

	@SuppressLint("MissingPermission")
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		//Risultato dell'activity che attiva il GPS
		if(requestCode==01){
			locationManager
				.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100, 0, locationListener);

		}

	}
}


