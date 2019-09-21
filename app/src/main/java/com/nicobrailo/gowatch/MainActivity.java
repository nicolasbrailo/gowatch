package com.nicobrailo.gowatch;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.time.Duration;
import java.time.Instant;
import java.util.Calendar;


public class MainActivity extends WearableActivity implements View.OnClickListener, ServiceConnection {

    private static final long UI_REFRESH_MS = 400;
    private static final long BTN_CLOSE_HOLD_TIME_MS = 700;
    final long BUZZ_DELTA_S = 30;
    final long BASE_BUZZ_LEN_MS = 100;
    final long MAX_BUZZ_LEN_MS = 800;

    private Instant timerStart;
    private Instant lastMarkStart;
    int markCount = 0;
    private int buzzCount = 0;

    private GoWatchTimerSvc timerService = null;
    private Ticker ticker;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final View v = getLayoutInflater().inflate(R.layout.activity_main, null);
        setContentView(v);
        // setContentView(R.layout.activity_main);
        v.setOnClickListener(this);
        findViewById(R.id.main_activity).setOnClickListener(this);
        findViewById(R.id.current_time).setOnClickListener(this);

        EditText txt = findViewById(R.id.timer_history);
        txt.setText(getString(R.string.bg_service_starting));

        // Start bg service
        final Intent i = new Intent(this, GoWatchTimerSvc.class);
        startService(i);
        bindService(i, this, 0);

        // TODO What's this?
        // setAmbientEnabled();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        GoWatchTimerSvc.SvcBinder b = (GoWatchTimerSvc.SvcBinder) iBinder;
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

        ticker = new Ticker(UI_REFRESH_MS, this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ticker.stopTicking();

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
        ticker.stopTicking();
    }

    private void onShutdownRequested() {
        timerService.shutdown();
        onDestroy();
        finish();
    }

    @Override
    public void onClick(View view) {
        markTime();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.e("XXXXXXX", String.valueOf(keyCode));
        Log.e("XXXX", event.toString());
        switch (keyCode) {
            case KeyEvent.KEYCODE_STEM_1:
            case KeyEvent.KEYCODE_STEM_2:
                if (event.getEventTime() - event.getDownTime() > BTN_CLOSE_HOLD_TIME_MS) {
                    onShutdownRequested();
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

    private void onTick() {
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


    static class Ticker extends Handler {
        final long delayMs;
        private final MainActivity self;
        boolean stop = false;

        Ticker(long delayMs, MainActivity self) {
            this.delayMs = delayMs;
            this.self = self;
            this.handleMessage(null);
        }

        void stopTicking() {
            stop = true;
        }

        @Override
        public void handleMessage(Message message) {
            if (!stop) {
                self.onTick();
                sendEmptyMessageDelayed(0, delayMs);
            }
        }
    }

    class Delta {
        final long minutes;
        final long seconds;
        final int millis;

        final long seconds_raw;

        Delta(Instant end, Instant start) {
            final Duration delta = Duration.between(start, end);
            this.minutes = delta.getSeconds() / 60;
            this.seconds = delta.getSeconds() % 60;
            this.millis = delta.getNano() / (int) 1e6;
            this.seconds_raw = delta.getSeconds();
        }
    }
}
