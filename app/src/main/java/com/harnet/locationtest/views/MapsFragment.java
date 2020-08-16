package com.harnet.locationtest.views;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.harnet.locationtest.R;
import com.harnet.locationtest.models.Place;
import com.harnet.locationtest.models.UserCoords;
import com.harnet.locationtest.repositories.PlacesRepository;
import com.harnet.locationtest.viewmodels.LocationMapsActivityViewModel;

import java.util.List;

public class MapsFragment extends Fragment {
    private String name = "maps";

    private GoogleMap mMap;
    private Marker userMarker;
    private Marker placeMarker;

    private LocationMapsActivityViewModel mLocationMapsActivityViewModel;
    OnMessageSendListener onMessageSendListener;

    // interface for exchanging data between fragments
    public interface OnMessageSendListener{
        public void onMessageSend(String message);
    }

    public MapsFragment(){
    }

    public String getName() {
        return name;
    }

    public LocationMapsActivityViewModel getmLocationMapsActivityViewModel() {
        return mLocationMapsActivityViewModel;
    }

    private OnMapReadyCallback callback = new OnMapReadyCallback() {

        @Override
        public void onMapReady(GoogleMap googleMap) {
            mMap = googleMap;
            // set initial user previous coordinates
            if(mLocationMapsActivityViewModel != null && mLocationMapsActivityViewModel.getmPersons().getValue().size() > 0){

                // shows user position
                LatLng userCoords = new LatLng(mLocationMapsActivityViewModel.getmPersons().getValue().get(0).getLat(), mLocationMapsActivityViewModel.getmPersons().getValue().get(0).getLng());
                if (userMarker == null) {
                    MarkerOptions options = new MarkerOptions().position(userCoords)
                            .title("User")
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.user_marker));
                    userMarker = mMap.addMarker(options);
                }

                // shows places from places list on Google map
                List<Place> lastPlaces = PlacesRepository.getInstance().getUsersDataSet().getValue();
                LatLng placeCoords = null;
                if(lastPlaces != null && lastPlaces.size() > 0){
//                    Log.i("Places", "onMapReady: " + lastPlaces);
                    LatLng lastAddedPlace = null;
                    for(Place place : lastPlaces){
                        placeCoords = new LatLng(place.getLat(), place.getLng());
                        placeMarker = googleMap.addMarker(new MarkerOptions().position(placeCoords).title(place.getName()));
                        lastAddedPlace = placeCoords;
                    }
                        // focus camera on the last added place
                        mMap.moveCamera(CameraUpdateFactory.newLatLng(lastAddedPlace));
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastAddedPlace, 12));
                }
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        //MVVM observer
        mLocationMapsActivityViewModel = new ViewModelProvider(this).get(LocationMapsActivityViewModel.class);
        mLocationMapsActivityViewModel.init(getContext(), getActivity());
        mLocationMapsActivityViewModel.getmPersons().observe(getActivity(), new Observer<List<UserCoords>>() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onChanged(List<UserCoords> coords) {
                if (coords != null && coords.size() > 0) {
                    Log.i("TestLoc:", "Coordinates on a map were changed" + coords.get(0).getLat() + ":" + coords.get(0).getLng());
                        //update user position on a map
                    if(userMarker != null && mMap != null){
                        LatLng userCoords = new LatLng(coords.get(0).getLat(), coords.get(0).getLng());
                        userMarker.setPosition(userCoords);

                        mMap.moveCamera(CameraUpdateFactory.newLatLng(userCoords));
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userCoords, 12));
                    }
                }
            }
        });

        return inflater.inflate(R.layout.fragment_maps, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(callback);
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        Activity activity = (Activity) context;
        try {
            onMessageSendListener = (OnMessageSendListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()+ "must implemented onMessageSend");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mLocationMapsActivityViewModel.getLocationService().getLocationManager().removeUpdates(mLocationMapsActivityViewModel.getLocationService().getLocationListener());
    }
}