package com.nicobrailo.gowatch;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import java.time.Instant;
import java.util.Calendar;

public class GoWatchTimerSvc extends Service {

    private Instant timerStart = null;
    private Instant lastMarkStart = null;
    private int markCount = 0;
    private String history =  null;
    private boolean needsReinit = true;

    private void initAll() {
        if (needsReinit) {
            timerStart = Calendar.getInstance().getTime().toInstant();
            lastMarkStart = Calendar.getInstance().getTime().toInstant();
            markCount = 0;
            history = "";
            needsReinit = false;
        }
    }

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

    public void shutdown() {
        needsReinit = true;
    }

    public int getMarkCount() {
        return markCount;
    }

    public void setMarkCount(int markCount) {
        this.markCount = markCount;
    }


    class SvcBinder extends Binder {
        GoWatchTimerSvc getService() {
            return GoWatchTimerSvc.this;
        }
    }

    IBinder svcBinder = new SvcBinder();

    @Override
    public void onCreate() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        initAll();
        return svcBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_NOT_STICKY;
    }
}
