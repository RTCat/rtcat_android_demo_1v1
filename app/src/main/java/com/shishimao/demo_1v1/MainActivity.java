package com.shishimao.demo_1v1;

import android.app.Activity;

import android.content.Intent;
import android.os.Bundle;

import android.view.View;

import android.widget.ProgressBar;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

public class MainActivity extends Activity {

    ProgressBar spinner;
    public static final String TOKEN = "TOKEN";
    private Socket mSocket;
    {
        try {
            mSocket = IO.socket("http://rtcat.io:1111");
        } catch (URISyntaxException e) {}
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        spinner = (ProgressBar)findViewById(R.id.progressBar1);
        mSocket.on("new message", onNewMessage);
        mSocket.connect();
    }

    public void sendMessage(View view)
    {
        JSONObject packet = new JSONObject();
        JSONObject content = new JSONObject();
        try {
            packet.put("content", content);
            packet.put("eventName", "in");
            mSocket.emit("new message", packet.toString());

            spinner.setVisibility(View.VISIBLE);
        } catch (JSONException e) {

        }
    }

    private Emitter.Listener onNewMessage = new Emitter.Listener(){
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject json = (JSONObject) args[0];
                    try {
                        String eventName = json.getString("eventName");

                        switch (eventName) {
                            case "wait":
                                t("wait");
                                break;
                            case "get_token":
                                String token = json.getString("token");

                                Intent itent = new Intent();
                                itent.putExtra(TOKEN, token);

                                itent.setClass(MainActivity.this, ChatActivity.class);
                                startActivity(itent);
                                MainActivity.this.finish();

                                spinner.setVisibility(View.GONE);

                                break;
                            default:
                                break;
                        }
                    } catch (Exception e) {

                    }

                }
            });
        }
    };

    public void t(String s) {
        Toast.makeText(MainActivity.this, s, Toast.LENGTH_SHORT).show();
    }

}
