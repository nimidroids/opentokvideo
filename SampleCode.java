package com.nimidroids.videocall;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



import android.app.ActionBar;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.parse.ParseException;
import com.nimidroids.utils.Constants;
import com.nimidroids.utils.OpenTokConfig;
import com.opentok.android.BaseVideoRenderer;
import com.opentok.android.OpentokError;
import com.opentok.android.Publisher;
import com.opentok.android.PublisherKit;
import com.opentok.android.Session;
import com.opentok.android.Stream;
import com.opentok.android.Subscriber;
import com.opentok.android.SubscriberKit;
import com.parse.FindCallback;
import com.parse.FunctionCallback;
import com.parse.GetCallback;
import com.parse.Parse;
import com.parse.ParseACL;
import com.parse.ParseCloud;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

/**
 * This application demonstrates the basic workflow for getting started with the
 * OpenTok 2.0 Android SDK. For more information, see the README.md file in the
 * samples directory.
 */
public class HelloWorldActivity extends Activity implements
        Session.SessionListener, Publisher.PublisherListener,
        Subscriber.SubscriberListener,Subscriber.VideoListener {

    private static final String LOGTAG = "demo-hello-world";
    private Session mSession;
    private Publisher mPublisher;
    private Subscriber mSubscriber;
    private ArrayList<Stream> mStreams;
    protected Handler mHandler = new Handler();

    private RelativeLayout mPublisherViewContainer;
    private RelativeLayout mSubscriberViewContainer;

    // Spinning wheel for loading subscriber view
    private ProgressBar mLoadingSub;

    private boolean resumeHasRun = false;

    private NotificationCompat.Builder mNotifyBuilder;
    NotificationManager mNotificationManager;
    private int notificationId;
   
    private String subToken=null;
    private String pubToken=null;
    private String sessID=null;
    private boolean isCompleteProcess=false;
    private String role=null;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(LOGTAG, "ONCREATE");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_layout);
        
      /*  */
        Bundle localBundle = getIntent().getExtras();
        sessID = localBundle.getString(Constants.SESSION);
        pubToken = localBundle.getString(Constants.PUBLISHER_TOKEN);
        subToken = localBundle.getString(Constants.SUBSCRIBER_TOKEN);
        role = localBundle.getString(Constants.ROLE);
      
  	//ActionBar actionBar = getActionBar();
      //actionBar.setHomeButtonEnabled(true);
      //actionBar.setDisplayHomeAsUpEnabled(true);

      mPublisherViewContainer = (RelativeLayout) findViewById(R.id.publisherview);
      mSubscriberViewContainer = (RelativeLayout) findViewById(R.id.subscriberview);
      mLoadingSub = (ProgressBar) findViewById(R.id.loadingSpinner);

      mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

      mStreams = new ArrayList<Stream>();
      sessionConnect();
        
        
       
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            onBackPressed();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Remove publisher & subscriber views because we want to reuse them
        if (mSubscriber != null) {
            mSubscriberViewContainer.removeView(mSubscriber.getView());
        }
        reloadInterface();

    }

    @Override
    public void onPause() {
        super.onPause();

        if (mSession != null) {
            mSession.onPause();

            if (mSubscriber != null) {
                mSubscriberViewContainer.removeView(mSubscriber.getView());
            }
        }

        mNotifyBuilder = new NotificationCompat.Builder(this)
                .setContentTitle(this.getTitle())
                .setContentText(getResources().getString(R.string.notification))
                .setSmallIcon(R.drawable.ic_launcher).setOngoing(true);

        Intent notificationIntent = new Intent(this, HelloWorldActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        mNotifyBuilder.setContentIntent(intent);
        notificationId = (int) System.currentTimeMillis();
        mNotificationManager.notify(notificationId, mNotifyBuilder.build());

    }

    @Override
    public void onResume() {
        super.onResume();

        if (!resumeHasRun) {
            resumeHasRun = true;
            return;
        } else {
            if (mSession != null) {
                mSession.onResume();
            }
        }
        mNotificationManager.cancel(notificationId);

        reloadInterface();

    }

    @Override
    public void onStop() {
        super.onStop();

        if (isFinishing()) {
            mNotificationManager.cancel(notificationId);
            if (mSession != null) {
                mSession.disconnect();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (mSession != null) {
            mSession.disconnect();
        }
        super.onBackPressed();
    }
    
    @Override
    public void onDestroy() {
        mNotificationManager.cancel(notificationId);
        if (mSession != null) {
            mSession.disconnect();
        }
        super.onDestroy();
        finish();
    }
    
    public void reloadInterface() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mSubscriber != null) {
                    attachSubscriberView(mSubscriber);
                }
            }
        }, 500);
    }

    public void reloadPublisherInterface() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mPublisher != null) {
                    attachPublisherView(mPublisher);
                }
            }
        }, 500);
    }
    
    private void sessionConnect() {
        if (mSession == null) {
            mSession = new Session(HelloWorldActivity.this,
                    OpenTokConfig.API_KEY, sessID);
            mSession.setSessionListener(this);
            if("publisher".equals(role)){
            mSession.connect(pubToken);
            }
            else{
             mSession.connect(subToken);	
            }
        }
    }
    @Override
    public void onConnected(Session session) {
        Log.i(LOGTAG, "Connected to the session.");
        if (mPublisher == null) {
        	
            mPublisher = new Publisher(HelloWorldActivity.this, "publisher");
            mPublisher.setPublishAudio(true);
            mPublisher.setPublishVideo(true);
            mPublisher.setCameraId(1);
            mPublisher.setPublisherListener(this);
            attachPublisherView(mPublisher);
            
            mSession.publish(mPublisher);
        }
    }
    
   
    @Override
    public void onDisconnected(Session session) {
        Log.i(LOGTAG, "Disconnected from the session.");
        if (mPublisher != null) {
            mPublisherViewContainer.removeView(mPublisher.getView());
        }

        if (mSubscriber != null) {
            mSubscriberViewContainer.removeView(mSubscriber.getView());
        }

        mPublisher = null;
        mSubscriber = null;
        mStreams.clear();
        mSession = null;
    }

    private void subscribeToStream(Stream stream) {
        mSubscriber = new Subscriber(HelloWorldActivity.this, stream);
        mSubscriber.setVideoListener(this);
        mSession.subscribe(mSubscriber);
       
        if (!stream.hasAudio())
        {
        	
        	mSubscriber.setSubscribeToAudio(true);	//just now add
          }
        //if (!stream.hasVideo())
        { 
        	  mSubscriber.setSubscribeToVideo(true); //just now add
        	}
        // start loading spinning
        mLoadingSub.setVisibility(View.VISIBLE);
      
    }

    private void unsubscribeFromStream(Stream stream) {
        mStreams.remove(stream);
        if (mSubscriber.getStream().equals(stream)) {
            mSubscriberViewContainer.removeView(mSubscriber.getView());
            mSubscriber = null;
            if (!mStreams.isEmpty()) {
                subscribeToStream(mStreams.get(0));
            }
        }
    }

    private void attachSubscriberView(Subscriber subscriber) {
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                getResources().getDisplayMetrics().widthPixels, getResources()
                        .getDisplayMetrics().heightPixels);
        mSubscriberViewContainer.addView(mSubscriber.getView(), layoutParams);
        subscriber.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE,
                BaseVideoRenderer.STYLE_VIDEO_FILL); 
    }

    private void attachPublisherView(Publisher publisher) {
        mPublisher.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE,
                BaseVideoRenderer.STYLE_VIDEO_FILL);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                320, 240);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM,
                RelativeLayout.TRUE);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT,
                RelativeLayout.TRUE);
        layoutParams.bottomMargin = dpToPx(8);
        layoutParams.rightMargin = dpToPx(8);
        mPublisherViewContainer.addView(mPublisher.getView(), layoutParams);
    }

    @Override
    public void onError(Session session, OpentokError exception) {
        Log.i(LOGTAG, "Session exception: " + exception.getMessage());
    }

    @Override
    public void onStreamReceived(Session session, Stream stream) {
           
        if (!OpenTokConfig.SUBSCRIBE_TO_SELF) {
            mStreams.add(stream);
            if (mSubscriber == null) {
                subscribeToStream(stream);
            }
        }
    }

    @Override
    public void onStreamDropped(Session session, Stream stream) {
        if (!OpenTokConfig.SUBSCRIBE_TO_SELF) {
            if (mSubscriber != null) {
                unsubscribeFromStream(stream);
            }
        }
    }

    @Override
    public void onStreamCreated(PublisherKit publisher, Stream stream) {
        if (!OpenTokConfig.SUBSCRIBE_TO_SELF) {
            mStreams.add(stream);
            if (mSubscriber == null) {
                subscribeToStream(stream);
                
            }
        }
    }

    @Override
    public void onStreamDestroyed(PublisherKit publisher, Stream stream) {
        if ((OpenTokConfig.SUBSCRIBE_TO_SELF && mSubscriber != null)) {
            unsubscribeFromStream(stream);
        }
    }

    @Override
    public void onError(PublisherKit publisher, OpentokError exception) {
        Log.i(LOGTAG, "Publisher exception: " + exception.getMessage());
    }

    @Override
    public void onVideoDataReceived(SubscriberKit subscriber) {
        Log.i(LOGTAG, "First frame received");

        // stop loading spinning
        mLoadingSub.setVisibility(View.GONE);
        attachSubscriberView(mSubscriber);
    }

    /**
     * Converts dp to real pixels, according to the screen density.
     * 
     * @param dp
     *            A number of density-independent pixels.
     * @return The equivalent number of real pixels.
     */
    private int dpToPx(int dp) {
        double screenDensity = this.getResources().getDisplayMetrics().density;
        return (int) (screenDensity * (double) dp);
    }

    /*@Override
	public void onVideoDisabled(SubscriberKit subscriber, String reason) {
        Log.i(LOGTAG,
                "Video disabled:" + reason);		
	}
   */
/*	@Override
	public void onVideoEnabled(SubscriberKit subscriber) {
        Log.i(LOGTAG,
                "Video enabled:" + reason);		
	}
*/
/*	@Override
	public void onVideoDisabled(SubscriberKit arg0) {
		// TODO Auto-generated method stub
		
	}*/
	@Override
	public void onConnected(SubscriberKit arg0) {
		// TODO Auto-generated method stub
		  // mViewContainer is an Android View
	       RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
	               getResources().getDisplayMetrics().widthPixels, getResources()
	                       .getDisplayMetrics().heightPixels);
	       mSubscriberViewContainer.addView(mSubscriber.getView(), layoutParams);
	      
		
	}
	
	@Override
	public void onDisconnected(SubscriberKit arg0) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void onError(SubscriberKit arg0, OpentokError arg1) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void onVideoDisabled(SubscriberKit arg0) {
		// TODO Auto-generated method stub
		
	}

}