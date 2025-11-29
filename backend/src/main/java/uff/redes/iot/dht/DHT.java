package uff.redes.iot.dht;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class DHT {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double temperatura;
    private Double umidade;
    private String origem;
    private String dataHora;
}
