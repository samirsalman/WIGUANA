package ppl.wiguana.wiguanadatalogger;

import android.content.Intent;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.NavigationView;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

public class Map extends AppCompatActivity {

	private MapView mapView;
	private BottomNavigationView navigationMenu;



	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Mapbox.getInstance(this, "pk.eyJ1Ijoic2FtaXJzYWxtYW4iLCJhIjoiY2p2M3o4aThnMHJ3OTQzbDhrb2ZxN2pzcSJ9.8nG6UJWq8NFqv51HTJ1tUw");
		setContentView(R.layout.activity_map);
		mapView = findViewById(R.id.mapView);
		navigationMenu= findViewById(R.id.navigation_view);
		Menu menu = navigationMenu.getMenu();
		MenuItem menuItem = menu.getItem(1);
		menuItem.setChecked(true);

		navigationMenu.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
			@Override
			public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
				switch(menuItem.getItemId()){
					case R.id.homepage:
						onBackPressed();
						break;

					default:
						return true;

				}
				return false;
			}
		});


		mapView.onCreate(savedInstanceState);
		mapView.getMapAsync(new OnMapReadyCallback() {
			@Override
			public void onMapReady(@NonNull final MapboxMap mapboxMap) {

				mapboxMap.setStyle(Style.LIGHT, new Style.OnStyleLoaded() {
					@Override
					public void onStyleLoaded(@NonNull Style style) {
// Add the marker image to map
						style.addImage("marker-icon-id",
							BitmapFactory.decodeResource(
								Map.this.getResources(), R.drawable.mapbox_marker_icon_default));

						GeoJsonSource geoJsonSource = new GeoJsonSource("source-id", Feature.fromGeometry(
							Point.fromLngLat(-87.679, 41.885)));
						style.addSource(geoJsonSource);

						SymbolLayer symbolLayer = new SymbolLayer("layer-id", "source-id");
						symbolLayer.withProperties(
							PropertyFactory.iconImage("marker-icon-id")
						);
						style.addLayer(symbolLayer);
						LocationComponent locationComponent = mapboxMap.getLocationComponent();

// Activate with options
						locationComponent.activateLocationComponent(
							LocationComponentActivationOptions.builder(Map.this, style).build());

// Enable to make component visible
						locationComponent.setLocationComponentEnabled(true);

// Set the component's camera mode
						locationComponent.setCameraMode(CameraMode.TRACKING);

// Set the component's render mode
						locationComponent.setRenderMode(RenderMode.COMPASS);


					}
				});
			}
		});
	}

	// Add the mapView's own lifecycle methods to the activity's lifecycle methods
	@Override
	public void onStart() {
		super.onStart();
		mapView.onStart();
	}

	@Override
	public void onResume() {
		super.onResume();
		mapView.onResume();
	}

	@Override
	public void onPause() {
		super.onPause();
		mapView.onPause();
	}

	@Override
	public void onStop() {
		super.onStop();
		mapView.onStop();
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		mapView.onLowMemory();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mapView.onDestroy();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		mapView.onSaveInstanceState(outState);
	}

}
