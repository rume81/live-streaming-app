package webhawksit.com.livestreaming.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import webhawksit.com.livestreaming.R;
import webhawksit.com.livestreaming.model.Streams;
import webhawksit.com.livestreaming.utils.PrefManager;

public class StreamsAdapter extends RecyclerView.Adapter<StreamsAdapter.ViewHolder> {

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public ImageView stream_item_image;
        public TextView stream_item_title, stream_item_user_image;

        public ViewHolder(View itemView) {
            super(itemView);
            stream_item_title = (TextView) itemView.findViewById(R.id.stream_item_title);
            stream_item_image = (ImageView) itemView.findViewById(R.id.stream_item_image);
            stream_item_user_image = (TextView) itemView.findViewById(R.id.stream_item_user_image);
        }
    }

    private ArrayList<Streams> _data;
    private Context mContext;

    // Easy access to the context object in the recyclerview
    private Context getContext() {
        return mContext;
    }

    public StreamsAdapter(Context context, ArrayList<Streams> _data) {
        this._data = _data;
        mContext = context;
        for (int i = 0; i < _data.size(); i++) {
            Streams val = _data.get(i);
            if (val.getStreamName().equals(PrefManager.getUserRecentStream(context))) {
                _data.remove(i);
                break;
            }
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View streamView = inflater.inflate(R.layout.stream_item, parent, false);
        ViewHolder viewHolder = new ViewHolder(streamView);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        // Get the data model based on position
        final Streams data = _data.get(position);
        TextView ListTitle = viewHolder.stream_item_title;
        ListTitle.setText(data.getStreamName()); /*"Beach festival with friends"*/

        TextView ListUserName = viewHolder.stream_item_user_image;
        ListUserName.setText(String.valueOf(data.getStreamName().toString().charAt(0)));

        ImageView ListImage = viewHolder.stream_item_image;
        ListImage.setImageResource(R.drawable.mat_wp7);
       // Picasso.with(getContext())
               // .load(data.getImageUrl()).noFade().into(ListImage);
    }

    @Override
    public int getItemCount() {
        return _data.size();
    }

}