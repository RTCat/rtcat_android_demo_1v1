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
        //大屏幕适配
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


    public void createSession(){
        Intent intent = getIntent();
        final String token = intent.getStringExtra(MainActivity.TOKEN);
        //线程工具, execute 新建一个线程
        executor.requestStart();
        executor.execute(new Runnable() {
            @Override
            public void run() {

                try {
                    //通过 token初始化一个session
                    session = cat.createSession(token);
                    //实现SessionObserver接口的SessionHandler类,用来监听Session触发事件
                    class SessionHandler implements SessionObserver {
                        //监听用户连入session事件
                        @Override
                        public void in(String token) {


                            JSONObject attr = new JSONObject();

                            SessionSendConfig ssc = new SessionSendConfig(localStream, attr, false);
                            //sendTo用于和其他用户(某一用户)建立连接并发送Stream,建立数据通道, 可以通过attr自己附带属性
                            session.sendTo(ssc, token);
                        }
                        //监听用户离开session事件,这里由于是 1对1,所以有人离开session就结束Acitivy
                        @Override
                        public void out(String token) {
                            l(token + "is out");
                            over();
                        }
                        //连接到Session时触发,返回session里已有的用户
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
                            //区别sendTo给在session中的所有用户发送.
                            session.send(ssc);
                        }

                        //获得远程通道
                        @Override
                        public void remote(final Receiver receiver) {
                            try {
                                //通过一个 List来维护receiver
                                receivers.put(receiver.getId(), receiver);
                                //给receiver增加一个ReceiverObserver,用于触发监听事件
                                receiver.addObserver(new ReceiverObserver() {
                                    @Override
                                    public void stream(final Stream stream) {
                                        //收到远程流,并播放 (需要自己动态增加一个GLSurfaceView对象).
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
                        //获得本地通道
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
                    //增加观察者,一定要在 connect方法之前执行.
                    session.addObserver(sh);
                    //连接服务器
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
