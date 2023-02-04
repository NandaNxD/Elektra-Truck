package com.elektra.elektraadmin;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapLongClickListener {
    int count = 0;
    FrameLayout map;
    LatLng center;
    GoogleMap gMap;
    Location currentLocation;
    Marker marker;
    FusedLocationProviderClient fusedClient;
    private static final int REQUEST_CODE = 101;
    LinearLayout radiusLinearLayout;
    SearchView searchView;
    TextView information;
    ImageButton resetCenter;
    Button genCircle;
    EditText radiusEditText;
    Button addLimit;
    Double liveLatitude=-1.0, liveLongitude=-1.0;
    BitmapDescriptor liveTruckMarker;

    Marker mrefLiveTruck;

    public void showTruckCameraAnimate(View view){
        if(mrefLiveTruck!=null){
            double latitude = mrefLiveTruck.getPosition().latitude;
            double longitude=mrefLiveTruck.getPosition().longitude;
            gMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(latitude,longitude)));
        }
        else{
            Toast.makeText(getApplicationContext(),"Truck Location Unknown",Toast.LENGTH_SHORT).show();
        }

    }


    //------------------------------------------------------------------------------------------------------
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference topRef = database.getReference("elektra");
    DatabaseReference liveLocationRef = database.getReference("elektra/GeoData");

    void readFirebase(){

        liveLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                List<Double> list=(List<Double>)snapshot.getValue();;

                if(list!=null) {
                    if(mrefLiveTruck!=null){
                        mrefLiveTruck.remove();
                    }
                    liveLatitude = list.get(0);
                    liveLongitude = list.get(1);
                    information.setText(list.toString());
                    mrefLiveTruck=gMap.addMarker(new MarkerOptions().position(new LatLng(liveLatitude,liveLongitude)).icon(liveTruckMarker));

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getApplicationContext(),"Live Truck Location Stopped",Toast.LENGTH_SHORT).show();
            }
        });
    }
    void pushLimitsFirebase(double lat,double lon, double rad){
        List<Double> roadLimitArray=new ArrayList<>();
        roadLimitArray.add(lat);
        roadLimitArray.add(lon);
        roadLimitArray.add(rad);
        database.getReference("elektra/RoadLimits").setValue(roadLimitArray).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                Toast.makeText(getApplicationContext(),"Truck Limit Uploaded",Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getApplicationContext(),"Truck Limit Upload Failed",Toast.LENGTH_SHORT).show();
            }
        });
    }



    //--------------------------------------------------------------------------------------------------------



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        Intent intent=getIntent();

        liveTruckMarker=BitmapDescriptorFactory.fromBitmap(resizeMapIcons("daimler",100,100));

        map = findViewById(R.id.map);
        searchView = findViewById(R.id.search);
        searchView.clearFocus();
        information = findViewById(R.id.information);
        resetCenter = findViewById(R.id.resetCenter);
        genCircle=findViewById(R.id.genCircle);
        radiusEditText=findViewById(R.id.radiusEditText);
        radiusLinearLayout=findViewById(R.id.radiusLinearLayout);
        radiusLinearLayout.setVisibility(View.INVISIBLE);
        addLimit=findViewById(R.id.addLimitButton);
        addLimit.setVisibility(View.INVISIBLE);

        readFirebase();


        resetCenter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gMap.clear();
                information.setText("Long press to Add Center");
                count = 0;
                radiusLinearLayout.setVisibility(View.INVISIBLE);
                addLimit.setVisibility(View.INVISIBLE);
            }
        });

        genCircle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(radiusEditText.getText().toString().isEmpty()){
                    Toast.makeText(getApplicationContext(),"Empty Radius Field",Toast.LENGTH_SHORT).show();
                    return;
                }
                gMap.clear();
                MarkerOptions markerOptions = new MarkerOptions().position(center).title("Center");
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));

                gMap.addMarker(markerOptions);

                generateCircle(center,1000*Double.parseDouble(radiusEditText.getText().toString()));
                Toast.makeText(getApplicationContext(),"Boundary limit added for the truck",Toast.LENGTH_SHORT).show();

            }
        });

        addLimit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(),"Truck Limits Added",Toast.LENGTH_SHORT).show();
                // Firebase push limits
                pushLimitsFirebase(center.latitude,center.longitude,Double.parseDouble(radiusEditText.getText().toString()));
            }
        });



        fusedClient = LocationServices.getFusedLocationProviderClient(this);
        getLocation();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                String loc = searchView.getQuery().toString();
                if (loc == null) {
                    Toast.makeText(MapsActivity.this, "Location Not Found", Toast.LENGTH_SHORT).show();
                } else {
                    Geocoder geocoder = new Geocoder(MapsActivity.this, Locale.getDefault());
                    try {
                        List<Address> addressList = geocoder.getFromLocationName(loc, 1);
                        if (addressList.size() > 0) {
                            LatLng latLng = new LatLng(addressList.get(0).getLatitude(), addressList.get(0).getLongitude());
                            if (marker != null) {
                                marker.remove();
                            }
                            MarkerOptions markerOptions = new MarkerOptions().position(latLng).title(loc);
                            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));

                            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 17);
                            gMap.animateCamera(cameraUpdate);
                            marker = gMap.addMarker(markerOptions);


                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
    }

    public void generateCircle(LatLng center, double rad) {

        int strokeColor = Color.BLUE; // Outline color of the circle
        int fillColor = Color.CYAN; // Fill color of the circle


        CircleOptions circleOptions = new CircleOptions()
                .center(center)
                .radius(rad)
                .strokeColor(strokeColor);

        gMap.addCircle(circleOptions);
    }

    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);
            return;
        }

        Task<Location> task = fusedClient.getLastLocation();

        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {

                if (location != null) {
                    currentLocation = location;
                    //Toast.makeText(getApplicationContext(), currentLocation.getLatitude() + "" + currentLocation.getLongitude(), Toast.LENGTH_SHORT).show();
                    SupportMapFragment supportMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
                    assert supportMapFragment != null;
                    supportMapFragment.getMapAsync(MapsActivity.this);

                }

            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.gMap = googleMap;
        LatLng latLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions().position(latLng).title("My Current Location");
        googleMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17));
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        gMap.setMyLocationEnabled(true);

        gMap.setOnMapLongClickListener(this);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocation();
            }
        }
    }
    double distance(LatLng latlng1,LatLng latlng2)
    {
        double x1=latlng1.latitude;
        double y1=latlng1.longitude;
        double x2=latlng2.latitude;
        double y2=latlng2.longitude;
        double lon1 = Math.toRadians(y1);
        double lon2 = Math.toRadians(y2);
        double lat1 = Math.toRadians(x1);
        double lat2 = Math.toRadians(x2);

        // Haversine formula
        double dlon = lon2 - lon1;
        double dlat = lat2 - lat1;
        double a = Math.pow(Math.sin(dlat / 2), 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.pow(Math.sin(dlon / 2),2);

        double c = 2 * Math.asin(Math.sqrt(a));

        // Radius of earth in kilometers. Use 3956
        // for miles
        double r = 6371;

        // calculate the result
        return(c * r)*1000;
    }
    public Bitmap resizeMapIcons(String iconName,int width, int height){
        Bitmap imageBitmap = BitmapFactory.decodeResource(getResources(),getResources().getIdentifier(iconName, "drawable", getPackageName()));
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(imageBitmap, width, height, false);
        return resizedBitmap;
    }

    @Override
    public void onMapLongClick(@NonNull LatLng latLng) {
        if(count==0){
            MarkerOptions markerOptions = new MarkerOptions().position(latLng).title("Center");
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));


            gMap.addMarker(markerOptions);
            center=latLng;
            information.setText("Long press to Add Boundary");
            count++;
            radiusLinearLayout.setVisibility(View.VISIBLE);
        }
        else if(count>=1){
            gMap.clear();
            count++;
            addLimit.setVisibility(View.VISIBLE);
            MarkerOptions markerOptions = new MarkerOptions().position(center).title("Center");
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));

            gMap.addMarker(markerOptions);

            gMap.addMarker(new MarkerOptions().position(latLng).title("Marker").icon(liveTruckMarker));

            double centerBoundaryDistance=distance(center,latLng);
            radiusEditText.setText(String.valueOf(centerBoundaryDistance/1000));
            generateCircle(center,centerBoundaryDistance);

            //Toast.makeText(getApplicationContext(),"Submit Boundary Limit",Toast.LENGTH_SHORT).show();
        }


    }


}