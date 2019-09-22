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
        stop();
        postDelayed(handle, delayMs);
    }

    void startWithDifferentDeltaForFirst(long firstDelayMs) {
        postDelayed(handle, firstDelayMs);
    }

    void stop() {
        removeCallbacks(handle);
    }
}
