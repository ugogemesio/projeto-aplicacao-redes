package uff.redes.iot.dht.service;

import org.springframework.stereotype.Service;
import uff.redes.iot.dht.model.DHTStats;
import uff.redes.iot.networkstats.NetworkStats;

import java.util.DoubleSummaryStatistics;
import java.util.LinkedList;
import java.util.List;

@Service
public class DHTStatsService {

    private static final int MAX_HISTORY = 100;

    // Histórico de RTTs, jitter, bytes e temperaturas
    private final List<Long> historicoRTT = new LinkedList<>();
    private final List<Long> historicoJitter = new LinkedList<>();
    private final List<Integer> historicoBytes = new LinkedList<>();
    private final List<Double> historicoTemperaturas = new LinkedList<>();
    private final List<Long> historicoTimestamps = new LinkedList<>();

    /**
     * Adiciona um novo registro de RTT, Jitter e quantidade de bytes
     */
    public synchronized void addRTT(long rtt, long jitter, int bytes) {
        if (historicoRTT.size() >= MAX_HISTORY) {
            historicoRTT.remove(0);
            historicoJitter.remove(0);
            historicoBytes.remove(0);
            historicoTimestamps.remove(0);
        }
        historicoRTT.add(rtt);
        historicoJitter.add(jitter);
        historicoBytes.add(bytes);
        historicoTimestamps.add(System.currentTimeMillis());
    }

    /**
     * Adiciona uma nova temperatura ao histórico
     */
    public synchronized void addTemperatura(double temperatura) {
        if (historicoTemperaturas.size() >= MAX_HISTORY) {
            historicoTemperaturas.remove(0);
        }
        historicoTemperaturas.add(temperatura);
    }

    /**
     * Retorna estatísticas de temperatura
     */
    public synchronized DHTStats getStats() {
        if (historicoTemperaturas.isEmpty()) {
            return new DHTStats(0, 0, 0);
        }

        DoubleSummaryStatistics stats = historicoTemperaturas
                .stream()
                .mapToDouble(Double::doubleValue)
                .summaryStatistics();

        return new DHTStats(
                stats.getMax(),
                stats.getMin(),
                stats.getAverage()
        );
    }

    /**
     * Retorna estatísticas de rede: throughput e jitter médio
     */
    public synchronized NetworkStats getNetworkStats() {
        if (historicoRTT.isEmpty()) return new NetworkStats(0, 0);

        // Jitter médio
        double totalJitter = historicoJitter.stream().mapToLong(Long::longValue).sum();
        double jitterMedia = totalJitter / historicoJitter.size();

        // Throughput em bytes/seg
        long totalBytes = historicoBytes.stream().mapToLong(Integer::longValue).sum();
        long totalTimeMs = 0;

        if (historicoTimestamps.size() > 1) {
            totalTimeMs = historicoTimestamps.get(historicoTimestamps.size() - 1)
                    - historicoTimestamps.get(0);
        }

        double throughput = totalTimeMs > 0 ? (totalBytes * 1000.0) / totalTimeMs : 0;

        return new NetworkStats(throughput, jitterMedia);
    }
}
