package uff.redes.iot.dht;

public record DHTCreateRequest(
        Double temperatura,
        Double umidade,
        String origem
) {}
