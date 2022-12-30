package com.nicobrailo.gowatch;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.time.Instant;
import java.util.Calendar;


public class MainActivity extends WearableActivity implements View.OnClickListener, ServiceConnection, Ticker.Callback {
    private static final long UI_REFRESH_MS = 400;
    private static final long BTN_LONG_PRESS_MS = 700;

    private Instant timerStart;
    private Instant lastMarkStart;
    int markCount = 0;

    private GoWatchHistorySvc timerService = null;
    private GoWatchBuzzSvc buzzService = null;
    private final Ticker ticker = new Ticker(UI_REFRESH_MS, this);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        findViewById(R.id.main_activity).setOnClickListener(this);
        findViewById(R.id.current_time).setOnClickListener(this);

        EditText txt = findViewById(R.id.timer_history);
        txt.setText(getString(R.string.bg_service_starting));

        // Start bg services
        final Intent i = new Intent(this, GoWatchHistorySvc.class);
        startService(i);
        bindService(i, this, 0);

        final Intent j = new Intent(this, GoWatchBuzzSvc.class);
        startService(j);
        bindService(j, this, 0);

        setAmbientEnabled();

        ticker.start();
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        if (iBinder.getClass().equals(GoWatchHistorySvc.SvcBinder.class)) {
            onHistoryServiceConnected((GoWatchHistorySvc.SvcBinder) iBinder);
        } else if (iBinder.getClass().equals(GoWatchBuzzSvc.SvcBinder.class)) {
            onBuzzServiceConnected((GoWatchBuzzSvc.SvcBinder) iBinder);
        } else {
            Log.e(MainActivity.class.getSimpleName(), "Can't start background service");
        }
    }

    void onHistoryServiceConnected(GoWatchHistorySvc.SvcBinder b) {
        timerService = b.getService();

        timerStart = timerService.getTimerStart();
        lastMarkStart = timerService.getLastMark();
        markCount = timerService.getMarkCount();

        if (buzzService != null) {
            buzzService.resetWithLastMarkTime(timerStart);
        }

        EditText txt = findViewById(R.id.timer_history);
        txt.setText(timerService.getHistory());
    }

    void onBuzzServiceConnected(GoWatchBuzzSvc.SvcBinder b) {
        buzzService = b.getService();

        if (timerService != null) {
            buzzService.resetWithLastMarkTime(timerStart);
        } else {
            buzzService.reset();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ticker.stop();

        if (timerService != null) {
            timerService.setTimerStart(timerStart);
            timerService.setLastMark(lastMarkStart);
            timerService.setMarkCount(markCount);

            EditText hist = findViewById(R.id.timer_history);
            timerService.setHistory(hist.getText().toString());

            timerService = null;
            unbindService(this);
        }

        if (buzzService != null) {
            buzzService.stop();
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        timerService = null;
        buzzService = null;
        ticker.stop();
    }

    private void resetTimer() {
        timerService.reset();
        buzzService.reset();
        timerStart = timerService.getTimerStart();
        lastMarkStart = timerService.getLastMark();
        markCount = timerService.getMarkCount();
        ((EditText)findViewById(R.id.timer_history)).setText("");
    }

    @Override
    public void onClick(View view) {
        markTime();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_STEM_1:
            case KeyEvent.KEYCODE_STEM_2:
                if (event.getEventTime() - event.getDownTime() > BTN_LONG_PRESS_MS) {
                    resetTimer();
                } else {
                    markTime();
                }
                return true;
            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    private void markTime() {
        final Instant now = Calendar.getInstance().getTime().toInstant();
        final Delta d = new Delta(now, lastMarkStart);

        // Debounce
        if (d.seconds == 0) return;

        // Update new
        lastMarkStart = now;

        // Update UI
        markCount += 1;
        EditText hist = findViewById(R.id.timer_history);
        hist.append(getString(R.string.timer_history_format, markCount, d.minutes, d.seconds, d.millis));

        buzzService.reset();
    }

    @Override
    public void onTick() {
        updateClock();
        updateTimer();
    }

    private void updateClock() {
        final Calendar cal = Calendar.getInstance();
        final int hrs = cal.get(Calendar.HOUR_OF_DAY);
        final int min = cal.get(Calendar.MINUTE);

        TextView mTextView = findViewById(R.id.clock_time);
        mTextView.setText(getString(R.string.watch_time_format, hrs, min));
    }

    private void updateTimer() {
        final Delta d = new Delta(Calendar.getInstance().getTime().toInstant(), timerStart);
        TextView t1 = findViewById(R.id.current_time);
        t1.setText(getString(R.string.current_time_format, d.minutes, d.seconds));

        final Delta d2 = new Delta(Calendar.getInstance().getTime().toInstant(), lastMarkStart);
        TextView t2 = findViewById(R.id.current_mark);
        t2.setText(getString(R.string.current_mark_format, d2.minutes, d2.seconds, d.millis));
    }
}
