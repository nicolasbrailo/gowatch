package com.nicobrailo.gowatch;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.wearable.activity.WearableActivity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.time.Duration;
import java.time.Instant;
import java.util.Calendar;


public class MainActivity extends WearableActivity implements View.OnClickListener {

    final long TICK_MS = 400;
    final long BUZZ_PATTERN1_S = 30;
    final long BUZZ_PATTERN2_S = 60;
    private static final long[] BUZZ1_PATTERN = {200};
    private static final long[] BUZZ2_PATTERN = {200, 100, 200};

    Instant timerStart = Calendar.getInstance().getTime().toInstant();
    Instant lastMarkStart = Calendar.getInstance().getTime().toInstant();
    int markCount = 0;

    private boolean buzz1_finished = false;
    private boolean buzz2_finished = false;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean init_from_prev = false;
        try {
            if (savedInstanceState != null) {
                timerStart = Instant.ofEpochSecond(savedInstanceState.getLong("timerStart"));
                lastMarkStart = Instant.ofEpochSecond(savedInstanceState.getLong("lastMarkStart"));
                markCount = savedInstanceState.getInt("markCount");
                buzz1_finished = savedInstanceState.getBoolean("buzz1_finished");
                buzz2_finished = savedInstanceState.getBoolean("buzz2_finished");
                init_from_prev = true;
            }
        } catch (Exception ignored) {
        }

        if (!init_from_prev) {
            timerStart = Calendar.getInstance().getTime().toInstant();
            lastMarkStart = Calendar.getInstance().getTime().toInstant();
            markCount = 0;
            buzz1_finished = false;
            buzz2_finished = false;
        }

        final View v = getLayoutInflater().inflate(R.layout.activity_main, null);
        setContentView(v);
        v.setOnClickListener(this);
        findViewById(R.id.main_activity).setOnClickListener(this);
        findViewById(R.id.current_time).setOnClickListener(this);

        tick_task.run();

        setAmbientEnabled();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putLong("timerStart", timerStart.getEpochSecond());
        savedInstanceState.putLong("lastMarkStart", lastMarkStart.getEpochSecond());
        savedInstanceState.putInt("markCount", markCount);
        savedInstanceState.putBoolean("buzz1_finished", buzz1_finished);
        savedInstanceState.putBoolean("buzz2_finished", buzz2_finished);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        tick_task_handle.removeCallbacks(tick_task);
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
                markTime();
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

        markCount += 1;
        EditText hist = findViewById(R.id.timer_history);
        hist.append(getString(R.string.timer_history_format, markCount, d.minutes, d.seconds, d.millis));
        lastMarkStart = now;

        buzz1_finished = false;
        buzz2_finished = false;
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

        if (d2.seconds_raw > BUZZ_PATTERN1_S && !buzz1_finished) {
            buzz1_finished = true;
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(VibrationEffect.createWaveform(BUZZ1_PATTERN, 0));
        }

        if (d2.seconds_raw > BUZZ_PATTERN2_S && !buzz2_finished) {
            buzz2_finished = true;
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(VibrationEffect.createWaveform(BUZZ2_PATTERN, 0));
        }
    }

    final Handler tick_task_handle  = new Handler();
    final Runnable tick_task = new Runnable() {
        @Override
        public void run() {
            updateClock();
            updateTimer();
            tick_task_handle.postDelayed(tick_task, TICK_MS);
        }
    };
}
