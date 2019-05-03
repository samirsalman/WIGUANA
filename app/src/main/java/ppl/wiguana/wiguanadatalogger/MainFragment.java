package ppl.wiguana.wiguanadatalogger;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
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


public class MainFragment extends Fragment {

	private OnFragmentInteractionListener mListener;
	private ConstraintLayout loading;
	private static final int INIT_MODEM=1;
	private static final int SEND_PACKET=2;
	public final static String GW_LAT = "GW_LAT";
	public final static String GW_LON = "GW_LON";
	public final static String GW_ALT = "GW_ALT";
	private Location mLastLocation = null;
	private LocationManager locationManager;
	private StringBuffer stringBuffer;
	private ArrayList<String> allDataLogHistory;
	private double gpsLat = 0;
	private double gpsLon = 0;
	private double gpsAlt = 0;
	private double gpsAccuracy = 0;
	private boolean started;
	private Location gwLocation = null;
	private ImageButton startButton;
	private ImageButton stopButton;
	private ImageButton sendButton;
	private int receivedPackages;
	private int lostPackages;
	private int lastSent;
	private UsbService usbService;
	private String dataStr = "";
	private final static int NOT_CONNECTED = 0;
	private final static int WAIT_FOR_MODEM = 1;
	private final static int INITIALIZATION = 2;
	private final static int READY = 3;
	//private final static int TRANSMITTING = 4;
	private final static int RECEIVING = 5;
	private final static int REQ_RSSI = 6;
	private int modemState = NOT_CONNECTED;
	FragmentManager fm;
	MainFragment home;
	//NotificationFragment notification;
	//DashBoardFragment dashBoard;
	FragmentTransaction ft;
	private TextView display;
	private TextView receivedPackagesTv;
	private TextView lostPackagesTv;
	private TextView gpsData;
	ConstraintLayout root;
	private MainFragment.MyHandler mHandler;
	private ScrollView scroll;
	private TextView logTextView;
	private TextView modemStateTextView;



	public MainFragment() {
		// Required empty public constructor
	}

	/**
	 * Use this factory method to create a new instance of
	 * this fragment using the provided parameters.
	 *
	 * @param param1 Parameter 1.
	 * @param param2 Parameter 2.
	 * @return A new instance of fragment MainFragment.
	 */
	// TODO: Rename and change types and number of parameters
	public static MainFragment newInstance(String param1, String param2) {
		MainFragment fragment = new MainFragment();
		Bundle args = new Bundle();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (getArguments() != null) {

		}

	}


	@SuppressLint("MissingPermission")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		// Inflate the layout for this fragment


		View v = inflater.inflate(R.layout.fragment_main, container, false);
		loading = v.findViewById(R.id.loading_background);
		mHandler = new MainFragment.MyHandler(this);
		allDataLogHistory = new ArrayList<String>(1000);
		stringBuffer = new StringBuffer();
		root = v.findViewById(R.id.root);
		started=false;
		sdf = new SimpleDateFormat("-ddMMyyyy-HHmmss");
		scroll = v.findViewById(R.id.dataLogCnt);
		display = v.findViewById(R.id.relevationLog);
		display.setText("");
		//All allDataLogHistory received from the usb are logged here is log is activated
		logTextView = v.findViewById(R.id.usbLog);
		logTextView.setText("");
		modemStateTextView = v.findViewById(R.id.deviceState);
		modemStateTextView.setText("START");
		gpsData = v.findViewById(R.id.gpsData);
		receivedPackagesTv = v.findViewById(R.id.receivedPackages);
		lostPackagesTv = v.findViewById(R.id.tvBer);
		startButton = v.findViewById(R.id.btStart);
		stopButton = v.findViewById(R.id.btStop);
		sendButton = v.findViewById(R.id.btSend);
		sendButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				sendProbePacket();
			}
		});
		locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
		locationManager
			.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100, 0, locationListener);
		return v;
	}


	// TODO: Rename method, update argument and hook method into UI event
	public void onButtonPressed(Uri uri) {
		if (mListener != null) {
			mListener.onFragmentInteraction(uri);
		}
	}

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		if (context instanceof OnFragmentInteractionListener) {
			mListener = (OnFragmentInteractionListener) context;
		} else {
			throw new RuntimeException(context.toString()
				+ " must implement OnFragmentInteractionListener");
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mListener = null;
	}

	/**
	 * This interface must be implemented by activities that contain this
	 * fragment to allow an interaction in this fragment to be communicated
	 * to the activity and potentially other fragments contained in that
	 * activity.
	 * <p>
	 * See the Android Training lesson <a href=
	 * "http://developer.android.com/training/basics/fragments/communicating.html"
	 * >Communicating with Other Fragments</a> for more information.
	 */
	public interface OnFragmentInteractionListener {
		// TODO: Update argument type and name
		void onFragmentInteraction(Uri uri);
	}


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




	private final ServiceConnection usbConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName arg0, IBinder arg1) {
			usbService = ((UsbService.UsbBinder) arg1).getService();
			usbService.setHandler(mHandler);
			// Try to init the modem in 1 sec
			//mHandler.sendMessageDelayed(mHandler.obtainMessage(INIT_MODEM),1000);
			modemState = WAIT_FOR_MODEM;
			modemStateTextView.setText("Attendo il Modem");
			modemStateTextView.invalidate();

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
				gwLocation=mLastLocation;
				loading.setVisibility(View.INVISIBLE);
				gpsLat = mLastLocation.getLatitude();
				gpsLon = mLastLocation.getLongitude();
				gpsAlt = mLastLocation.getAltitude();
				gpsAccuracy = mLastLocation.getAccuracy();
				gpsData.setText("GPS - Lat:"+ gpsLat +" Lon:"+ gpsLon +" Alt:"+ gpsAlt +" Acc:"+ gpsAccuracy);
			}
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {}

		public void onProviderEnabled(String provider) {}

		public void onProviderDisabled(String provider) {}
	};


	public void startAuto(View view){
		if (!started) {
			if(gwLocation!=null) {
				if(modemState==READY) {
					started = true;
					receivedPackages = 0;
					lostPackages = 0;
					lastSent = -1;
					mHandler.sendMessage(mHandler.obtainMessage(2));
					display.setText("Started\n");
				}else{
					Snackbar.make(root,"Modem not Ready", Snackbar.LENGTH_LONG).show();
				}
			}else{
				Snackbar.make(root,"Gps not found!", Snackbar.LENGTH_LONG).show();
			}
		}
	}

	public void stopAuto(View view){
		if(started) {
			started = false;
			if(allDataLogHistory.size()>0)saveData();
			lastSent = -1;
		}
	}

	private SimpleDateFormat sdf;

	private String fileName="WData";
	private String dirName="WDATA";


	private String getFilename(){
		long mils = System.currentTimeMillis();
		return fileName+sdf.format(new Date(mils))+".json";
		//Cambiato formato da csv a json
	}

	private void saveData(){

		// Devo creare l'oggetto RILEVAZIONE con GPSData e Potenze segnali, poi lo scrivo in json sul file
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

			//pw.println("FileName: "+tmpfn+"  Pkt:"+receivedPackages+" Lost:"+lostPackages);
			for (int i = 0; i < allDataLogHistory.size(); i++) {
				String string = allDataLogHistory.get(i);


				pw.println(string);
			}
			pw.flush();
			pw.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		display.append("allDataLogHistory saved in "+tmpfn);
		allDataLogHistory.clear();
	}

	PowerManager.WakeLock wakeLock;



	@Override
	public void onResume() {
		super.onResume();
		setFilters();  // Start listening notifications from UsbService
		startService(UsbService.class, usbConnection, null); // Start UsbService(if it was not started before) and Bind it

	}

	@Override
	public void onPause() {
		super.onPause();
		//usbService.unregisterReceiver(mUsbReceiver);
		//usbService.unbindService(usbConnection);
	}

	@Override
	public void onStop() {
		super.onStop();
		try {
			locationManager.removeUpdates(locationListener);
		}catch (Exception e){
			e.printStackTrace();
		}
		//wakeLock.acquire();

	}

	private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras) {
		if (!UsbService.SERVICE_CONNECTED) {
			Intent startService = new Intent(getActivity(), service);
			if (extras != null && !extras.isEmpty()) {
				Set<String> keys = extras.keySet();
				for (String key : keys) {
					String extra = extras.getString(key);
					startService.putExtra(key, extra);
				}
			}
			getActivity().startService(startService);
		}
		Intent bindingIntent = new Intent(getActivity(), service);
		getActivity().bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
	}

	private void setFilters() {
		IntentFilter filter = new IntentFilter();
		filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED);
		filter.addAction(UsbService.ACTION_NO_USB);
		filter.addAction(UsbService.ACTION_USB_DISCONNECTED);
		filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED);
		filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED);
		getActivity().registerReceiver(mUsbReceiver, filter);
	}


	public void parseMessage(String msg ){

		stringBuffer.append(msg);
		int ind = stringBuffer.indexOf("\r");
		if(ind>=0) {
			// received a command
			String line = stringBuffer.substring(0, ind);
			stringBuffer.delete(0, ind+1);

			//String strippedline=line.replaceAll("\n"," -- ");
			//display.append("RCV>"+strippedline+"\n");
			//parseResp(line);
			modemFSM(line);
		}
	}


	/*
	 * This handler will be passed to UsbService. Data received from serial port is displayed through this handler
	 */
	private static class MyHandler extends Handler {
		private final WeakReference<MainFragment> mActivity;

		public MyHandler(MainFragment activity) {
			mActivity = new WeakReference<>(activity);
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {

				case UsbService.MESSAGE_FROM_SERIAL_PORT: //0
					String data = (String) msg.obj;
					mActivity.get().logTextView.append(data);
					mActivity.get().parseMessage(data);
					break;

				case 1: //1 start test

					break;
				case 2: //1 sendPacket
					if(mActivity.get().isStarted()) {
						mActivity.get().sendProbePacket();
						this.sendMessageDelayed(obtainMessage(2), 10000);
					}
					break;
			}
		}
	}




	public boolean isStarted(){
		return started;
	}

	private int stateCounter;

	private void modemFSM(String msg) {
		if (modemState == WAIT_FOR_MODEM) {
			// Wait for reset
			if (msg.contains("Reset")) {
				stateCounter = 0;
				modemState = INITIALIZATION;
				modemStateTextView.setText("INITIALIZATION FASE:" + stateCounter);
				modemStateTextView.invalidate();

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
				stateCounter++;
				modemStateTextView.setText("INITIALIZATION FASE:" + stateCounter);
				modemStateTextView.invalidate();
			}

			if (stateCounter > 3) {
				modemState = READY;
				modemStateTextView.setText("MODEM IS READY");
			}
		}

		if (modemState == RECEIVING) {

			if (msg.contains("OK")) {
				stateCounter++;

				if (msg.contains("RX")) {
					// pacchetto ricevuto chiedo RSSI
					usbService.write("ATPLR?\n".getBytes()); // leggo RSSI
					//AGGIUNGO DATI ALL'INTERNO DI DATA (ARRAYLIST DA SALVARE POI IN SAVEDATA)
				}
				if (stateCounter > 2) {
					modemStateTextView.setText("MODEM READY");
					modemStateTextView.invalidate();
					modemState = READY;
				}

			}

			if (msg.contains("Error")) {
				Snackbar.make(root,"Package error  : " + msg,Snackbar.LENGTH_LONG).show();
				modemStateTextView.setText("MODEM READY");
				modemStateTextView.invalidate();
				modemState = READY;
			}
		}

	}

	int packagesCount =0;

	public void sendProbePacket(){
		if(modemState==READY) {
			packagesCount++;
			dataStr="";

			String cntstr = ByteUtil.byteArrayToString(ByteUtil.uint32BufferBE(packagesCount));
			String payload = "1003"+cntstr+"0A01";
			usbService.write("ATPTA=00\n".getBytes());
			usbService.write(("AT+TX "+payload+"\n").getBytes());
			usbService.write("AT+RX\n".getBytes());
			modemState=RECEIVING;

			long unixTime = System.currentTimeMillis() / 1000L;

			modemStateTextView.setText("RECEIVING");
			float distance_meters = mLastLocation.distanceTo(gwLocation);
			dataStr = dataStr+ packagesCount +","+unixTime+","+mLastLocation.getLongitude()
				+","+mLastLocation.getLatitude()
				+","+mLastLocation.getAltitude()
				+","+gwLocation.getLongitude()
				+","+gwLocation.getLatitude()
				+","+gwLocation.getAltitude()
				+","+distance_meters;

			stateCounter =0;

		}
	}



	SharedPreferences.Editor putDouble(final SharedPreferences.Editor edit, final String key, final double value) {
		return edit.putLong(key, Double.doubleToRawLongBits(value));
	}

	double getDouble(final SharedPreferences prefs, final String key, final double defaultValue) {
		return Double.longBitsToDouble(prefs.getLong(key, Double.doubleToLongBits(defaultValue)));
	}


}








