package com.nicobrailo.gowatch;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.time.Instant;
import java.util.Calendar;

public class GoWatchHistorySvc extends Service {

    private static final long MAX_TIME_HISTORY_RETAIN_SEC = 60 * 15;

    private Instant timerStart = null;
    private Instant lastMarkStart = null;
    private int markCount = 0;
    private String history =  null;


    public Instant getTimerStart() {
        return timerStart;
    }

    public Instant getLastMark() {
        return lastMarkStart;
    }

    public String getHistory() {
        return history;
    }

    public void setTimerStart(Instant timerStart) {
        this.timerStart = timerStart;
    }

    public void setLastMark(Instant lastMarkStart) {
        this.lastMarkStart = lastMarkStart;
    }

    public void setHistory(String history) {
        this.history = history;
    }

    public void reset() {
        timerStart = Calendar.getInstance().getTime().toInstant();
        lastMarkStart = Calendar.getInstance().getTime().toInstant();
        markCount = 0;
        history = "";
    }

    public int getMarkCount() {
        return markCount;
    }

    public void setMarkCount(int markCount) {
        this.markCount = markCount;
    }


    class SvcBinder extends Binder {
        GoWatchHistorySvc getService() {
            return GoWatchHistorySvc.this;
        }
    }

    IBinder svcBinder = new SvcBinder();

    @Override
    public void onCreate() {
        reset();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // If the UI spent too long in the bg, just start from zero
        final Instant now = Calendar.getInstance().getTime().toInstant();
        Delta last_init = new Delta(getTimerStart(), now);
        if (last_init.seconds_raw > MAX_TIME_HISTORY_RETAIN_SEC) {
            Log.i("GoWatch", "Long time elapsed: resetting timer");
            reset();
        }

        return svcBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_NOT_STICKY;
    }
}
