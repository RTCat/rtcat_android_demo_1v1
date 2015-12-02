## 实时猫 Android SDK Demo
根据[实时猫 Android SDK](https://shishimao.com) 开发的 Demo

## 说明
此样例通过和[后台](https://github.com/RTCat/rtcat_android_demo_1v1_backend)交互，实现1对1视频和音频聊天功能

## 使用

1. `git clone https://github.com/zombiecong/rtcat_android_demo_1v1.git`

2. 通过`Android Studio`导入, `File > Import Project` ,选择项目中的build.gradle文件导入

3. 在项目中增加权限和jar，so文件（详情参考实时猫Android SDK 文档）


## 代码说明
### `MainActivity.java`

通过`socket-io`和服务器通信获得token，开发者可以自行部署[后台服务器](https://github.com/RTCat/rtcat_android_demo_1v1_backend)，并更新以下后台地址

```java
mSocket = IO.socket("xxx");
```

### `ChatActivity.java`


以下代码为了适配Android 电视屏幕大，改变`videoView`大小， `videoView`用于渲染视频

```java
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
```


以下代码创建RTCat对象,并创建播放本地流。RTCat对象用于创建本地流和session,详情参考 api

```java
        cat = new RTCat(ChatActivity.this, true, true, true, L.VERBOSE);
        localStream = cat.createStream();
        localStream.play(videoView);
```

以下是 Session使用方法，具体内容通过代码注释说明

```
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
```


回收资源, 需要回收Session和Stream（本地）资源。 

```
        if(session != null)
        {
            session.disconnect();
        }

        if(localStream != null)
        {
            localStream.dispose();
        }

```