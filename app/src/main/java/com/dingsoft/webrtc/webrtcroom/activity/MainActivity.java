package com.dingsoft.webrtc.webrtcroom.activity;

import android.app.Activity;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.dingsoft.webrtc.webrtcroom.R;
import com.dingsoft.webrtc.webrtcroom.webrtcmodule.PeerConnectionParameters;
import com.dingsoft.webrtc.webrtcroom.webrtcmodule.RtcListener;
import com.dingsoft.webrtc.webrtcroom.webrtcmodule.WebRtcClient;
import com.yanzhenjie.permission.Action;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.Permission;
import org.webrtc.EglBase;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;

import java.util.HashMap;
import java.util.List;

public class MainActivity extends Activity implements RtcListener,View.OnClickListener{
    //控件
    private EditText roomName;
    private Button openCamera;
    private Button switchCamera;
    private Button createRoom;
    private Button exitRoom;
    private SurfaceViewRenderer localSurfaceViewRenderer;
    private LinearLayout remoteVideoLl;
    private HashMap<String,View> remoteViews;
    //EglBase
    private EglBase rootEglBase;
    //WebRtcClient
    private WebRtcClient webRtcClient;
    //PeerConnectionParameters
    private PeerConnectionParameters peerConnectionParameters;
    //host地址
    //private String socketHost = "http://172.16.70.226:8081";
    private String socketHost = "https://172.16.70.226:8443";

    //记录用户首次点击返回键的时间
    private long firstTime = 0;
    //摄像头是否开启
    private boolean isCameraOpen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        roomName =  findViewById(R.id.room);
        openCamera = findViewById(R.id.openCamera);
        openCamera.setOnClickListener(this);
        switchCamera =  findViewById(R.id.switchCamera);
        switchCamera.setOnClickListener(this);
        createRoom = findViewById(R.id.create);
        createRoom.setOnClickListener(this);
        exitRoom = findViewById(R.id.exit);
        exitRoom.setOnClickListener(this);
        localSurfaceViewRenderer = findViewById(R.id.localVideo);
        remoteVideoLl = findViewById(R.id.remoteVideoLl);
        remoteViews = new HashMap<>();
        //创建WebRtcClient
        createWebRtcClient();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //某些机型锁屏点亮后需要重新开启摄像头
        if (isCameraOpen){
            webRtcClient.startCamera(localSurfaceViewRenderer,WebRtcClient.FONT_FACTING);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //数据销毁
        localSurfaceViewRenderer.release();
        localSurfaceViewRenderer = null;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode){
            case KeyEvent.KEYCODE_BACK:
                long secondTime=System.currentTimeMillis();
                if(secondTime-firstTime>2000){
                    Toast.makeText(MainActivity.this,"再按一次退出程序",Toast.LENGTH_SHORT).show();
                    firstTime = secondTime;
                    return true;
                }else{
                    System.exit(0);
                }
                break;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.openCamera:
                //开启_关闭摄像头
                if(isCameraOpen){
                    //关闭
                    webRtcClient.closeCamera();
                    //数据
                    localSurfaceViewRenderer.clearImage();
                    localSurfaceViewRenderer.setBackground(new ColorDrawable(getResources().getColor(R.color.colorBlack)));
                    //localSurfaceViewRenderer.setForeground(new ColorDrawable(R.color.colorBlack));
                    localSurfaceViewRenderer.release();
                    isCameraOpen = false;
                    openCamera.setText("开启摄像头");
                }else{
                    //开启
                    openCamera();
                }
                break;
            case R.id.switchCamera:
                //切换摄像头
                switchCamera();
                break;
            case R.id.create:
                //创建并加入聊天室
                String roomId = roomName.getText().toString();
                if(isCameraOpen){
                    webRtcClient.createAndJoinRoom(roomId);
                    createRoom.setEnabled(false);
                }else{
                    Toast.makeText(this,"请先开启摄像头",Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.exit:
                //退出聊天室
                webRtcClient.exitRoom();
                createRoom.setEnabled(true);
                break;
            default:
                break;
        }
    }

    //创建配置参数
    private void createPeerConnectionParameters(){
        //获取webRtc 音视频配置参数
        Point displaySize = new Point();
        this.getWindowManager().getDefaultDisplay().getSize(displaySize);
        displaySize.set(480,320);
        peerConnectionParameters =  new PeerConnectionParameters(true, false,
                    false, displaySize.x, displaySize.y, 30,
                    0, "VP8",
                    true,false,0,"OPUS",
                    false,false,false,false,false,false,
                    false,false,false,false);
    }

    //创建webRtcClient
    private void createWebRtcClient(){
        //配置参数
        createPeerConnectionParameters();
        //创建视频渲染器
        rootEglBase = EglBase.create();
        //WebRtcClient对象
        webRtcClient = new WebRtcClient(getApplicationContext(),
                rootEglBase,
                peerConnectionParameters,
                MainActivity.this,
                socketHost);
    }

    //本地摄像头创建
    private void openCamera(){
        if(AndPermission.hasPermissions(this,Permission.Group.CAMERA)){
            startCamera();
        }else{
            AndPermission.with(this)
                    .runtime()
                    .permission(Permission.Group.CAMERA)
                    .onGranted(new Action<List<String>>() {
                        @Override
                        public void onAction(List<String> data) {
                            //申请权限成功
                            startCamera();
                        }
                    })
                    .onDenied(new Action<List<String>>() {
                        @Override
                        public void onAction(List<String> data) {
                            //当用户没有允许该权限时，回调该方法
                            Toast.makeText(MainActivity.this, "没有获取照相机权限，该功能无法使用", Toast.LENGTH_SHORT).show();
                        }
                    }).start();
        }
    }

    //开启摄像头
    private void startCamera(){
        //初始化渲染源
        localSurfaceViewRenderer.init(rootEglBase.getEglBaseContext(), null);
        //填充模式
        localSurfaceViewRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        localSurfaceViewRenderer.setZOrderMediaOverlay(true);
        localSurfaceViewRenderer.setEnableHardwareScaler(false);
        localSurfaceViewRenderer.setMirror(true);
        localSurfaceViewRenderer.setBackground(null);
        //启动摄像头
        webRtcClient.startCamera(localSurfaceViewRenderer,WebRtcClient.FONT_FACTING);
        //状态设置
        isCameraOpen = true;
        openCamera.setText("关闭摄像头");
    }

    //切换摄像头
    private void switchCamera(){
        if (webRtcClient != null){
            webRtcClient.switchCamera();
        }
    }

    //清空远端摄像头
    public void clearRemoteCamera(){
        remoteVideoLl.removeAllViews();
    }

    /* RtcListener 数据回调 */
    @Override
    public void onAddRemoteStream(String peerId,VideoTrack videoTrack) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ////UI线程执行
                //构建远端view
                SurfaceViewRenderer remoteView = new SurfaceViewRenderer(MainActivity.this);
                //初始化渲染源
                remoteView.init(rootEglBase.getEglBaseContext(), null);
                //填充模式
                remoteView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
                remoteView.setZOrderMediaOverlay(true);
                remoteView.setEnableHardwareScaler(false);
                remoteView.setMirror(true);
                //控件布局
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(360,360);
                layoutParams.topMargin = 20;
                remoteVideoLl.addView(remoteView,layoutParams);
                //添加至hashmap中
                remoteViews.put(peerId,remoteView);
                //添加数据
                //VideoTrack videoTrack = mediaStream.videoTracks.get(0);
                videoTrack.addSink(remoteView);
            }
        });
    }

    @Override
    public void onRemoveRemoteStream(String peerId) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ////UI线程执行
                //移除远端view
                SurfaceViewRenderer remoteView = (SurfaceViewRenderer)remoteViews.get(peerId);
                if (remoteView != null){
                    remoteVideoLl.removeView(remoteView);
                    remoteViews.remove(peerId);
                    //数据销毁
                    remoteView.release();
                    remoteView = null;
                }
            }
        });
    }
}
