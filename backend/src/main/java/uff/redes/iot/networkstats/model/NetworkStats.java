package uff.redes.iot.networkstats.model;

public record NetworkStats(
        double throughput,  // em bps
        double jitter,      // em ms
        long currentRtt,    // em ms
        double packetLossRate, // em %
        String deviceId
) {}