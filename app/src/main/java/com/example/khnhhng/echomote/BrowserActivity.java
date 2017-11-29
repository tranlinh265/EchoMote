/*
 * Copyright (C) 2013 4th Line GmbH, Switzerland
 *
 * The contents of this file are subject to the terms of either the GNU
 * Lesser General Public License Version 2 or later ("LGPL") or the
 * Common Development and Distribution License Version 1 or later
 * ("CDDL") (collectively, the "License"). You may not use this file
 * except in compliance with the License. See LICENSE.txt for more
 * information.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package com.example.khnhhng.echomote;

import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import org.fourthline.cling.android.AndroidUpnpService;
import org.fourthline.cling.android.AndroidUpnpServiceImpl;
import org.fourthline.cling.android.FixedAndroidLogHandler;
import org.fourthline.cling.controlpoint.SubscriptionCallback;
import org.fourthline.cling.model.gena.CancelReason;
import org.fourthline.cling.model.gena.GENASubscription;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.message.header.UDADeviceTypeHeader;
import org.fourthline.cling.model.meta.*;
import org.fourthline.cling.model.state.StateVariableValue;
import org.fourthline.cling.model.types.UDADeviceType;
import org.fourthline.cling.model.types.UDAServiceId;
import org.fourthline.cling.registry.DefaultRegistryListener;
import org.fourthline.cling.registry.Registry;
import org.fourthline.cling.transport.Router;
import org.fourthline.cling.transport.RouterException;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Christian Bauer
 */
// DOC:CLASS
public class BrowserActivity extends ListActivity {

    // DOC:CLASS
    // DOC:SERVICE_BINDING
    private static Device controlling_device;

    public static boolean remote_display = false;

    public static Device getControlling_device()
    {
        return controlling_device;
    }

    private ArrayAdapter<DeviceDisplay> listAdapter;

    private BrowseRegistryListener registryListener = new BrowseRegistryListener(this);

    public static AndroidUpnpService upnpService;
    public static Service sevice;
    public static SubscriptionCallback updateVolumeCallback;
    public static SubscriptionCallback updatePowerCallback;

    private ServiceConnection serviceConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder service) {
            upnpService = (AndroidUpnpService) service;

            // Clear the list
            listAdapter.clear();

            // Get ready for future controlling_device advertisements
            upnpService.getRegistry().addListener(registryListener);

            // Now add all devices to the list we already know about
            for (Device device : upnpService.getRegistry().getDevices()) {
                registryListener.deviceAdded(device);
            }

            // Search asynchronously for all devices, they will respond soon
            upnpService.getControlPoint().search(new UDADeviceTypeHeader(new UDADeviceType("CDPlayer")));

        }

        public void onServiceDisconnected(ComponentName className) {
            upnpService = null;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Fix the logging integration between java.util.logging and Android internal logging
        org.seamless.util.logging.LoggingUtil.resetRootHandler(
            new FixedAndroidLogHandler()
        );
        // Now you can enable logging as needed for various categories of Cling:
        // Logger.getLogger("org.fourthline.cling").setLevel(Level.FINEST);

        listAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        setListAdapter(listAdapter);

        // This will start the UPnP service if it wasn't already started
        getApplicationContext().bindService(
            new Intent(this, AndroidUpnpServiceImpl.class),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (upnpService != null) {
            upnpService.getRegistry().removeListener(registryListener);
        }
        // This will stop the UPnP service if nobody else is bound to it
        getApplicationContext().unbindService(serviceConnection);
    }
    // DOC:SERVICE_BINDING

    // DOC:MENU
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 0, 0, R.string.searchLAN).setIcon(android.R.drawable.ic_menu_search);
        // DOC:OPTIONAL
        menu.add(0, 1, 0, R.string.switchRouter).setIcon(android.R.drawable.ic_menu_revert);
        menu.add(0, 2, 0, R.string.toggleDebugLogging).setIcon(android.R.drawable.ic_menu_info_details);
        // DOC:OPTIONAL
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 0:
                if (upnpService == null)
                    break;
                Toast.makeText(this, R.string.searchingLAN, Toast.LENGTH_SHORT).show();
                upnpService.getRegistry().removeAllRemoteDevices();
                upnpService.getControlPoint().search(new UDADeviceTypeHeader(new UDADeviceType("CDPlayer")));;
                break;
            // DOC:OPTIONAL
            case 1:
                if (upnpService != null) {
                    Router router = upnpService.get().getRouter();
                    try {
                        if (router.isEnabled()) {
                            Toast.makeText(this, R.string.disablingRouter, Toast.LENGTH_SHORT).show();
                            router.disable();
                        } else {
                            Toast.makeText(this, R.string.enablingRouter, Toast.LENGTH_SHORT).show();
                            router.enable();
                        }
                    } catch (RouterException ex) {
                        Toast.makeText(this, getText(R.string.errorSwitchingRouter) + ex.toString(), Toast.LENGTH_LONG).show();
                        ex.printStackTrace(System.err);
                    }
                }
                break;
            case 2:
                Logger logger = Logger.getLogger("org.fourthline.cling");
                if (logger.getLevel() != null && !logger.getLevel().equals(Level.INFO)) {
                    Toast.makeText(this, R.string.disablingDebugLogging, Toast.LENGTH_SHORT).show();
                    logger.setLevel(Level.INFO);
                } else {
                    Toast.makeText(this, R.string.enablingDebugLogging, Toast.LENGTH_SHORT).show();
                    logger.setLevel(Level.FINEST);
                }
                break;
            // DOC:OPTIONAL
        }
        return false;
    }
    // DOC:MENU

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {

        controlling_device = ((DeviceDisplay)l.getItemAtPosition(position)).device;
        Service switchPowerService = controlling_device.findService(new UDAServiceId("SwitchPower"));

        if(switchPowerService == null)
            return;

//        Seri<AndroidUpnpService> uPnpServiceSeri = new Seri<AndroidUpnpService>();
//        uPnpServiceSeri.setInstance(upnpService);
//
//        Seri<Device> deviceSeri = new Seri<Device>();
//        deviceSeri.setInstance(controlling_device);
        sevice = switchPowerService;
        boolean failed = false;
        Intent intent = new Intent(BrowserActivity.this, RemotePlayerActivity.class);
        updateVolumeCallback = new SubscriptionCallback(controlling_device.findService(new UDAServiceId("Audio"))) {
            @Override
            protected void failed(GENASubscription genaSubscription, UpnpResponse upnpResponse, Exception e, String s) {

            }

            @Override
            protected void established(GENASubscription genaSubscription) {

            }

            @Override
            protected void ended(GENASubscription genaSubscription, CancelReason cancelReason, UpnpResponse upnpResponse) {
//                Log.d("Death", "True");
                RemotePlayerActivity activity = RemotePlayerActivity.getInstance();
                if (activity != null && cancelReason != null)
                    activity.finish();

            }

            @Override
            protected void eventReceived(GENASubscription genaSubscription) {
                Map<String, StateVariableValue> values = genaSubscription.getCurrentValues();
                StateVariableValue status = values.get("Volume");
//                Log.d("Volume values:", status.toString());
                int volumn = Integer.parseInt(status.toString());
                if(RemotePlayerActivity.getInstance() != null)
                    RemotePlayerActivity.getInstance().volumeBar.setProgress(volumn);
//                Log.d("Event", "Receive");
            }

            @Override
            protected void eventsMissed(GENASubscription genaSubscription, int i) {

            }
        };

        updatePowerCallback = new SubscriptionCallback(switchPowerService) {
            @Override
            protected void failed(GENASubscription genaSubscription, UpnpResponse upnpResponse, Exception e, String s) {

            }

            @Override
            protected void established(GENASubscription genaSubscription) {

            }

            @Override
            protected void ended(GENASubscription genaSubscription, CancelReason cancelReason, UpnpResponse upnpResponse) {
                RemotePlayerActivity activity = RemotePlayerActivity.getInstance();
                if (activity != null && cancelReason != null)
                    activity.finish();
            }

            @Override
            protected void eventReceived(GENASubscription genaSubscription) {
                Map<String, StateVariableValue> values = genaSubscription.getCurrentValues();
                StateVariableValue status = values.get("Status");
                final int status_value = Integer.parseInt(status.toString());
                Log.d("Status", status.toString());
//                Log.d("Volume values:", status.toString());

//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        if(RemotePlayerActivity.getInstance() != null)
//                            RemotePlayerActivity.getInstance().powerBtn
//                                    .setText(status_value == 1 ? "ON" : "OFF");
//                    }
//                });
            }


            @Override
            protected void eventsMissed(GENASubscription genaSubscription, int i) {

            }
        };
//        Bundle bundle = new Bundle();
//        bundle.putSerializable("uPnpService", uPnpServiceSeri);
//        bundle.putSerializable("Device", deviceSeri);
//        intent.putExtra("bundle", bundle);
        upnpService.getControlPoint().execute(updateVolumeCallback);
        upnpService.getControlPoint().execute(updatePowerCallback);
        //bundle.putSerializable("controlling_device", );
        remote_display = true;
        startActivity(intent);
        upnpService.getRegistry().removeAllRemoteDevices();
        upnpService.getControlPoint().search(new UDADeviceTypeHeader(new UDADeviceType("CDPlayer")));

//        AlertDialog dialog = new AlertDialog.Builder(this).create();
//        dialog.setTitle(R.string.deviceDetails);

//        dialog.setMessage(deviceDisplay.getDetailsMessage());
//        dialog.setButton(
//            getString(R.string.OK),
//            new DialogInterface.OnClickListener() {
//                public void onClick(DialogInterface dialog, int which) {
//                }
//            }
//        );
//        dialog.show();
//        TextView textView = (TextView) dialog.findViewById(android.R.id.message);
//        textView.setTextSize(12);
//        super.onListItemClick(l, v, position, id);

//        Device controlling_device = deviceDisplay.controlling_device;
//
//        Service service = controlling_device.findService(new UDAServiceId("SwitchPower"));
//        org.fourthline.cling.model.meta.Action getaction = service.getAction("GetStatus");
//        ActionInvocation getstatusaction = new ActionInvocation(getaction);
////        settargetaction.setInput("newTargetValue", true);
//        new ActionCallback.Default(getstatusaction, upnpService.getControlPoint()).run();
//        boolean result = (Boolean) getstatusaction.getOutput("ResultStatus").getValue();
//
//
//
//        org.fourthline.cling.model.meta.Action action = service.getAction("SetTarget");
//        ActionInvocation settargetaction = new ActionInvocation(action);
//        settargetaction.setInput("newTargetValue", result? false:true);
//        new ActionCallback.Default(settargetaction, upnpService.getControlPoint()).run();

    }

    protected class BrowseRegistryListener extends DefaultRegistryListener {
        private ListActivity activity;
        public BrowseRegistryListener(ListActivity activity)
        {
            this.activity = activity;
        }

        /* Discovery performance optimization for very slow Android devices! */
        @Override
        public void remoteDeviceDiscoveryStarted(Registry registry, RemoteDevice device) {
            if(device.getType().equals(new UDADeviceType("CDPlayer")) && device.isFullyHydrated())
                deviceAdded(device);
        }

        @Override
        public void remoteDeviceDiscoveryFailed(Registry registry, final RemoteDevice device, final Exception ex) {
            runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(
                        BrowserActivity.this,
                        "Discovery failed of '" + device.getDisplayString() + "': "
                            + (ex != null ? ex.toString() : "Couldn't retrieve controlling_device/service descriptors"),
                        Toast.LENGTH_LONG
                    ).show();
                }
            });
            deviceRemoved(device);
            Log.d("Event", "Remove");
        }
        /* End of optimization, you can remove the whole block if your Android handset is fast (>= 600 Mhz) */


        @Override
        public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
            if(device.getType().equals(new UDADeviceType("CDPlayer")) && device.isFullyHydrated())
                deviceAdded(device);
        }

        @Override
        public void remoteDeviceRemoved(Registry registry, RemoteDevice device) {
            deviceRemoved(device);
//            Log.d("Event", "Remove");
        }

        @Override
        public void localDeviceAdded(Registry registry, LocalDevice device) {
            deviceAdded(device);
        }

        @Override
        public void localDeviceRemoved(Registry registry, LocalDevice device) {
            deviceRemoved(device);
        }

        public void deviceAdded(final Device device) {
            runOnUiThread(new Thread(listAdapter, device, activity));
        }

        public void deviceRemoved(final Device device) {
            runOnUiThread(new Runnable() {
                public void run() {
                    listAdapter.remove(new DeviceDisplay(device));
                    if(controlling_device.equals(device) && RemotePlayerActivity.getInstance() != null)
                    {
                        RemotePlayerActivity.getInstance().finish();;
                    }
                }
            });
        }

    }

    public class Thread implements Runnable{
        private ArrayAdapter<BrowserActivity.DeviceDisplay> listAdapter;
        private DeviceDisplay d;
        private ListActivity activity;

        public Thread(ArrayAdapter<BrowserActivity.DeviceDisplay> listAdapter, Device device, ListActivity activity)
        {
            this.listAdapter = listAdapter;
            this.d = new DeviceDisplay(device);
            this.activity = activity;
        }

        @Override
        public void run() {
            int pos = listAdapter.getPosition(d);
            if(pos>=0)
            {
                listAdapter.remove(d);
                listAdapter.insert(d, pos);
            } else {
                listAdapter.add(d);
            }
//            if(d.device.isFullyHydrated())
            new CallListener(activity, d.device);
        }
    }

    protected class DeviceDisplay {

        Device device;

        public DeviceDisplay(Device device) {
            this.device = device;
        }

        public Device getDevice() {
            return device;
        }

        // DOC:DETAILS
        public String getDetailsMessage() {
            StringBuilder sb = new StringBuilder();
            if (getDevice().isFullyHydrated()) {
                sb.append(getDevice().getDisplayString());
                sb.append("\n\n");
                for (Service service : getDevice().getServices()) {
                    sb.append(service.getServiceType()).append("\n");
                }
            } else {
                sb.append(getString(R.string.deviceDetailsNotYetAvailable));
            }
            return sb.toString();
        }
        // DOC:DETAILS

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DeviceDisplay that = (DeviceDisplay) o;
            return device.equals(that.device);
        }

        @Override
        public int hashCode() {
            return device.hashCode();
        }

        @Override
        public String toString() {
            String name =
                getDevice().getDetails() != null && getDevice().getDetails().getFriendlyName() != null
                    ? getDevice().getDetails().getFriendlyName()
                    : getDevice().getDisplayString();
            String udn = device.getIdentity().getUdn().getIdentifierString();
            name = name + " - " + udn.substring(udn.length() - 4);
            // Display a little star while the controlling_device is being loaded (see performance optimization earlier)
            return device.isFullyHydrated() ? name : name + " *";
        }
    }
    // DOC:CLASS_END
    // ...
}
// DOC:CLASS_END
