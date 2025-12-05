package uff.redes.iot.networkstats.model;

import jakarta.persistence.*;

@Entity
@Table(name = "network_stats")
public class NetworkStatsEntidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private double throughput;
    private double jitter;
    private double rtt;

    // Construtor vazio obrigatório para JPA
    public NetworkStatsEntidade() {
    }

    // Construtor completo
    public NetworkStatsEntidade(double throughput, double jitter, double rtt) {
        this.throughput = throughput;
        this.jitter = jitter;
        this.rtt = rtt;
    }

    public Long getId() {
        return id;
    }

    public double getThroughput() {
        return throughput;
    }

    public void setThroughput(double throughput) {
        this.throughput = throughput;
    }

    public double getJitter() {
        return jitter;
    }

    public void setJitter(double jitter) {
        this.jitter = jitter;
    }

    public double getRtt() {
        return rtt;
    }

    public void setRtt(double rtt) {
        this.rtt = rtt;
    }
}
