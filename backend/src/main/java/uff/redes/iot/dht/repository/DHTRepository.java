package uff.redes.iot.dht.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uff.redes.iot.dht.model.DHT;

import java.util.Optional;

public interface DHTRepository extends JpaRepository<DHT, Long> {
    Optional<DHT> findTopByOrderByDataHoraDesc();

}
