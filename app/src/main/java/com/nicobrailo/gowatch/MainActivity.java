package com.nicobrailo.gowatch;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
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
    final long BUZZ_DELTA_S = 30;
    final long BASE_BUZZ_LEN_MS = 100;
    final long MAX_BUZZ_LEN_MS = 800;

    private Instant timerStart;
    private Instant lastMarkStart;
    int markCount = 0;
    private int buzzCount = 0;

    private GoWatchHistorySvc timerService = null;
    private Ticker ticker = new Ticker(UI_REFRESH_MS, this);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        findViewById(R.id.main_activity).setOnClickListener(this);
        findViewById(R.id.current_time).setOnClickListener(this);

        EditText txt = findViewById(R.id.timer_history);
        txt.setText(getString(R.string.bg_service_starting));

        // Start bg service
        final Intent i = new Intent(this, GoWatchHistorySvc.class);
        startService(i);
        bindService(i, this, 0);

        setAmbientEnabled();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        GoWatchHistorySvc.SvcBinder b = (GoWatchHistorySvc.SvcBinder) iBinder;
        if (b == null) {
            Log.e(MainActivity.class.getSimpleName(), "Can't start background service");
            return;
        }

        timerService = b.getService();

        timerStart = timerService.getTimerStart();
        lastMarkStart = timerService.getLastMark();
        markCount = timerService.getMarkCount();

        EditText txt = findViewById(R.id.timer_history);
        txt.setText(timerService.getHistory());

        ticker.start();
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
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        timerService = null;
        ticker.stop();
    }

    private void resetTimer() {
        timerService.reset();
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
        buzzCount = 0;
        EditText hist = findViewById(R.id.timer_history);
        hist.append(getString(R.string.timer_history_format, markCount, d.minutes, d.seconds, d.millis));
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

        // TODO: Need to save buzzes to svc too
        // TODO: Seems to drift on bg, move to svc entirely?
        final int expected_buzzes = (int) (d2.seconds_raw / BUZZ_DELTA_S);
        if (expected_buzzes > buzzCount) {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            long buzz_length_ms = (BASE_BUZZ_LEN_MS * expected_buzzes) / 2;
            if (buzz_length_ms > MAX_BUZZ_LEN_MS) buzz_length_ms = MAX_BUZZ_LEN_MS;
            final long[] BUZZ_PATTERN = {0, buzz_length_ms};
            v.vibrate(VibrationEffect.createWaveform(BUZZ_PATTERN, -1));
            buzzCount = expected_buzzes;
        }
    }
}
