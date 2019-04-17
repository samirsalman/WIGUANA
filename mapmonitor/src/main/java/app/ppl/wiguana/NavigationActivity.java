package app.ppl.wiguana;

import android.content.res.AssetManager;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.google.gson.JsonObject;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.style.layers.CircleLayer;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.mapboxsdk.style.sources.Source;
import com.mapbox.mapboxsdk.style.sources.VectorSource;

import static com.mapbox.mapboxsdk.style.expressions.Expression.color;
import static com.mapbox.mapboxsdk.style.expressions.Expression.exponential;
import static com.mapbox.mapboxsdk.style.expressions.Expression.get;
import static com.mapbox.mapboxsdk.style.expressions.Expression.interpolate;
import static com.mapbox.mapboxsdk.style.expressions.Expression.match;
import static com.mapbox.mapboxsdk.style.expressions.Expression.stop;
import static com.mapbox.mapboxsdk.style.expressions.Expression.zoom;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleRadius;

import com.google.gson.JsonObject;


import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class NavigationActivity extends AppCompatActivity {

    private MapView mapView;
    private MapboxMap mapboxMap;


    private List<Point> routeCoordinates;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);


        routeCoordinates = new ArrayList<Point>();
        routeCoordinates.add(Point.fromLngLat(12.625066,41.854384));
        routeCoordinates.add(Point.fromLngLat(12.623285,41.854056));
        mapView = (MapView) findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                NavigationActivity.this.mapboxMap = mapboxMap;
                LineString lineString = LineString.fromLngLats(routeCoordinates);


                Feature [] features = new Feature[2];
                JsonObject jo = new JsonObject();
                jo.addProperty("power","low");
                features[0] = Feature.fromGeometry(Point.fromLngLat(12.625066,41.854384),jo);

                jo = new JsonObject();
                jo.addProperty("power","high");
                features[1] = Feature.fromGeometry(Point.fromLngLat(12.623285,41.854056),jo);
                FeatureCollection featureCollection =
                        FeatureCollection.fromFeatures(features);

                Source geoJsonSource = new GeoJsonSource("line-source", featureCollection);

                mapboxMap.addSource(geoJsonSource);



                CircleLayer circleLayer = new CircleLayer("trees-style", "line-source");
                // replace street-trees-DC-9gvg5l with the name of your source layer
                circleLayer.setSourceLayer("street-trees-DC-9gvg5l");
                circleLayer.withProperties(
                        circleRadius(
                                interpolate(
                                        exponential(1.75f),
                                        zoom(),
                                        stop(12, 2f),
                                        stop(22, 180f)
                                )),
                        circleColor(
                                match(get("power"), color(Color.parseColor("#000000")),
                                        stop("low", color(Color.parseColor("#ff0000"))),
                                        stop("high", color(Color.parseColor("#00ff00"))))));
                mapboxMap.addLayer(circleLayer);



            }
        });
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
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
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

    public  String loadJSONFromAsset(String filename) {
        String json = "";
        try {
            AssetManager assetManager = getAssets();
            InputStream is = assetManager.open(filename);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }
}
