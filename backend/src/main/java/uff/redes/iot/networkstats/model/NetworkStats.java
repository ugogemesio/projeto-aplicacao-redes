package uff.redes.iot.networkstats.model;


public record NetworkStats(
        double throughput,
        double jitter,
        double rtt
) {}
