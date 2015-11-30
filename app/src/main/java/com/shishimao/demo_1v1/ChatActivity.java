package com.shishimao.demo_1v1;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.shishimao.sdk.LooperExecutor;
import com.shishimao.sdk.RTCat;
import com.shishimao.sdk.Receiver;
import com.shishimao.sdk.ReceiverObserver;
import com.shishimao.sdk.Sender;
import com.shishimao.sdk.Session;
import com.shishimao.sdk.SessionObserver;
import com.shishimao.sdk.SessionSendConfig;
import com.shishimao.sdk.Stream;

import com.shishimao.sdk.tools.L;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;


public class ChatActivity extends Activity {
    GLSurfaceView videoView;
    GLSurfaceView videoViewRemote;

    RTCat cat;
    Stream localStream;
    Session session;
    LooperExecutor executor = new LooperExecutor();
    HashMap<String,Sender> senders = new HashMap<>();
    HashMap<String,Receiver> receivers = new HashMap<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        setContentView(R.layout.chat);


        int screenWidth  = getWindowManager().getDefaultDisplay().getWidth();
        int width = 0;
        if(screenWidth > 1500)
        {
            width = 80;
        }else{
            width = 20;
        }
        float mmInPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_MM, width,
                getResources().getDisplayMetrics());

        videoView = (GLSurfaceView)findViewById(R.id.glview);
        ViewGroup.LayoutParams layoutParams=videoView.getLayoutParams();
        layoutParams.width = (int)mmInPx;
        layoutParams.height = (int)mmInPx;
        videoView.setLayoutParams(layoutParams);

        cat = new RTCat(ChatActivity.this, true, true, true, L.VERBOSE);
        localStream = cat.createStream();
        localStream.play(videoView);

        createSession();
    }


    public void createSession()
    {
        Intent intent = getIntent();
        final String token = intent.getStringExtra(MainActivity.TOKEN);
        createSession(token);
    }

    public void createSession(final String token){
        executor.requestStart();
        executor.execute(new Runnable() {
            @Override
            public void run() {

                try {

                    session = cat.createSession(token);

                    class SessionHandler implements SessionObserver {
                        @Override
                        public void in(String token) {


                            JSONObject attr = new JSONObject();
                            SessionSendConfig ssc = new SessionSendConfig(localStream, attr, false);
                            session.sendTo(ssc, token);
                        }

                        @Override
                        public void out(String token) {
                            l(token + "is out");
                            over();
                        }

                        @Override
                        public void connected(JSONArray wits) {
                            String wit = "";
                            for (int i = 0; i < wits.length(); i++) {
                                try {
                                    wit = wit + wits.getString(i);

                                } catch (Exception e) {

                                }
                            }


                            JSONObject attr = new JSONObject();
                            SessionSendConfig ssc = new SessionSendConfig(localStream, attr, false);
                            session.send(ssc);
                        }

                        @Override
                        public void remote(final Receiver receiver) {
                            try {
                                receivers.put(receiver.getId(), receiver);

                                receiver.addObserver(new ReceiverObserver() {
                                    @Override
                                    public void stream(final Stream stream) {

                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                videoViewRemote = new GLSurfaceView(ChatActivity.this);
                                                FrameLayout layout = (FrameLayout) findViewById(R.id.main_layout);
                                                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.MATCH_PARENT);
                                                layout.addView(videoViewRemote, params);
                                                stream.play(videoViewRemote);
                                            }
                                        });

                                    }

                                    @Override
                                    public void message(String message) {

                                    }

                                    @Override
                                    public void close() {

                                    }
                                });

                                receiver.response();
                            } catch (Exception e) {
                                l(e.toString());
                            }


                        }

                        @Override
                        public void local(Sender sender) {
                            senders.put(sender.getId(), sender);
                        }

                        @Override
                        public void message(String token, String message) {

                        }

                        @Override
                        public void error(String error) {

                        }
                    }

                    SessionHandler sh = new SessionHandler();

                    session.addObserver(sh);

                    session.connect();

                } catch (Exception e) {
                    l(e.toString());
                }
            }
        });
    }

    public void l(String o)
    {

        Log.d("RTCatTest", o);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK)
        {

            over();

        }
        return false;
    }

    private void over()
    {
        Intent myIntent = new Intent();
        myIntent = new Intent(ChatActivity.this,MainActivity.class);
        startActivity(myIntent);
        finish();

    }

    @Override
    protected void onPause() {
        super.onPause();
        if(videoView != null)
        {
            videoView.onPause();
        }
        if(videoViewRemote != null)
        {
            videoViewRemote.onPause();
        }
        if(localStream != null)
        {
            localStream.stop();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(videoView != null)
        {
            videoView.onResume();
        }
        if(videoViewRemote != null)
        {
            videoViewRemote.onResume();
        }
        if(localStream != null)
        {
            localStream.restart();
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(session != null)
        {
            session.disconnect();
        }

        if(localStream != null)
        {
            localStream.dispose();
        }

    }
}
