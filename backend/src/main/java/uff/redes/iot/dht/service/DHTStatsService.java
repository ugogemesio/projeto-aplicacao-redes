package uff.redes.iot.dht.service;

import org.springframework.stereotype.Service;
import uff.redes.iot.dht.model.DHTStats;

import java.util.DoubleSummaryStatistics;
import java.util.LinkedList;
import java.util.List;
import uff.redes.iot.networkstats.NetworkStats;

@Service
public class DHTStatsService {

    private static final int MAX_HISTORY = 100;
    private final List<Double> historicoTemperaturas = new LinkedList<>();



    private final List<Long> historicoRTT = new LinkedList<>();
    private final List<Integer> historicoBytes = new LinkedList<>();
    public synchronized void addTemperatura(double temperatura) {
        if (historicoTemperaturas.size() >= MAX_HISTORY) {
            historicoTemperaturas.remove(0);
        }
        historicoTemperaturas.add(temperatura);
    }
    private final List<Long> historicoTimestamps = new LinkedList<>();

    public synchronized void addRTT(long rtt, int bytes) {
        if (historicoRTT.size() >= MAX_HISTORY) {
            historicoRTT.remove(0);
            historicoBytes.remove(0);
            historicoTimestamps.remove(0);
        }
        historicoRTT.add(rtt);
        historicoBytes.add(bytes);
        historicoTimestamps.add(System.currentTimeMillis()); // hora que chegou
    }

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

    public synchronized NetworkStats getNetworkStats() {

        System.out.println("🔍 DEBUG - Tamanhos das listas:");
        System.out.println("  - historicoRTT: " + historicoRTT.size());
        System.out.println("  - historicoBytes: " + historicoBytes.size());
        System.out.println("  - historicoTimestamps: " + historicoTimestamps.size());

        if (historicoTimestamps.size() > 1) {
            System.out.println("  - Primeiro timestamp: " + historicoTimestamps.get(0));
            System.out.println("  - Último timestamp: " + historicoTimestamps.get(historicoTimestamps.size() - 1));
            System.out.println("  - Diferença (ms): " +
                    (historicoTimestamps.get(historicoTimestamps.size() - 1) - historicoTimestamps.get(0)));
        }
        if (historicoRTT.size() < 2) {
            return new NetworkStats(0, 0);
        }

        // Cálculo do Jitter (média das variações de RTT)
        double totalDiff = 0;
        for (int i = 1; i < historicoRTT.size(); i++) {
            totalDiff += Math.abs(historicoRTT.get(i) - historicoRTT.get(i - 1));
        }
        double jitter = totalDiff / (historicoRTT.size() - 1);

        // Cálculo do Throughput - corrigido para evitar divisão por zero
        long totalBytes = historicoBytes.stream().mapToLong(Integer::longValue).sum();
        long totalTimeMs = 0;

        if (historicoTimestamps.size() > 1) {
            totalTimeMs = historicoTimestamps.get(historicoTimestamps.size() - 1)
                    - historicoTimestamps.get(0);
        }

        double throughput = 0;
        if (totalTimeMs > 0) {
            throughput = (totalBytes * 1000.0) / totalTimeMs; // bytes por segundo
        }

        return new NetworkStats(throughput, jitter);
    }
}
