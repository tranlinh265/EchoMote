package com.example.khnhhng.echomote;


import android.app.ListActivity;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import org.fourthline.cling.android.AndroidUpnpService;
import org.fourthline.cling.controlpoint.ActionCallback;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.meta.*;
import org.fourthline.cling.model.types.UDAServiceId;
import static android.content.Context.TELEPHONY_SERVICE;

public class CallListener {
    private boolean isOn;
    private int currentVolume;
    private boolean isMute = false;
    private AndroidUpnpService upnpService;
    private Device device;

    public CallListener(ListActivity activity, Device device)
    {
        this.device = device;
        upnpService = BrowserActivity.upnpService;

        Service service = device.findService(new UDAServiceId("SwitchPower"));
        isOn = getStatus(service);

        TelephonyManager telephonyManager = (TelephonyManager) activity.getSystemService(TELEPHONY_SERVICE);
        PhoneStateListener phoneStateListener = new PhoneStateListener(){
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                switch(state)
                {
                    case TelephonyManager.CALL_STATE_IDLE:
                        unmute(); break;
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                    case TelephonyManager.CALL_STATE_RINGING:
                        mute(); break;
                }
            }
        };
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    private boolean getStatus(Service service)
    {
        Action action = service.getAction("GetStatus");
        ActionInvocation invocation = new ActionInvocation(action);
        new ActionCallback.Default(invocation, upnpService.getControlPoint()).run();
        Boolean status = (Boolean) invocation.getOutput("ResultStatus").getValue();
        return status.booleanValue();
    }

    public void mute()
    {
        if(!isOn) return;
        if(!isMute)
            currentVolume = getVolume();
        updateVolume(0);
        isMute = true;
    }

    public void unmute()
    {
        if(!isOn) return;
        if(getVolume() == 0)
            updateVolume(currentVolume);
        isMute = false;
    }

    public int getVolume()
    {
        Service service = device.findService(new UDAServiceId("Audio"));
        Action action = service.getAction("GetAudio");
        ActionInvocation invocation = new ActionInvocation(action);
        new ActionCallback.Default(invocation, upnpService.getControlPoint()).run();
        Integer currentVolume = (Integer) invocation.getOutput("CurrentVolume").getValue();
        return currentVolume.intValue();
    }

    public void updateVolume(int value)
    {
        Service service = device.findService(new UDAServiceId("Audio"));
        if(!isOn) {
            currentVolume = getVolume();
            return;
        }
        Action action = service.getAction("SetVolume");
        ActionInvocation settargetaction = new ActionInvocation(action);
        settargetaction.setInput("NewVolume", value);
        new ActionCallback.Default(settargetaction, upnpService.getControlPoint()).run();
    }
}
