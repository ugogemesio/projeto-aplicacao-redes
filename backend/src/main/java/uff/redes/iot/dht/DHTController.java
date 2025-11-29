package uff.redes.iot.dht;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dht")
@CrossOrigin
public class DHTController {

    private final DHTRepository repository;

    @PostMapping
    public ResponseEntity<String> salvar(@RequestParam Double temperatura,
                                         @RequestParam Double umidade,
                                         @RequestParam(defaultValue = "ESP32") String origem) {

        DHT entidade = new DHT();
        entidade.setTemperatura(temperatura);
        entidade.setUmidade(umidade);
        entidade.setOrigem(origem);
        entidade.setDataHora(LocalDateTime.now().toString());

        repository.save(entidade);

        return ResponseEntity.ok("Dados recebidos!");
    }

    @GetMapping
    public ResponseEntity<?> listar() {
        return ResponseEntity.ok(repository.findAll());
    }
}
