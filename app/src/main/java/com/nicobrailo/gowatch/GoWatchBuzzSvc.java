package com.nicobrailo.gowatch;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;

import java.time.Instant;
import java.util.Calendar;

public class GoWatchBuzzSvc extends Service implements Ticker.Callback {
    final long BUZZ_DELTA_MS = 30 * 1000;
    final long BASE_BUZZ_LEN_MS = 100;
    final long MAX_BUZZ_LEN_MS = 800;

    Ticker ticker = new Ticker(BUZZ_DELTA_MS, this);
    private int buzzCount = 1;

    @Override
    public void onTick() {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        long buzz_length_ms = (BASE_BUZZ_LEN_MS * buzzCount) / 2;
        if (buzz_length_ms > MAX_BUZZ_LEN_MS) buzz_length_ms = MAX_BUZZ_LEN_MS;
        final long[] BUZZ_PATTERN = {0, buzz_length_ms};
        v.vibrate(VibrationEffect.createWaveform(BUZZ_PATTERN, -1));
        buzzCount += 1;
    }

    public void stop() {
        ticker.stop();
    }

    public void reset() {
        buzzCount = 1;
        ticker.start();
    }

    /**
     * Foce a last mark - next buzz will be in BUZZ_DELTA - (now() - timerstart)
     */
    public void resetWithLastMarkTime(Instant timerStart) {
        final Instant now = Calendar.getInstance().getTime().toInstant();
        final Delta d = new Delta(now, timerStart);
        long next_buzz = BUZZ_DELTA_MS - (d.seconds * 1000);
        if (next_buzz < 1000) next_buzz = BUZZ_DELTA_MS;
        ticker.startWithDifferentDeltaForFirst(next_buzz);
    }

    class SvcBinder extends Binder {
        GoWatchBuzzSvc getService() {
            return GoWatchBuzzSvc.this;
        }
    }

    IBinder svcBinder = new SvcBinder();

    @Override
    public void onCreate() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return svcBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_NOT_STICKY;
    }
}
