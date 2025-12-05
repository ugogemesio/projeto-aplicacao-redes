package uff.redes.iot.networkstats.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uff.redes.iot.networkstats.model.NetworkStats;
import uff.redes.iot.networkstats.model.NetworkStatsEntidade;
import uff.redes.iot.networkstats.repository.NetworkStatsRepository;
import uff.redes.iot.networkstats.service.NetworkStatsService;

import java.util.List;

@RestController
@RequestMapping("/network/stats")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class NetworkStatsController {

    private final NetworkStatsService service;
    private final NetworkStatsRepository repository;

    /**
     * Retorna métricas atuais (calculadas a partir do histórico em memória)
     */
    @GetMapping
    public ResponseEntity<NetworkStats> getCurrentStats() {
        return ResponseEntity.ok(service.getNetworkStats());
    }



    /**
     * Força salvar as métricas atuais no banco
     */
    @PostMapping("/save")
    public ResponseEntity<NetworkStats> forceSave() {
        NetworkStats saved = service.persistCurrentStats();
        return ResponseEntity.ok(saved);
    }

    /**
     * Retorna o histórico salvo no banco
     */
    @GetMapping("/history")
    public ResponseEntity<List<NetworkStatsEntidade>> getHistory() {
        return ResponseEntity.ok(repository.findAll());
    }
}
