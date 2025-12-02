package uff.redes.iot.dht.model;

//pode evoluir pra uma entidade
public record DHTStats(
        double maxTemp,
        double minTemp,
        double avgTemp
) {}
