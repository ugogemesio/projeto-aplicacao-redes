package uff.redes.iot.dht.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import uff.redes.iot.dht.model.DHTStats;
import uff.redes.iot.dht.service.DHTStatsService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dht/stats")
@CrossOrigin
public class DHTStatsController {

    private final DHTStatsService statsService;

    @GetMapping
    public DHTStats getStats() {
        return statsService.getStats();
    }
}
