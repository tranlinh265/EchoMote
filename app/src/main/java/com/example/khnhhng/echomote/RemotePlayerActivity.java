package com.example.khnhhng.echomote;

import android.app.Activity;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import org.fourthline.cling.android.AndroidUpnpService;
import org.fourthline.cling.controlpoint.ActionCallback;
import org.fourthline.cling.model.action.ActionArgumentValue;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.Action;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.model.types.UDAServiceId;


public class RemotePlayerActivity extends Activity implements View.OnClickListener, SeekBar.OnSeekBarChangeListener{
    public ImageButton powerBtn;
    private ImageButton playBtn;
    private ImageButton pauseBtn;
    private ImageButton stopBtn;
    public SeekBar volumeBar;
    static private Device device;
    static private AndroidUpnpService upnpService;

    private boolean isPlaying=false;
    private boolean isPause=false;
    private boolean isStop=false;
    public boolean isOn;
    public static int currentVolume;

    private static RemotePlayerActivity instance;

    public static RemotePlayerActivity getInstance()
    {
        return instance;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ac_remoteplayer);

        powerBtn = (ImageButton) findViewById(R.id.powerBtn);
        powerBtn.setOnClickListener(this);

        playBtn = (ImageButton) findViewById(R.id.playBtn);
        playBtn.setOnClickListener(this);

        pauseBtn = (ImageButton) findViewById(R.id.pauseBtn);
        pauseBtn.setOnClickListener(this);

        stopBtn = (ImageButton) findViewById(R.id.stopBtn);
        stopBtn.setOnClickListener(this);

        volumeBar = (SeekBar) findViewById(R.id.volumeBar);
        volumeBar.setOnSeekBarChangeListener(this);

        device = BrowserActivity.getControlling_device();
        upnpService = BrowserActivity.upnpService;

        isOn = getStatus(device.findService(new UDAServiceId("SwitchPower")));
//        powerBtn.setText(isOn ? "ON" : "OFF");

        currentVolume = getVolume();
        volumeBar.setProgress(currentVolume);

        instance = this;
        initView();
    }
    private void initView(){
        if(isOn){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                powerBtn.setBackground(getResources().getDrawable(R.drawable.circle_on_ic));
            }
        }
    }
//    public static void updatePower(int i)
//    {
//        powerBtn.setText(i == 1 ? "ON" : "OFF");
//    }


    private boolean getStatus(Service service)
    {
//        if(device.)
        Action action = service.getAction("GetStatus");

        ActionInvocation invocation = new ActionInvocation(action);
        new ActionCallback.Default(invocation, upnpService.getControlPoint()).run();
        ActionArgumentValue status = invocation.getOutput("ResultStatus");
        if(status == null)
        {
            RemotePlayerActivity.this.finish();
            return false;
        }

        return (boolean)status.getValue();
    }

    private void switchPower()
    {
        Service service = device.findService(new UDAServiceId("SwitchPower"));
        Action action = service.getAction("SetTarget");
        ActionInvocation settargetaction = new ActionInvocation(action);
        boolean newvalue = getStatus(service)? false: true;
        settargetaction.setInput("newTargetValue", newvalue);
        new ActionCallback.Default(settargetaction, upnpService.getControlPoint()).run();
        Log.d("failed", settargetaction.getFailure() != null ? "Disconnected" : "On service");
        if(isOn){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                powerBtn.setBackground(getResources().getDrawable(R.drawable.circle_ic));
            }
        }else{
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                powerBtn.setBackground(getResources().getDrawable(R.drawable.circle_on_ic));
            }
        }
        isOn = !isOn;
    }

    private void playAudio()
    {
        Service service = device.findService(new UDAServiceId("PlayCD"));
        if(!isOn) return;
        Action action = service.getAction("Play");
        ActionInvocation settargetaction = new ActionInvocation(action);
        new ActionCallback.Default(settargetaction, upnpService.getControlPoint()).run();
        if(settargetaction.getFailure() != null)
            RemotePlayerActivity.this.finish();
    }

    private void pauseAudio()
    {
        Service service = device.findService(new UDAServiceId("PlayCD"));
        if(!isOn) return;
        Action action = service.getAction("Pause");
        ActionInvocation settargetaction = new ActionInvocation(action);
        new ActionCallback.Default(settargetaction, upnpService.getControlPoint()).run();
        if(settargetaction.getFailure() != null)
            RemotePlayerActivity.this.finish();
    }

    private void stopAudio()
    {
        Service service = device.findService(new UDAServiceId("PlayCD"));
        if(!isOn) return;
        Action action = service.getAction("Stop");
        ActionInvocation settargetaction = new ActionInvocation(action);
        new ActionCallback.Default(settargetaction, upnpService.getControlPoint()).run();
        if(settargetaction.getFailure() != null)
            RemotePlayerActivity.this.finish();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        instance = null;
        BrowserActivity.updateVolumeCallback.end();
        BrowserActivity.updatePowerCallback.end();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId())
        {
            case R.id.powerBtn:
                this.updateStatus(false, false,false);
                switchPower(); break;
            case R.id.playBtn:
                if(this.isOn && !this.isPlaying){
                    this.updateStatus(true, false,false);
                }
                playAudio(); break;
            case R.id.pauseBtn:
                if(this.isOn && !this.isPause){
                    this.updateStatus(false, true, false);
                }
                pauseAudio(); break;
            case R.id.stopBtn:
                if(this.isOn && !this.isStop){
                    this.updateStatus(false,false,true);
                }
                stopAudio();break;
        }
    }

    private void updateStatus(boolean isPlaying, boolean isPause, boolean isStop){
        this.isPlaying = isPlaying;
        this.isPause = isPause;
        this.isStop = isStop;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN){
            if(this.isPlaying){
                playBtn.setBackground(getResources().getDrawable(R.drawable.circle_on_ic));
            }
            else{
                playBtn.setBackground(getResources().getDrawable(R.drawable.circle_ic));
            }
            if(this.isPause){
                pauseBtn.setBackground(getResources().getDrawable(R.drawable.circle_on_ic));
            }
            else{
                pauseBtn.setBackground(getResources().getDrawable(R.drawable.circle_ic));
            }
            if(this.isStop){
                stopBtn.setBackground(getResources().getDrawable(R.drawable.circle_on_ic));
            }
            else{
                stopBtn.setBackground(getResources().getDrawable(R.drawable.circle_ic));
            }
        }

    }
    public static void updateVolume(int value)
    {
//        Service service = device.findService(new UDAServiceId("Audio"));
//        if(!isOn) {
//            volumeBar.setProgress(currentVolume);
//            return;
//        }
//        Action action = service.getAction("SetVolume");
//        ActionInvocation settargetaction = new ActionInvocation(action);
//        settargetaction.setInput("NewVolume", value);
////        Log.d("Volume", "" + value);
//        new ActionCallback.Default(settargetaction, upnpService.getControlPoint()).run();
////        Log.d("AfterSetVolume", "" + getVolume());
    }

    private int getVolume()
    {
        Service service = device.findService(new UDAServiceId("Audio"));
        Action action = service.getAction("GetAudio");
        ActionInvocation invocation = new ActionInvocation(action);
        new ActionCallback.Default(invocation, upnpService.getControlPoint()).run();
        ActionArgumentValue currentVolume = invocation.getOutput("CurrentVolume");
        if(currentVolume == null)
        {
            RemotePlayerActivity.this.finish();
            return 0;
        }
//        Log.d("Volume", "" + currentVolume);
        return (int)currentVolume.getValue();
    }
    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        if(seekBar != volumeBar) return;
        if(!b) return;
        Action action = device.findService(new UDAServiceId("Audio")).getAction("SetVolume");
        ActionInvocation settargetaction = new ActionInvocation(action);
        settargetaction.setInput("NewVolume", i);
        ActionCallback actionCallback = new ActionCallback(settargetaction) {
            @Override
            public void success(ActionInvocation actionInvocation) {
                Log.e("123","123");
            }

            @Override
            public void failure(ActionInvocation actionInvocation, UpnpResponse upnpResponse, String s) {
                RemotePlayerActivity.this.finish();
            }
        };

        upnpService.getControlPoint().execute(actionCallback);
//        Log.d("Event", "Send");
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        BrowserActivity.updateVolumeCallback.end();
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
//        if(seekBar != volumeBar) return;
//        updateVolume(seekBar.getProgress());
        upnpService.getControlPoint().execute(BrowserActivity.updateVolumeCallback);
    }
}
