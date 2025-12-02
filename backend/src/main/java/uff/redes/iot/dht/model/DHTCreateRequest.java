package uff.redes.iot.dht.model;

public record DHTCreateRequest(
        Double temperatura,
        Double umidade,
        String origem
) {}
