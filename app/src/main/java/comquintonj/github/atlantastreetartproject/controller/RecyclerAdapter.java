package comquintonj.github.atlantastreetartproject.controller;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;

import comquintonj.github.atlantastreetartproject.R;
import comquintonj.github.atlantastreetartproject.model.ArtInformation;

/**
 * The adapter for the explore activity for the RecyclerView
 */
public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.MyViewHolder> {

    /**
     * The current context of the application
     */
    final private Context mContext;

    /**
     * The HashMap that stores the path to the image and data
     */
    private HashMap<String, ArtInformation> pathAndDataMap;

    /**
     * ViewHolder for the RecyclerView in the Explore screen
     */
    public class MyViewHolder extends RecyclerView.ViewHolder {
        public ImageView pictureOfArt;
        public TextView artistText;
        public TextView distanceText;

        public MyViewHolder(View view) {
            super(view);
            pictureOfArt = (ImageView) view.findViewById(R.id.artPicture);
            artistText = (TextView) view.findViewById(R.id.arist_card_text);
            distanceText = (TextView) view.findViewById(R.id.distance_card_text);
        }
    }

    /**
     * Constructor for the adapter
     * @param mContext the current context of the application
     * @param pathAndDataMap the HashMap that stores the path to the image and data
     */
    public RecyclerAdapter(Context mContext,
                           HashMap<String, ArtInformation> pathAndDataMap) {
        this.mContext = mContext;
        this.pathAndDataMap = pathAndDataMap;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, final int position) {

        // Initiate Firebase storage reference to retrieve images
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();

        // Get a list of image paths from the key set
        ArrayList<String> imagePaths = new ArrayList<>(pathAndDataMap.keySet());

        // Grab the file paths and store them as StorageReferences
        final ArrayList<StorageReference> images = new ArrayList<>();
        for (String pathName : imagePaths) {
            StorageReference pathReference = storageRef.child(pathName);
            images.add(pathReference);
        }

        // Retrieve images from references
        StorageReference pieceOfArt = images.get(position);
        Glide.with(mContext.getApplicationContext())
                .using(new FirebaseImageLoader())
                .load(pieceOfArt)
                .into(holder.pictureOfArt);

        // Using same position, set proper artist name
        String artistOfArt = pathAndDataMap.get(imagePaths.get(position)).getArtist();
        holder.artistText.setText(artistOfArt);
        double distance = pathAndDataMap.get(imagePaths.get(position)).getDistance();
        if (!(distance == 0.00)) {
            String distanceValue = String.valueOf(String.format("%.2f", distance)) + " mi";
            holder.distanceText.setText(distanceValue);
        }
    }

    @Override
    public int getItemCount() {
        return pathAndDataMap.size();
    }

}