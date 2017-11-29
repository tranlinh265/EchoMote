package com.example.khnhhng.echomote;

import android.app.Activity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
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
    public ImageView powerBtn;
    private ImageView playBtn;
    private ImageView pauseBtn;
    private ImageView stopBtn;
    public SeekBar volumeBar;
    static private Device device;
    static private AndroidUpnpService upnpService;

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
        setContentView(R.layout.activityremoteplayer);

        powerBtn = (ImageView) findViewById(R.id.powerBtn);
        powerBtn.setOnClickListener(this);

        playBtn = (ImageView) findViewById(R.id.playBtn);
        playBtn.setOnClickListener(this);

        pauseBtn = (ImageView) findViewById(R.id.pauseBtn);
        pauseBtn.setOnClickListener(this);

        stopBtn = (ImageView) findViewById(R.id.stopBtn);
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
                switchPower(); break;
            case R.id.playBtn:
                playAudio(); break;
            case R.id.pauseBtn:
                pauseAudio(); break;
            case R.id.stopBtn:
                stopAudio();break;
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
