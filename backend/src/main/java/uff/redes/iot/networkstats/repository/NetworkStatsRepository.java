package uff.redes.iot.networkstats.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uff.redes.iot.networkstats.model.NetworkStatsEntidade;

@Repository
public interface NetworkStatsRepository extends JpaRepository<NetworkStatsEntidade, Long> {

}
