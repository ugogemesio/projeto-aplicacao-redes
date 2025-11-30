package uff.redes.iot.dht;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DHTRepository extends JpaRepository<DHT, Long> {
    Optional<DHT> findTopByOrderByDataHoraDesc();

}
