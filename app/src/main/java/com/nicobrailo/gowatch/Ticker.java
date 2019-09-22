package com.nicobrailo.gowatch;

import android.os.Handler;

class Ticker extends Handler {
    public interface Callback {
        void onTick();
    }

    private final long delayMs;
    private final Callback self;
    private final Runnable handle = new Runnable() {
        @Override
        public void run() {
            self.onTick();
            Ticker.this.start();
        }
    };

    Ticker(long delayMs, Callback self) {
        this.delayMs = delayMs;
        this.self = self;
    }

    void start() {
        postDelayed(handle, delayMs);
    }

    void stop() {
        removeCallbacks(handle);
    }
}
