package uff.redes.iot.dht;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DHTService {

    private final DHTRepository repository;

    public DHTResponse salvar(DHTCreateRequest request) {
        DHT entidade = new DHT();
        entidade.setTemperatura(request.temperatura());
        entidade.setUmidade(request.umidade());
        entidade.setOrigem(request.origem());
        entidade.setDataHora(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        DHT salvo = repository.save(entidade);

        return new DHTResponse(
                salvo.getId(),
                salvo.getTemperatura(),
                salvo.getUmidade(),
                salvo.getOrigem(),
                salvo.getDataHora()
        );
    }
    public DHTResponse buscarUltimo() {
        return repository.findTopByOrderByDataHoraDesc()
                .map(salvo -> new DHTResponse(
                        salvo.getId(),
                        salvo.getTemperatura(),
                        salvo.getUmidade(),
                        salvo.getOrigem(),
                        salvo.getDataHora()
                ))
                .orElse(null);
    }

    public List<DHTResponse> listarTodos() {
        return repository.findAll().stream().map(salvo ->
                new DHTResponse(
                        salvo.getId(),
                        salvo.getTemperatura(),
                        salvo.getUmidade(),
                        salvo.getOrigem(),
                        salvo.getDataHora()
                )
        ).toList();
    }
}
