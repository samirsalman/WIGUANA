package ppl.wiguana.wiguanadatalogger;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
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

import java.util.List;

import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconOffset;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link MapFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link MapFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MapFragment extends Fragment {

	private MapView mapView;
	private BottomNavigationView navigationMenu;

	// TODO: Rename parameter arguments, choose names that match
	// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
	private static final String ARG_PARAM1 = "param1";
	private static final String ARG_PARAM2 = "param2";

	// TODO: Rename and change types of parameters
	private String mParam1;
	private String mParam2;

	private OnFragmentInteractionListener mListener;

	public MapFragment() {
		// Required empty public constructor
	}

	/**
	 * Use this factory method to create a new instance of
	 * this fragment using the provided parameters.
	 *
	 * @param param1 Parameter 1.
	 * @param param2 Parameter 2.
	 * @return A new instance of fragment MapFragment.
	 */
	// TODO: Rename and change types and number of parameters
	public static MapFragment newInstance(String param1, String param2) {
		MapFragment fragment = new MapFragment();
		Bundle args = new Bundle();
		args.putString(ARG_PARAM1, param1);
		args.putString(ARG_PARAM2, param2);
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Mapbox.getInstance(getActivity().getApplicationContext(), "pk.eyJ1Ijoic2FtaXJzYWxtYW4iLCJhIjoiY2p2M3o4aThnMHJ3OTQzbDhrb2ZxN2pzcSJ9.8nG6UJWq8NFqv51HTJ1tUw");

		if (getArguments() != null) {
			mParam1 = getArguments().getString(ARG_PARAM1);
			mParam2 = getArguments().getString(ARG_PARAM2);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		View v = inflater.inflate(R.layout.fragment_map, container, false);
		mapView = v.findViewById(R.id.mapView);
		navigationMenu= v.findViewById(R.id.navigation_view);

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
								getActivity().getResources(), R.drawable.mapbox_marker_icon_default));

						//CREO LA LISTA DELLE FEATURE, OVVERO LE POSIZIONI GPS DEI VARI MARKER
						Feature[] featureList ={Feature.fromGeometry(Point.fromLngLat(-50.679, 41.885)),Feature.fromGeometry(
								Point.fromLngLat(-50.679, 80.885)),Feature.fromGeometry(
									Point.fromLngLat(80.679, 60.885))};

						//AGGIUNGO L'ICONA CHE VORRO' DARE AI MARKER SULLA MAPPA E LE DO UN ID my-marker-image
						style.addImage("my-marker-image", BitmapFactory.decodeResource(
							getActivity().getResources(), R.drawable.mapbox_marker_icon_default));
						//AGGIUNGO LA SORGENTE DEI MARKER, OVVERO LA LISTA CREATA PRIMA E LE DO UN ID marker-source
						style.addSource(new GeoJsonSource("marker-source",
							FeatureCollection.fromFeatures(featureList)));
						//AGGIUNGO IL MARKER VERO E PROPRIO SU MAPPA
						style.addLayer(new SymbolLayer("marker-layer", "marker-source")
							.withProperties(PropertyFactory.iconImage("my-marker-image"),
								iconOffset(new Float[]{0f, -9f})));


						LocationComponent locationComponent = mapboxMap.getLocationComponent();

// Activate with options
						locationComponent.activateLocationComponent(
							LocationComponentActivationOptions.builder(getActivity(), style).build());

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
	public void onDestroy() {
		super.onDestroy();
		mapView.onDestroy();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		mapView.onSaveInstanceState(outState);
	}
}
