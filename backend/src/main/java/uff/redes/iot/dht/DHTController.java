package uff.redes.iot.dht;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dht")
@CrossOrigin
public class DHTController {

    private final DHTService service;
    @PostMapping
    public ResponseEntity<String> salvar(@RequestBody DHTCreateRequest dto) {
        service.salvar(dto);
        return ResponseEntity.ok("Dados recebidos!");
    }

    @GetMapping
    public ResponseEntity<?> listar() {
        return ResponseEntity.ok(service.listarTodos());
    }
}
