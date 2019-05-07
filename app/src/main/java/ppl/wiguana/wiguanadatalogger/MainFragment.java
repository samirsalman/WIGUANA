package ppl.wiguana.wiguanadatalogger;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
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
import android.widget.LinearLayout;
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
	private ScrollView scrollLog;
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
	private boolean started=false;
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
	private TextView receivedPackagesTv;
	private TextView lostPackagesTv;
	private TextView latitudeTv;
	private TextView longitudeTv;
	private TextView altitudeTv;
	private TextView accuracyTv;
	private ConstraintLayout root;
	private MainFragment.MyHandler mHandler;
	private TextView logTextView;
	private TextView modemStateTextView;
	private int packagesCount =0;
	private LinearLayout gpsData;




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
		gpsData = v.findViewById(R.id.gpsData);
		loading = v.findViewById(R.id.loading_background);
		scrollLog= v.findViewById(R.id.usbLogWrap);
		mHandler = new MainFragment.MyHandler(this);
		allDataLogHistory = new ArrayList<String>(1000);
		stringBuffer = new StringBuffer();
		root = v.findViewById(R.id.root);
		started=false;
		sdf = new SimpleDateFormat("-ddMMyyyy-HHmmss");
		//All allDataLogHistory received from the usb are logged here is log is activated
		logTextView = v.findViewById(R.id.usbLog);
		logTextView.setText("");
		modemStateTextView = v.findViewById(R.id.deviceState);
		receivedPackagesTv = v.findViewById(R.id.receivedPackages);
		lostPackagesTv = v.findViewById(R.id.tvBer);
		startButton = v.findViewById(R.id.btStart);
		stopButton = v.findViewById(R.id.btStop);
		sendButton = v.findViewById(R.id.btSend);
		latitudeTv = v.findViewById(R.id.latitude);
		longitudeTv = v.findViewById(R.id.longitude);
		altitudeTv = v.findViewById(R.id.altitude);
		accuracyTv = v.findViewById(R.id.accuracy);
		sendButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				sendProbePacket();
			}
		});
		startButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startAuto(v);
			}
		});
		stopButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				stopAuto(v);
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

	public void setModemStateTextView(int state){

		switch(state){
			case 0:
				modemStateTextView.setText("Non connesso");
				break;

			case 1:
				modemStateTextView.setText("Attendo il modem");
				break;

			case 2:
				modemStateTextView.setText("Inizializzazione");
				break;

			case 3:
				modemStateTextView.setText("MODEM IS READY");
				break;

			case 5:
				modemStateTextView.setText("Ricevo");
				break;

			case 6:
				modemStateTextView.setText("Ricevo RSSI");
				break;
		}
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
					Snackbar snackbar = Snackbar.make(root,"USB Ready",Snackbar.LENGTH_SHORT);
					snackbar.getView().setBackgroundColor(Color.GREEN);
					snackbar.show();
					break;

				case UsbService.ACTION_USB_PERMISSION_NOT_GRANTED: // USB PERMISSION NOT GRANTED
					Snackbar snackbarNG = Snackbar.make(root,"USB Permission not granted",Snackbar.LENGTH_SHORT);
					snackbarNG.getView().setBackgroundColor(Color.RED);
					snackbarNG.show();
					break;

				case UsbService.ACTION_NO_USB: // NO USB CONNECTED
					Snackbar snackbarNOUSB = Snackbar.make(root,"No USB connected",Snackbar.LENGTH_SHORT);
					snackbarNOUSB.getView().setBackgroundColor(Color.RED);
					snackbarNOUSB.show();
					break;

				case UsbService.ACTION_USB_DISCONNECTED: // USB DISCONNECTED
					Snackbar snackbarDC = Snackbar.make(root,"USB disconnected",Snackbar.LENGTH_SHORT);
					snackbarDC.getView().setBackgroundColor(Color.RED);
					snackbarDC.show();
					break;

				case UsbService.ACTION_USB_NOT_SUPPORTED: // USB NOT SUPPORTED
					Snackbar snackbarNS = Snackbar.make(root,"USB device not supported",Snackbar.LENGTH_SHORT);
					snackbarNS.getView().setBackgroundColor(Color.RED);
					snackbarNS.show();
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
				//LAT
				latitudeTv.setText("LAT:\n" + String.valueOf(gpsLat));
				longitudeTv.setText("LON:\n" + String.valueOf(gpsLon));
				altitudeTv.setText("ALT:\n" + String.valueOf(gpsAlt));
				accuracyTv.setText("ACC:\n" + String.valueOf(gpsAccuracy));
				//LON
				//ALT
				//ACCURACY
				//gpsData.setText("GPS - Lat:"+ gpsLat +" Lon:"+ gpsLon +" Alt:"+ gpsAlt +" Acc:"+ gpsAccuracy);
			}
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {}

		public void onProviderEnabled(String provider) {}

		public void onProviderDisabled(String provider) {}
	};


	public void startAuto(View view){
		if(modemState == WAIT_FOR_MODEM || modemState== NOT_CONNECTED){
				Snackbar snackbar= Snackbar.make(root,"Modem not ready yet",Snackbar.LENGTH_LONG);
				snackbar.getView().setBackgroundColor(Color.RED);
				snackbar.show();
		}else {
			if (!started) {
				if (gwLocation != null) {
					if (modemState == READY) {
						started = true;
						receivedPackages = 0;
						lostPackages = 0;
						lastSent = -1;
						mHandler.sendMessage(mHandler.obtainMessage(2));
						Snackbar.make(root, "Start Autorelevation", Snackbar.LENGTH_LONG).show();
					} else {
						Snackbar.make(root, "Modem not Ready", Snackbar.LENGTH_LONG).show();
					}
				} else {
					Snackbar.make(root, "Gps not found!", Snackbar.LENGTH_LONG).show();
				}
			}
		}
	}

	public void stopAuto(View view){
		if(modemState == WAIT_FOR_MODEM || modemState== NOT_CONNECTED){
			Snackbar snackbar= Snackbar.make(root,"Modem not ready yet",Snackbar.LENGTH_LONG);
			snackbar.getView().setBackgroundColor(Color.RED);
			snackbar.show();
		}else {
			if (started) {
				started = false;
				if (allDataLogHistory.size() > 0) saveData();
				lastSent = -1;
				Snackbar.make(root, "Autorelevation is stopped", Snackbar.LENGTH_LONG).show();
			}
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

		allDataLogHistory.clear();
	}

	PowerManager.WakeLock wakeLock;



	@SuppressLint("MissingPermission")
	@Override
	public void onResume() {
		super.onResume();
		setFilters();  // Start listening notifications from UsbService
		startService(UsbService.class, usbConnection, null);
		if(getArguments()!=null){
			modemState=getArguments().getInt("state");
			setModemStateTextView(modemState);
			mLastLocation=getArguments().getParcelable("pos");
			if(mLastLocation!=null) {
				gpsData.invalidate();
			}
			receivedPackages = (getArguments().get("received")!=null) ? getArguments().getInt("received") : 0;
			lostPackages = (getArguments().get("losted")!=null) ? getArguments().getInt("losted") : 0;
			receivedPackagesTv.invalidate();
			lostPackagesTv.invalidate();
		}
		// Start UsbService(if it was not started before) and Bind it

	}

	@Override
	public void onPause() {
		super.onPause();
		Bundle b = new Bundle();
		b.putInt("received",receivedPackages);
		b.putInt("losted",lostPackages);
		b.putInt("state",modemState);
		b.putParcelable("pos",mLastLocation);
		setArguments(b);
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

				case UsbService.MESSAGE_FROM_SERIAL_PORT://0
					String data = (String) msg.obj;
					WiguanaTestActivity wiguanaTestActivity = (WiguanaTestActivity) mActivity.get().getActivity();
					if(wiguanaTestActivity.readData()!=null) {
								String logData = wiguanaTestActivity.readData();
								logData= logData + " " + data;
								wiguanaTestActivity.writeData(logData);
							}
						else{
							wiguanaTestActivity.writeData(data);

						}


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

	private int stateCounter=0;

	private void modemFSM(String msg) {

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
			stateCounter++;
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
					logTextView.append("Package " + packagesCount + " received\n");

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
				Snackbar snackbar = Snackbar.make(root,"Package error  : " + msg,Snackbar.LENGTH_LONG);
				snackbar.getView().setBackgroundColor(Color.RED);
				snackbar.show();
				modemStateTextView.setText("MODEM READY");
				lostPackages++;
				lostPackagesTv.setText(String.valueOf(lostPackages));
				logTextView.append("Error package " + packagesCount + " not received\n");
				modemStateTextView.invalidate();
				modemState = READY;
			}
		}

	}


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








