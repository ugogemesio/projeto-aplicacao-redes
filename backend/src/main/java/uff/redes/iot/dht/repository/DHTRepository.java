package uff.redes.iot.dht.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uff.redes.iot.dht.model.DHTEntidade;

import java.util.Optional;

@Repository
public interface DHTRepository extends JpaRepository<DHTEntidade, Long> {
    Optional<DHTEntidade> findTopByOrderByDataHoraDesc();

}
