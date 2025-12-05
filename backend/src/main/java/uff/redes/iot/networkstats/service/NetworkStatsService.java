package uff.redes.iot.networkstats.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import uff.redes.iot.networkstats.model.NetworkStats;
import uff.redes.iot.networkstats.model.NetworkStatsEntidade;
import uff.redes.iot.networkstats.repository.NetworkStatsRepository;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class NetworkStatsService {

    private static final int MAX_HISTORY = 1000;

    private final Deque<Long> historicoRTT = new LinkedList<>();
    private final Deque<Long> historicoJitter = new LinkedList<>();
    private final Deque<Integer> historicoBytes = new LinkedList<>();
    private final Deque<Long> historicoTimestamps = new LinkedList<>();

    private final NetworkStatsRepository repository;
    private final AtomicInteger unsavedCount = new AtomicInteger(0);

    public NetworkStatsService(NetworkStatsRepository repository) {
        this.repository = repository;
    }

    /**
     * Registra nova amostra vinda do ESP32.
     * Mantém o histórico limitado e incrementa contador de amostras para persistência.
     */
    public synchronized void registrarAmostra(long rtt, long jitter, int bytes, long timestamp) {

        historicoRTT.addLast(rtt);
        historicoJitter.addLast(jitter);
        historicoBytes.addLast(bytes);
        historicoTimestamps.addLast(timestamp);

        // limita histórico
        while (historicoRTT.size() > MAX_HISTORY) {
            historicoRTT.removeFirst();
            historicoJitter.removeFirst();
            historicoBytes.removeFirst();
            historicoTimestamps.removeFirst();
        }

        // incrementa contador de amostras não salvas
        unsavedCount.incrementAndGet();


        persistCurrentStats(); // salva imediatamente (sincronizado)
        unsavedCount.set(0);

    }

    /**
     * Calcula as métricas atuais com base no histórico em memória.
     */
    public synchronized NetworkStats getNetworkStats() {
        if (historicoRTT.isEmpty()) return new NetworkStats(0, 0, 0);

        double jitterMedia = historicoJitter.stream().mapToLong(Long::longValue).average().orElse(0.0);

        long totalBytes = historicoBytes.stream().mapToLong(Integer::longValue).sum();

        long totalTimeMs = 0;
        if (historicoTimestamps.size() > 1) {
            totalTimeMs = historicoTimestamps.getLast() - historicoTimestamps.getFirst();
        }

        double throughput = totalTimeMs > 0 ? (totalBytes * 1000.0) / totalTimeMs : 0.0;

        long lastRtt = historicoRTT.getLast();

        return new NetworkStats(throughput, jitterMedia, lastRtt);
    }

    /**
     * Persiste as métricas atuais no banco (pode ser chamado periodicamente).
     */
    public synchronized NetworkStats persistCurrentStats() {
        NetworkStats current = getNetworkStats();
        NetworkStatsEntidade entidade = new NetworkStatsEntidade(
                current.throughput(),
                current.jitter(),
                current.rtt()
        );
        NetworkStatsEntidade salvo = repository.save(entidade);
        return new NetworkStats(salvo.getThroughput(), salvo.getJitter(), salvo.getRtt());
    }

}
