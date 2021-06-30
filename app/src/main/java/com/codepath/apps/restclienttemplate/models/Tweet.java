package com.codepath.apps.restclienttemplate.models;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.parceler.Parcel;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Parcel
public class Tweet {

    public String body;
    public String id;
    public String createdAt;
    public User user;
    public List<String> urls;
    private static final int SECOND_MILLIS = 1000;
    private static final int MINUTE_MILLIS = 60 * SECOND_MILLIS;
    private static final int HOUR_MILLIS = 60 * MINUTE_MILLIS;
    private static final int DAY_MILLIS = 24 * HOUR_MILLIS;
    public static final String TAG = "Tweet";

    // Empty constructor needed by the Parceler library
    public Tweet() {}

    // Given the JSON object, turn it into a tweet object
    public static Tweet fromJson(JSONObject jsonObject) throws JSONException {
        Tweet tweet = new Tweet();
        tweet.body = jsonObject.getString("text");
        tweet.createdAt = jsonObject.getString("created_at");
        tweet.id = jsonObject.getString("id");
        tweet.user = User.fromJson(jsonObject.getJSONObject("user"));
        if (jsonObject.has("extended_entities")) {
            tweet.urls = getImageUrlArray(jsonObject.getJSONObject("extended_entities").getJSONArray("media"));
        }
        else if (jsonObject.getJSONObject("entities").has("media")){
            tweet.urls = getImageUrlArray(jsonObject.getJSONObject("entities").getJSONArray("media"));
        }
        else {
            tweet.urls = new ArrayList<>();
        }
        return tweet;
    }

    private static List<String> getImageUrlArray(JSONArray jsonArray) throws JSONException {
        List<String> urls = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            String type = jsonArray.getJSONObject(i).getString("type");
            urls.add(jsonArray.getJSONObject(i).getString("media_url_https"));
            Log.d(TAG, String.valueOf(jsonArray));
        }
        return urls;
    }

    // Get a list of Tweet objects
    public static List<Tweet> fromJsonArray(JSONArray jsonArray) throws JSONException {
        List<Tweet> tweets = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            tweets.add(fromJson(jsonArray.getJSONObject(i)));
        }
        return tweets;
    }

    // Get the correct relative time
    public static String getRelativeTimeAgo(String rawJsonDate) {
        String twitterFormat = "EEE MMM dd HH:mm:ss ZZZZZ yyyy";
        SimpleDateFormat sf = new SimpleDateFormat(twitterFormat, Locale.ENGLISH);
        sf.setLenient(true);

        try {
            long time = sf.parse(rawJsonDate).getTime();
            long now = System.currentTimeMillis();

            final long diff = now - time;
            if (diff < MINUTE_MILLIS) {
                return "just now";
            } else if (diff < 2 * MINUTE_MILLIS) {
                return "a minute ago";
            } else if (diff < 50 * MINUTE_MILLIS) {
                return diff / MINUTE_MILLIS + " m";
            } else if (diff < 90 * MINUTE_MILLIS) {
                return "an hour ago";
            } else if (diff < 24 * HOUR_MILLIS) {
                return diff / HOUR_MILLIS + " h";
            } else if (diff < 48 * HOUR_MILLIS) {
                return "yesterday";
            } else {
                return diff / DAY_MILLIS + " d";
            }
        } catch (ParseException e) {
            Log.i(TAG, "getRelativeTimeAgo failed");
            e.printStackTrace();
        }

        return "";
    }

}
