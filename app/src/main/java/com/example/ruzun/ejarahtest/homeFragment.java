package com.example.ruzun.ejarahtest;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.nabinbhandari.android.permissions.PermissionHandler;
import com.nabinbhandari.android.permissions.Permissions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;


public class homeFragment extends Fragment {

    ImageView createPost;
    FirebaseAuth mFirebaseAuth;
    FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
    DatabaseReference databaseReference = firebaseDatabase.getReference();
    final ArrayList<Post> posts = new ArrayList<Post>();
    ListView listView;
    Double lat, lng;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private String userId;
    String email;
    private FirebaseAuth.AuthStateListener mAuthStateListener;

    TextView userNameMenu, userEmailMenu;
    String currentUserEmail;
    UserLocation userCurrentLocation;

    Map<String, Integer> UserTotalPoints = new HashMap<>();

    User currentUser;
    SwipeRefreshLayout swipeRefreshLayout;
    boolean remove;
    PostAdapter<Post> adapter;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
       View view  = inflater.inflate(R.layout.fragment_home,container,false);

       setUpFirebaseListener();

       PostActivity.isPoster=false;
        mFirebaseAuth = FirebaseAuth.getInstance();

        createPost = view.findViewById(R.id.createPost);
        email = mFirebaseAuth.getCurrentUser().getEmail();
        swipeRefreshLayout=view.findViewById(R.id.swipeRefreshLayout);

        createPost.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), postpage.class));
            }
        });

        databaseReference.child("userLocation").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()){
                    if (mFirebaseAuth.getUid().equals(snapshot.getKey()))
                    {
                        userCurrentLocation = snapshot.getValue(UserLocation.class);

                    }
                }

            }



            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        databaseReference.child("User").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()){
                    User user = snapshot.getValue(User.class);
                    UserTotalPoints.put(user.getEmail(), user.getPoints());

                    if(user.getEmail()!=null&&email!=null)
                        if (user.getEmail().toLowerCase().equals(email.toLowerCase()))
                        {
                            currentUser = user;
                        }
                }

            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


        databaseReference.child("Post").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                posts.removeAll(posts);
                for (DataSnapshot snapshot : dataSnapshot.getChildren()){
                    Post post = snapshot.getValue(Post.class);
                    Location postLocation = new Location("");
                    Location userLocation = new Location("");

                    if(post.getLocation()!=null){

                        postLocation.setLatitude(post.getLocation().get(0));
                        postLocation.setLongitude(post.getLocation().get(1));

                        userLocation.setLatitude(userCurrentLocation.getL().get(0));
                        userLocation.setLongitude(userCurrentLocation.getL().get(1));
                    }

                    if (userLocation.distanceTo(postLocation)<=1000)
                    {
                        if(UserTotalPoints.get(post.getUsername())!=null){
                            post.setPoints(UserTotalPoints.get(post.getUsername()));
                        }
                        posts.add(post);
                        dispaly();

                    }

                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getActivity().recreate();
              
            }
        });



        //to get the email of the current user (treating email as it is the user ID)


        callPermissions();




        listView = (ListView) view.findViewById(R.id.post_list);



        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Post post = posts.get(position);

                Log.i("EEE", "home to post");
                Intent i = new Intent(getActivity(),PostActivity.class);
                i.putExtra("CONTENT", post.getContent());
                i.putExtra("NAME", post.getName());
                i.putExtra("POST_ID", post.getPostID());
                i.putExtra("EMAIL", post.getUsername());
                i.putExtra("CAT", post.getCatogry());
                i.putExtra("VIEWS", post.getViews()+1);
                startActivity(i);
            }
        });

        return view ;
    }

    private void setAdapter(PostAdapter<Post> adapter) {
        this.adapter=adapter;
    }
    private PostAdapter<Post> getAdapter(){
        return adapter;
    }

    private void updateTimeline(PostAdapter<Post> adapter) {
            adapter.notifyDataSetChanged();
    }


    public void callPermissions(){
        String[] permissions = {Manifest.permission.ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION};
        Permissions.check(getActivity(), permissions, "Location is required to use Ejarah",
                new Permissions.Options().setSettingsDialogMessage("Warning").setRationaleDialogTitle("Info"), new PermissionHandler() {
            @Override
            public void onGranted() {
                requestLocationUpdates();
            }

            @Override
            public void onDenied(Context context, ArrayList<String> deniedPermissions) {
                super.onDenied(context, deniedPermissions);
                callPermissions();
            }
        });
    }

    public void requestLocationUpdates(){
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(getActivity());

        locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(10000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        fusedLocationClient.requestLocationUpdates(locationRequest,new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);

                lat=locationResult.getLastLocation().getLatitude();
                lng=locationResult.getLastLocation().getLongitude();
                setUserLocation(lat,lng);
            }
        }, getActivity().getMainLooper());
    }


    public void setUserLocation(Double lat, Double lng){
       try{
           userId= FirebaseAuth.getInstance().getCurrentUser().getUid();
           databaseReference= FirebaseDatabase.getInstance().getReference("userLocation");
           GeoFire geoFire=new GeoFire(databaseReference);
           geoFire.setLocation(userId, new GeoLocation(lat, lng));
       }
       catch (Exception e){
           e.printStackTrace();
       }

    }

    void dispaly(){
        Collections.reverse(posts);
        PostAdapter<Post> adapter = new PostAdapter<Post>(getContext(),posts);
        listView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        notifyOutOfRange(adapter);

    }

    void notifyOutOfRange(PostAdapter<Post> adapter){
        adapter.notifyDataSetChanged();
    }
    public void onStart(){
        super.onStart();

        FirebaseAuth.getInstance().addAuthStateListener(mAuthStateListener);
    }

    //----------------------------------------------------------------------
    @Override
    public void onStop(){
        super.onStop();
        if(mAuthStateListener!=null)
            FirebaseAuth.getInstance().removeAuthStateListener(mAuthStateListener);
    }

    //----------------------------------------------------------------------
    private void setUpFirebaseListener(){
        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user == null ){
                    Intent intent = new Intent(getActivity(), LogIn.class);
                    startActivity(intent);
                }


            }
        };
    }


}

