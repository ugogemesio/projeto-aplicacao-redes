package uff.redes.iot.dht.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uff.redes.iot.dht.model.DHTStats;
import uff.redes.iot.networkstats.model.NetworkStats;

import java.util.DoubleSummaryStatistics;
import java.util.LinkedList;
import java.util.List;

@Service
public class DHTStatsService {

    private static final int MAX_HISTORY = 100;

    private final List<Double> historicoTemperaturas = new LinkedList<>();


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
}
