package uff.redes.iot.dht.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import uff.redes.iot.dht.model.DHTCreateRequest;
import uff.redes.iot.dht.model.DHTResponse;
import uff.redes.iot.dht.service.DHTService;


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


    @GetMapping("/ultimo")
    @ResponseBody
    public DHTResponse ultimo() {
        //return service.ultimo();
        return null;
    }
}
