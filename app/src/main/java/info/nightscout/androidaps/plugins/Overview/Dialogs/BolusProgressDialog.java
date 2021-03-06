package info.nightscout.androidaps.plugins.Overview.Dialogs;


import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.squareup.otto.Subscribe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.nightscout.androidaps.MainApp;
import info.nightscout.androidaps.R;
import info.nightscout.androidaps.interfaces.PumpInterface;
import info.nightscout.androidaps.plugins.Overview.events.EventOverviewBolusProgress;
import info.nightscout.androidaps.plugins.DanaR.events.EventDanaRBolusStart;
import info.nightscout.androidaps.plugins.DanaR.events.EventDanaRConnectionStatus;

public class BolusProgressDialog extends DialogFragment implements View.OnClickListener {
    private static Logger log = LoggerFactory.getLogger(BolusProgressDialog.class);
    Button stopButton;
    TextView statusView;
    TextView stopPressedView;
    ProgressBar progressBar;

    static double amount;
    public static boolean bolusEnded = false;
    public static boolean running = true;

    boolean started = false;

    public BolusProgressDialog() {
        super();
    }

    public BolusProgressDialog(double amount) {
        this.amount = amount;
        bolusEnded = false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getDialog().setTitle(String.format(MainApp.sResources.getString(R.string.overview_bolusprogress_goingtodeliver), amount));
        View view = inflater.inflate(R.layout.overview_bolusprogress_dialog, container, false);
        stopButton = (Button) view.findViewById(R.id.overview_bolusprogress_stop);
        statusView = (TextView) view.findViewById(R.id.overview_bolusprogress_status);
        stopPressedView = (TextView) view.findViewById(R.id.overview_bolusprogress_stoppressed);
        progressBar = (ProgressBar) view.findViewById(R.id.overview_bolusprogress_progressbar);

        stopButton.setOnClickListener(this);
        progressBar.setMax(100);
        statusView.setText(MainApp.sResources.getString(R.string.waitingforpump));

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        MainApp.bus().register(this);
        running = true;
        if (bolusEnded) dismiss();
    }

    @Override
    public void onPause() {
        super.onPause();
        MainApp.bus().unregister(this);
        running = false;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.overview_bolusprogress_stop:
                log.debug("Stop bolus delivery button pressed");
                stopPressedView.setVisibility(View.VISIBLE);
                PumpInterface pump = MainApp.getConfigBuilder();
                pump.stopBolusDelivering();
                break;
        }
    }

    @Subscribe
    public void onStatusEvent(final EventOverviewBolusProgress ev) {
        Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    log.debug("Status: " + ev.status + " Percent: " + ev.percent);
                    statusView.setText(ev.status);
                    progressBar.setProgress(ev.percent);
                    if (ev.percent == 100) {
                        stopButton.setVisibility(View.INVISIBLE);
                        scheduleDismiss();
                    }
                }
            });
        }
    }

    @Subscribe
    public void onStatusEvent(final EventDanaRBolusStart ev) {
        started = true;
    }

    @Subscribe
    public void onStatusEvent(final EventDanaRConnectionStatus c) {

        Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            if (c.sStatus == c.CONNECTING) {
                                statusView.setText(String.format(getString(R.string.danar_history_connectingfor), c.sSecondsElapsed));
                            } else if (c.sStatus == c.CONNECTED) {
                                statusView.setText(MainApp.sResources.getString(R.string.connected));
                            } else {
                                statusView.setText(MainApp.sResources.getString(R.string.disconnected));
                                if (started) scheduleDismiss();
                            }
                        }
                    }
            );
        }

    }

    private void scheduleDismiss() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Activity activity = getActivity();
                if (activity != null) {
                    activity.runOnUiThread(
                            new Runnable() {
                                @Override
                                public void run() {
                                    dismiss();
                                }
                            });
                }
            }
        });
        t.start();
    }
}
