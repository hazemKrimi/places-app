package com.example.places;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {
    FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(final GoogleMap map) {
        db.collection("places")
            .get()
            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            double lat = (double)(document.getData().get("lat"));
                            double lng = (double)(document.getData().get("lng"));
                            String name = (String)(document.getData().get("name"));

                            LatLng marker = new LatLng(lat, lng);
                            Marker place = map.addMarker(new MarkerOptions().position(marker).title(name));
                            map.moveCamera(CameraUpdateFactory.newLatLng(marker));

                            Map<String, Object> data = new HashMap<>();
                            data.put("id", document.getId());
                            data.put("lat", lat);
                            data.put("lng", lng);
                            data.put("name", name);

                            place.setTag(data);
                        }
                    }
                }
            });

        map.setOnMarkerClickListener(this);

        map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(final LatLng point) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

                LayoutInflater inflater = getLayoutInflater();
                final View view = inflater.inflate(R.layout.add_dialog, null);

                Button addButton = view.findViewById(R.id.add);
                Button cancelButton = view.findViewById(R.id.cancel);

                builder.setView(view);

                final AlertDialog dialog = builder.create();
                dialog.show();

                addButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final String name = ((EditText) view.findViewById(R.id.name)).getText().toString();

                        final Map<String, Object> data = new HashMap<>();
                        data.put("lat", point.latitude);
                        data.put("lng", point.longitude);
                        data.put("name", name);

                        db.collection("places")
                            .add(data)
                            .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                @Override
                                public void onSuccess(DocumentReference documentReference) {
                                    LatLng marker = new LatLng(point.latitude, point.longitude);
                                    Marker place = map.addMarker(new MarkerOptions().position(marker).title(name));
                                    map.moveCamera(CameraUpdateFactory.newLatLng(marker));

                                    data.put("id", documentReference.getId());
                                    place.setTag(data);
                                }
                            });

                        dialog.hide();
                    }
                });

                cancelButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.hide();
                    }
                });
            }
        });
    }

    @Override
    public boolean onMarkerClick(final Marker marker) {
        final String id = (String)((HashMap)(marker.getTag())).get("id");
        final double lat = (double)((HashMap)(marker.getTag())).get("lat");
        final double lng = (double)((HashMap)(marker.getTag())).get("lng");
        final String name = (String)((HashMap)(marker.getTag())).get("name");

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

        LayoutInflater inflater = getLayoutInflater();
        final View view = inflater.inflate(R.layout.view_manage_dialog, null);

        ((EditText)(view.findViewById(R.id.name))).setText(name);

        Button editButton = view.findViewById(R.id.edit);
        Button deleteButton = view.findViewById(R.id.delete);
        Button cancelButton = view.findViewById(R.id.cancel);

        builder.setView(view);

        final AlertDialog dialog = builder.create();
        dialog.show();

        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String name = ((EditText) view.findViewById(R.id.name)).getText().toString();

                final Map<String, Object> data = new HashMap<>();
                data.put("lat", lat);
                data.put("lng", lng);
                data.put("name", name);

                DocumentReference ref = db.collection("places").document(id);
                ref.update(data);

                dialog.hide();
            }
        });

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                db.collection("places").document(id).delete();

                marker.remove();
                dialog.hide();
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.hide();
            }
        });

        return true;
    }
}
