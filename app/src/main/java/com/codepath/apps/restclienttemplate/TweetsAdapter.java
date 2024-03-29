package com.codepath.apps.restclienttemplate;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.codepath.apps.restclienttemplate.models.Tweet;
import com.codepath.asynchttpclient.callback.JsonHttpResponseHandler;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.parceler.Parcels;

import java.sql.Time;
import java.util.List;

import jp.wasabeef.glide.transformations.RoundedCornersTransformation;
import okhttp3.Headers;

import static android.app.Activity.RESULT_OK;
import static com.codepath.apps.restclienttemplate.TimelineActivity.REQUEST_CODE;

public class TweetsAdapter extends RecyclerView.Adapter<TweetsAdapter.ViewHolder>{

    Context context;
    List<Tweet> tweets;
    public static final String TAG = "TweetsAdapter";
    public static final String USER_NAME_TAG = "user_name";

    // // Define listener member variable
    private OnItemClickListener listener;

    // Define the listener interface
    public interface OnItemClickListener {
        void onItemClick(View itemView, int position);
    }

    // Define the method that allows the parent activity or fragment to define the listener
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    // Pass in the context and list of tweets
    public TweetsAdapter(Context context, List<Tweet> tweets) {
        this.context = context;
        this.tweets = tweets;
    }

    // For each row, inflate a layout for a tweet
    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_tweet, parent, false);
        return new ViewHolder(view);
    }

    // Bind values based on the position of the element
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // Get the data at position
        Tweet tweet = tweets.get(position);
        // Bind the tweet with the view holder
        holder.bind(tweet);
    }

    // Return the number of items in the list
    @Override
    public int getItemCount() {
        return tweets.size();
    }

    // Clean all elements of the recycler
    public void clear() {
        tweets.clear();
        notifyDataSetChanged();
    }

    // Add a list of items -- change to type used
    public void addAll(List<Tweet> list) {
        tweets.addAll(list);
        notifyDataSetChanged();
    }

    // Define a viewholder
    public class ViewHolder extends RecyclerView.ViewHolder {

        // Create member variables for all of the views
        ImageView ivProfileImage;
        TextView tvBody;
        TextView tvScreenName;
        TextView tvTimeStamp;
        ListView lvListImage;
        ImageAdapter adapter;
        ImageButton btnReply;
        TextView tvUserName;
        TwitterClient client;
        ImageButton ibRetweet;
        ImageButton ibLike;
        TextView retweetCount;
        TextView favoriteCount;
        Boolean liked;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Get references to every view
            ivProfileImage = itemView.findViewById(R.id.ivProfileImage);
            tvBody = itemView.findViewById(R.id.tvBody);
            tvScreenName = itemView.findViewById(R.id.tvScreenName);
            tvTimeStamp = itemView.findViewById(R.id.tvTimeStamp);
            lvListImage = itemView.findViewById(R.id.lvListImage);
            btnReply = itemView.findViewById(R.id.ibReply);
            tvUserName = itemView.findViewById(R.id.tvUsername);
            client = TwitterApp.getRestClient(context);
            ibRetweet = itemView.findViewById(R.id.ibRetweet);
            ibLike = itemView.findViewById(R.id.ibLike);
            retweetCount = itemView.findViewById(R.id.tvRetweets);
            favoriteCount = itemView.findViewById(R.id.tvLikes);
            liked = false;

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //clickListener.onItemClick(itemView, getAdapterPosition());
                    int position = getAdapterPosition();
                    Intent i = new Intent(context, DetailsActivity.class);
                    i.putExtra("itemTweet", Parcels.wrap(tweets.get(position)));
                    Activity a = (Activity) context;
                    a.startActivityForResult(i, REQUEST_CODE);
                }
            });
        }

        public void bind(final Tweet tweet) {
            // Create references to each view
            tvBody.setText(tweet.body);
            tvScreenName.setText("@"+tweet.user.screenName);
            tvUserName.setText(tweet.user.name);
            tvTimeStamp.setText(Tweet.getRelativeTimeAgo(tweet.createdAt));
            retweetCount.setText(tweet.retweetCount);
            favoriteCount.setText(tweet.favoriteCount);

            // Bind profile image to profile image view
            Glide.with(context).load(tweet.user.profileImageUrl).fitCenter().
                    transform(new RoundedCornersTransformation(20,10)).into(ivProfileImage);

            // if the tweet contains embedded URLS set the list adapter
            if (!tweet.urls.isEmpty()) {
                lvListImage.setVisibility(View.VISIBLE);
                adapter = new ImageAdapter(tweet.urls, context);
                lvListImage.setAdapter(adapter);
            } else {
                lvListImage.setVisibility(View.GONE);
            }

            Log.d(TAG, String.valueOf(tweet.id) + " " + tweet.user.screenName);

            // If the user clicks the reply button open the compose activity
            btnReply.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent i = new Intent(context, ComposeActivity.class);
                    i.putExtra(USER_NAME_TAG, tvScreenName.getText());
                    // Cast the context as an activity so that we can call startActivity for result
                    Activity a = (Activity) context;
                    a.startActivityForResult(i, REQUEST_CODE);
                }
            });

            // Set an onclick listener on the retweet button
            ibRetweet.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // If the button is pressed, make an API call to twitter to publish the tweet
                    client.retweetTweet(tweet.id, new JsonHttpResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Headers headers, JSON json) {
                            Log.i(TAG, "onSuccess to retweet tweet");
                            try {
                                // Add retweet to timeline
                                Tweet tweet = Tweet.fromJson(json.jsonObject);
                                TimelineActivity activity = (TimelineActivity) context;
                                activity.tweets.add(0,tweet);
                                activity.adapter.notifyDataSetChanged();
                                activity.rvTweets.smoothScrollToPosition(0);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure(int statusCode, Headers headers, String response, Throwable throwable) {
                            Log.e(TAG, "onFailure to publish tweet");
                        }
                    });
                }
            });


            // Set onClickListener to like button
            ibLike.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // If the button is not already pressed like the tweet
                    if (!liked) {
                        client.favoriteTweet(tweet.id, new JsonHttpResponseHandler() {
                            @Override
                            public void onSuccess(int statusCode, Headers headers, JSON json) {
                                Log.d(TAG, "onSuccess to like tweet");
                                // Change the image to indicate that the tweet is now liked
                                ibLike.setImageResource(R.drawable.ic_vector_heart);
                                // Increase the number of likes on the tweet
                                int i = Integer.valueOf(tweet.favoriteCount);
                                favoriteCount.setText(String.valueOf(i + 1));
                                liked = true;

                            }

                            @Override
                            public void onFailure(int statusCode, Headers headers, String response, Throwable throwable) {
                                Log.e(TAG, "onFailure to like a tweet");
                            }
                        });
                    } else {
                        client.unfavoriteTweet(tweet.id, new JsonHttpResponseHandler() {
                            @Override
                            public void onSuccess(int statusCode, Headers headers, JSON json) {
                                Log.d(TAG, "onSuccess to unlike tweet");
                                // Change the image to indicate that the tweet is now liked
                                ibLike.setImageResource(R.drawable.ic_vector_heart_stroke);
                                // Increase the number of likes on the tweet
                                int i = Integer.valueOf(tweet.favoriteCount);
                                favoriteCount.setText(String.valueOf(i));
                                liked = false;

                            }

                            @Override
                            public void onFailure(int statusCode, Headers headers, String response, Throwable throwable) {
                                Log.e(TAG, "onFailure to unlike a tweet");
                            }
                        });
                    }
                }
            });
        }

    }
}
