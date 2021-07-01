package com.codepath.apps.restclienttemplate;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.codepath.apps.restclienttemplate.models.Tweet;
import com.codepath.asynchttpclient.callback.JsonHttpResponseHandler;

import org.json.JSONException;
import org.parceler.Parcels;

import jp.wasabeef.glide.transformations.RoundedCornersTransformation;
import okhttp3.Headers;

import static com.codepath.apps.restclienttemplate.TimelineActivity.REQUEST_CODE;
import static com.codepath.apps.restclienttemplate.TweetsAdapter.USER_NAME_TAG;

public class DetailsActivity extends AppCompatActivity {

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
    Tweet tweet;
    Context context;
    ImageButton ret;
    public static final String TAG = "DetailsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        ivProfileImage = findViewById(R.id.ivProfileImage);
        tvBody = findViewById(R.id.tvBody);
        tvScreenName = findViewById(R.id.tvScreenName);
        tvTimeStamp = findViewById(R.id.tvTimeStamp);
        lvListImage = findViewById(R.id.lvListImage);
        btnReply = findViewById(R.id.ibReply);
        tvUserName = findViewById(R.id.tvUsername);
        client = TwitterApp.getRestClient(this);
        ibRetweet = findViewById(R.id.ibRetweet);
        ibLike = findViewById(R.id.ibLike);
        retweetCount = findViewById(R.id.tvRetweets);
        favoriteCount = findViewById(R.id.tvLikes);
        ret = findViewById(R.id.ibBack);
        liked = false;

        tweet = (Tweet) Parcels.unwrap(getIntent().getParcelableExtra("itemTweet"));
        context = getApplicationContext();

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
                startActivityForResult(i, REQUEST_CODE);
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
                    }

                    @Override
                    public void onFailure(int statusCode, Headers headers, String response, Throwable throwable) {
                        Log.e(TAG, "onFailure to publish tweet");
                    }
                });
            }
        });

        // Set onClick listener to return button
        ret.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
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

    // Retrieve the composed tweet back from ComposeActivity
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @org.jetbrains.annotations.Nullable Intent data) {
        // requestCode is what we defined above REQUEST_CODE
        // resultCode is what we defined by Android (make sure child finished successfully)
        // data is whatever child communicated back to us
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            // Get data from the intent (tweet)
            Tweet tweet = Parcels.unwrap(data.getParcelableExtra(ComposeActivity.KEY));
            // Update the recycler view with new tweet
            // tweets.add(0, tweet); // Modify data source to include new tweet at the first position (top of timeline)
            // adapter.notifyDataSetChanged(); // Notify the adapter
            // rvTweets.smoothScrollToPosition(0); // Automatically go to top of timeline
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}