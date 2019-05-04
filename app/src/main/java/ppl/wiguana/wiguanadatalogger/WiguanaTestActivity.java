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
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
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

public class WiguanaTestActivity extends AppCompatActivity implements MainFragment.OnFragmentInteractionListener, MapFragment.OnFragmentInteractionListener, LogFragment.OnFragmentInteractionListener {


	BottomNavigationView bottomMenu;
	FragmentManager fm;
	MainFragment home;
	LocationManager locationManager;
	MapFragment map;
	LogFragment log;
	FragmentTransaction ft;



	private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
		= new BottomNavigationView.OnNavigationItemSelectedListener() {

		@Override
		public boolean onNavigationItemSelected(@NonNull MenuItem item) {
			switch (item.getItemId()) {
				case R.id.homepage:

					fm = getSupportFragmentManager();
					ft = fm.beginTransaction();
					ft.replace(R.id.main_frame,home);
					ft.addToBackStack("home");
					ft.commit();
					return true;

				case R.id.map_menu:
					fm = getSupportFragmentManager();
					ft = fm.beginTransaction();
					ft.replace(R.id.main_frame,map);
					ft.addToBackStack("map");
					ft.commit();
					return true;

				case R.id.measures:
					if(home.getArguments()!=null) {
						log.setArguments(home.getArguments());
						System.out.println(home.getArguments());
					}
					fm = getSupportFragmentManager();
					ft = fm.beginTransaction();
					ft.replace(R.id.main_frame,log);
					ft.addToBackStack("log");
					ft.commit();
					return true;

			}
			return false;
		}
	};




	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_wiguana_test);
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		bottomMenu= findViewById(R.id.bottomNavigationView);
		home = MainFragment.newInstance("p1","p2");
		map= MapFragment.newInstance("p1","p2");
		log = LogFragment.newInstance("a","b");
		fm = getSupportFragmentManager();


		bottomMenu.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);


	}



	@SuppressLint("MissingPermission")
	@Override
	protected void onStart() {
		super.onStart();
		checkLocationPermission();
		PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
		//Lock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
			//"MyApp::MyWakelockTag");
		//wakeLock.acquire();
	}



	SharedPreferences.Editor putDouble(final SharedPreferences.Editor edit, final String key, final double value) {
		return edit.putLong(key, Double.doubleToRawLongBits(value));
	}

	double getDouble(final SharedPreferences prefs, final String key, final double defaultValue) {
		return Double.longBitsToDouble(prefs.getLong(key, Double.doubleToLongBits(defaultValue)));
	}


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

								if(ft==null) {
									ft = fm.beginTransaction();
									ft.add(R.id.main_frame, home);
									ft.commit();
								}
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


	private LocationListener locationListener = new LocationListener() {
		public void onLocationChanged(Location location) {
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {}

		public void onProviderEnabled(String provider) {}

		public void onProviderDisabled(String provider) {}
	};


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

	@Override
	public void onFragmentInteraction(Uri uri) {

	}
}


