package com.aknopov.wssimulator.proxy.toxy;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Random;

import com.aknopov.wssimulator.Utils;

/**
 * ToxicLatency delays input data by {@code latency ± rand(jitter) }
 */
public class ToxicLatency extends Toxic {
    Random randomizer = new Random(Instant.now().toEpochMilli());

    private final long latencyMs;
    private final long jitterMs;

    /**
     * Creates the instance
     *
     * @param startDelay start delay of toxic
     * @param latency average latency
     * @param jitter latency variance <= latency
     */
    public ToxicLatency(Duration startDelay, Duration latency, Duration jitter) {
        super(startDelay);

        Utils.checkArgument(latency.compareTo(jitter) >= 0, "Average latency is less than jitter");
        this.latencyMs = latency.toMillis();
        this.jitterMs = jitter.toMillis();
    }

    @Override
    public Iterable<ByteBuffer> transformData(ByteBuffer inData) {
        if (canStart()) {
            Duration sleepTime = Duration.ofMillis(latencyMs + randomizer.nextLong(-jitterMs, jitterMs));
            Utils.sleepUnchecked(sleepTime);
        }
        return Collections.singletonList(inData);
    }
}
