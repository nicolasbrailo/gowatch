package com.nicobrailo.gowatch;

import java.time.Duration;
import java.time.Instant;

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
