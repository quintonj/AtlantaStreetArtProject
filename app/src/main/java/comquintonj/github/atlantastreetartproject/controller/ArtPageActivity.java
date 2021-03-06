package comquintonj.github.atlantastreetartproject.controller;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Objects;

import comquintonj.github.atlantastreetartproject.R;
import comquintonj.github.atlantastreetartproject.model.ArtInformation;
import comquintonj.github.atlantastreetartproject.model.User;

/**
 * The screen for an individual piece of art that the user is taken to after selecting an image
 * on the explore screen.
 */
public class ArtPageActivity extends AppCompatActivity {

    /**
     * An ArtInformation object to store data about the individual piece of art
     */
    private ArtInformation pieceOfArt;

    /**
     * Keeps track of if user has allowed location permission
     */
    private boolean allowed;

    /**
     * Keeps track of whether or not the art is in the user's tour
     */
    private boolean hasInTour = false;

    /**
     * Keeps track of whether or not the art has been upvoted
     */
    private boolean upvoteSet = false;

    /**
     * Keeps track of whether or not the art has been downvoted
     */
    private boolean downvoteSet = false;

    /**
     * The context of the current state of the application
     */
    private Context context;

    /**
     * A reference to the Firebase database to store information about the art
     */
    private DatabaseReference mDatabase;

    /**
     * Current user of the application
     */
    private FirebaseUser user;

    /**
     * The art that has been rated by the user
     */
    private HashMap<String, String> artRated;

    /**
     * The navigate to button
     */
    private ImageView navigateImage;

    /**
     * The add to tour button
     */
    private ImageView tourImage;

    /**
     * Image of the art
     */
    private ImageView imageOfArt;

    /**
     * Image of the flag button
     */
    private ImageView flagImage;

    /**
     * Upvote button to rate art
     */
    private ImageButton upvoteButton;

    /**
     * Downvote button to rate art
     */
    private ImageButton downvoteButton;

    /**
     * The index of the art in the User's tour
     */
    private int tourIndex = 0;

    /**
     * An intent to go back to the explore screen
     */
    private Intent exploreIntent;

    /**
     * The current location of the user
     */
    private Location userLocation;

    /**
     * A LocationListener to keep track of the user's location
     */
    private final LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(final Location location) {
            
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String s) {


        }

        @Override
        public void onProviderDisabled(String s) {
            Toast.makeText(context, "Please enable GPS", Toast.LENGTH_SHORT).show();
        }
    };

    /**
     * A reference to the Firebase storage kept in order to upload images
     */
    private StorageReference mStorage;

    /**
     * The TextView to show the artist of the art
     */
    private TextView artistText;

    /**
     * The TextView to show the submitter of the art
     */
    private TextView submitterText;

    /**
     * The TextView to show the distance to the art
     */
    private TextView distanceText;

    /**
     * The TextView to show the number of downvotes the art has
     */
    private TextView downvoteText;

    /**
     * The TextView to show the number of upvotes the art has
     */
    private TextView upvoteText;

    /**
     * The TextView to add the art to the user's tour
     */
    private TextView tourText;

    /**
     * The TextView that is used to navigate to the art
     */
    private TextView navigateText;

    /**
     * The TextView that is used to flag the art
     */
    private TextView flagText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_art_page);

        // No title in action bar
        setTitle("");

        user = FirebaseAuth.getInstance().getCurrentUser();
        LocationManager mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        allowed = checkLocationPermission();
        if (allowed) {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0,
                    0, mLocationListener);
            userLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }

        // Set up back button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize instance variables
        context = this;
        exploreIntent = new Intent(context, ExploreActivity.class);

        // Initialize view
        imageOfArt = (ImageView) findViewById(R.id.art_image_view);
        artistText = (TextView) findViewById(R.id.art_artist_text);
        submitterText = (TextView) findViewById(R.id.art_submitter_text);
        downvoteText = (TextView) findViewById(R.id.downvote_text);
        upvoteText = (TextView) findViewById(R.id.upvote_text);
        downvoteButton = (ImageButton) findViewById(R.id.downvote_button);
        upvoteButton = (ImageButton) findViewById(R.id.upvote_button);
        distanceText = (TextView) findViewById(R.id.distance_text);
        navigateText = (TextView) findViewById(R.id.navigateText);
        navigateImage = (ImageView) findViewById(R.id.navigateImage);
        tourText = (TextView) findViewById(R.id.tourText);
        tourImage = (ImageView) findViewById(R.id.tourImage);
        flagImage = (ImageView) findViewById(R.id.flagView);
        flagText = (TextView) findViewById(R.id.flagText);
        turnOffDownvote();
        turnOffUpvote();
        setDrawable();

        // Initialize Firebase references
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mStorage = FirebaseStorage.getInstance().getReference();

        // Grab the path of the art that was selected
        final String bundleExtra = getIntent().getExtras().getString("ArtPath");

        // Retrieve the ArtInformation object from Firebase
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (bundleExtra != null) {
                    // Using the path, find the piece of art in the database
                    pieceOfArt = dataSnapshot.child("Art")
                            .child(bundleExtra).getValue(ArtInformation.class);
                    if (pieceOfArt != null) {
                        updateArtView();
                        if (allowed) {
                            updateDistanceView();
                        }
                    } else {
                        startActivity(exploreIntent);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        // Retrieve the User object from Firebase
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (bundleExtra != null) {
                    // Using the path, find the piece of art in the database
                    if (user.getDisplayName() != null) {
                        artRated = (HashMap<String, String>) dataSnapshot.child("Users")
                                .child(user.getDisplayName()).child("rated").getValue();
                        if (pieceOfArt != null) {
                            updateRatingView();
                            addClickListeners();
                        } else {
                            startActivity(exploreIntent);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (getIntent().hasExtra("Submit")) {
                    startActivity(exploreIntent);
                    finish();
                } else {
                    onBackPressed();
                    finish();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Glide.get(context).clearMemory();
    }

    /**
     * Initialize the information about the art
     */
    public void updateArtView() {
        String artist = "Artist: " + pieceOfArt.getArtist();
        String submitter = "Submitter: " + pieceOfArt.getDisplayName();
        // Set the artist TextView
        artistText.setText(artist);
        // Set the submitter TextView
        submitterText.setText(submitter);

        // Set up proper image path and populate the image view
        StorageReference pathReference = mStorage.child("image/"
                + pieceOfArt.getPhotoPath());
        Glide.with(getApplicationContext())
                .using(new FirebaseImageLoader())
                .load(pathReference)
                .into(imageOfArt);
        for (int i = 0; i < User.tourArt.size(); i++) {
            ArtInformation current = User.tourArt.get(i);
            if (Objects.equals(pieceOfArt.getPhotoPath(), current.getPhotoPath())) {
                // The art is already in the user's tour and should be removed
                hasInTour = true;
                tourIndex = i;
                String remove = "Remove from tour";
                tourText.setText(remove);
            }
        }
    }

    /**
     * Update the view that shows the distance away from the user
     */
    public void updateDistanceView() {
        Location artLocation = new Location("");
        artLocation.setLatitude(pieceOfArt.getLatitude());
        artLocation.setLongitude(pieceOfArt.getLongitude());
        if (userLocation != null) {
            double distanceInMeters = userLocation.distanceTo(artLocation);
            double distanceInMiles = distanceInMeters / 1609.344;
            String distanceValue = String.valueOf(String.format("%.2f", distanceInMiles)) + " mi";
            distanceText.setText(distanceValue);
        }
    }

    /**
     * Update the ratings of the art
     */
    private void updateRatingView() {
        upvoteText.setText(String.valueOf(pieceOfArt.getRatingUpvotes()));
        downvoteText.setText(String.valueOf(pieceOfArt.getRatingDownvotes()));
        if (artRated != null) {
            // Check to see if user has already rated this piece of art
            if (artRated.containsKey(pieceOfArt.getPhotoPath())) {
                String rated = (String) artRated.get(pieceOfArt.getPhotoPath());
                switch (rated) {
                    case "Upvoted":
                        // If the user has already upvoted the art, highlight the upvote button
                        turnOffDownvote();
                        setUpvoteButton();
                        upvoteSet = true;
                        break;
                    case "Downvoted":
                        // If the user has already downvoted the art, highlight the downvote button
                        turnOffUpvote();
                        setDownvoteButton();
                        downvoteSet = true;
                        break;
                    default:
                        // The user hasn't rated the art yet
                        downvoteSet = false;
                        upvoteSet = false;
                        turnOffDownvote();
                        turnOffUpvote();
                        break;
                }
            }
        }
    }

    /**
     * Add click listeners to the rating buttons
     */
    private void addClickListeners() {
        // User has decided to give the art a downvote
        downvoteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (user.getDisplayName() != null) {
                    String setting = "Downvoted";
                    if (downvoteSet) {
                        downvoteSet = false;
                        turnOffDownvote();
                        pieceOfArt.decDownvote();
                        setting = "";
                    } else if (upvoteSet) {
                        // The user has already upvoted this art
                        upvoteSet = false;
                        turnOffUpvote();
                        setDownvoteButton();
                        pieceOfArt.decUpvote();
                        pieceOfArt.incDownvote();
                        downvoteSet = true;
                    } else {
                        // The user hasn't voted on this art
                        downvoteSet = true;
                        setDownvoteButton();
                        pieceOfArt.incDownvote();
                    }

                    // Store changes in Firebase database
                    mDatabase.child("Users").child(user.getDisplayName())
                            .child("rated").child(pieceOfArt.getPhotoPath()).setValue(setting);
                    mDatabase.child("Art").child(pieceOfArt.getPhotoPath()).setValue(pieceOfArt);

                    // Update TextView to reflect increased downvote
                    downvoteText.setText(String.valueOf(pieceOfArt.getRatingDownvotes()));
                }
            }
        });

        // User has decided to give the art an upvote
        upvoteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (user.getDisplayName() != null) {
                    String setting = "Upvoted";
                    if (upvoteSet) {
                        upvoteSet = false;
                        turnOffUpvote();
                        pieceOfArt.decUpvote();
                        setting = "";
                    } else if (downvoteSet) {
                        // The user has already downvoted this art
                        downvoteSet = false;
                        turnOffDownvote();
                        setUpvoteButton();
                        pieceOfArt.decDownvote();
                        pieceOfArt.incUpvote();
                        upvoteSet = true;
                    } else {
                        // The user hasn't voted on this art
                        upvoteSet = true;
                        setUpvoteButton();
                        pieceOfArt.incUpvote();
                    }

                    // Store changes in Firebase database
                    mDatabase.child("Users").child(user.getDisplayName())
                            .child("rated").child(pieceOfArt.getPhotoPath()).setValue(setting);
                    mDatabase.child("Art").child(pieceOfArt.getPhotoPath()).setValue(pieceOfArt);

                    // Update TextView to reflect increased upvote
                    upvoteText.setText(String.valueOf(pieceOfArt.getRatingUpvotes()));
                }
            }
        });

        // User has decided to navigate to the art
        navigateText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (userLocation != null) {
                    String url = "http://maps.google.com/maps?saddr="
                            + "My+Location"
                            + "&daddr="
                            + pieceOfArt.getLatitude() + "," + pieceOfArt.getLongitude()
                            + "&mode=walking";
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    context.startActivity(intent);
                } else {
                    Toast.makeText(context, "Location could not be found, please try again later",
                            Toast.LENGTH_SHORT).show();
                }

            }
        });

        // User has decided to add the art to their tour
        tourText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (hasInTour) {
                    hasInTour = false;
                    User.tourArt.remove(tourIndex);
                    Toast.makeText(context, "Removed from tour", Toast.LENGTH_SHORT).show();
                    String add = "Add to tour";
                    tourText.setText(add);
                } else {
                    hasInTour = true;
                    // The art is not in the user's tour
                    if (!(User.tourArt.size() <= 8)) {
                        Toast.makeText(context, "You have reached" +
                                "the maximum amount of tour stops", Toast.LENGTH_SHORT).show();
                    } else {
                        // Add the piece of art to the tour
                        User.tourArt.add(pieceOfArt);
                        String remove = "Remove from tour";
                        tourText.setText(remove);
                        Toast.makeText(context, "Added to tour", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        flagText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Open a dialog for choosing which criteria to sort by
                CharSequence options[] = new CharSequence[] {"Inappropriate", "Not street art",
                        "Other reason"};

                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Report Art:");
                builder.setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            // Inappropriate art
                            sendFeedback("Inappropriate art");
                        } else if (which == 1) {
                            // Not street art
                            sendFeedback("Not street art");
                        } else if (which == 2) {
                            // Other reason
                            sendFeedback("Other reason");
                        }
                    }
                });
                builder.show();
            }
        });
    }

    /**
     * Opens an email with the given feedback entered
     * @param typeOfFeedback the type of feedback the user chose to report
     */
    public void sendFeedback(String typeOfFeedback) {
        Intent Email = new Intent(Intent.ACTION_SEND);
        Email.setType("message/rfc822");
        Email.putExtra(Intent.EXTRA_EMAIL, new String[] { "theatlantastreetartproject@gmail.com" });
        Email.putExtra(Intent.EXTRA_SUBJECT, "Report Art");
        String bodyText = "Reasoning: " + typeOfFeedback + "\n\n"
                + "Art Title: " + pieceOfArt.getTitle() + "\n\n"
                + "Art ID: " + pieceOfArt.getPhotoPath() + "\n\n"
                + "Additional information: ";
        Email.putExtra(Intent.EXTRA_TEXT, bodyText);
        startActivity(Intent.createChooser(Email, "Send Feedback:"));
    }

    /**
     * Denote an upvote by coloring the button the accent color
     */
    public void setUpvoteButton() {
        DrawableCompat.setTint(upvoteButton.getDrawable(),
                ContextCompat.getColor(context, R.color.colorAccent));
    }

    /**
     * Denote an downvote by coloring the button the accent color
     */
    public void setDownvoteButton() {
        DrawableCompat.setTint(downvoteButton.getDrawable(),
                ContextCompat.getColor(context, R.color.colorAccent));
    }

    /**
     * Change the color of the upvote button back to black
     */
    public void turnOffUpvote() {
        DrawableCompat.setTint(upvoteButton.getDrawable(),
                ContextCompat.getColor(context, R.color.Theme3));
    }

    /**
     * Change the color of the downvote button back to black
     */
    public void turnOffDownvote() {
        DrawableCompat.setTint(downvoteButton.getDrawable(),
                ContextCompat.getColor(context, R.color.Theme3));
    }

    /**
     * Changes the color of the drawables
     */
    public void setDrawable() {
        DrawableCompat.setTint(navigateImage.getDrawable(),
                ContextCompat.getColor(context, R.color.colorAccent));
        DrawableCompat.setTint(tourImage.getDrawable(),
                ContextCompat.getColor(context, R.color.colorAccent));
        DrawableCompat.setTint(flagImage.getDrawable(),
                ContextCompat.getColor(context, R.color.colorAccent));
    }

    /**
     * Check to see if the user has given permission to read external storage
     * @return whether or not the user has given permission
     */
    public boolean checkLocationPermission() {
        int result = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        if (result == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }
}

