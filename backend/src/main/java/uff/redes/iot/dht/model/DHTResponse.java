package uff.redes.iot.dht.model;

public record DHTResponse(
        Long id,
        Double temperatura,
        Double umidade,
        String origem,
        String dataHora
) {}
