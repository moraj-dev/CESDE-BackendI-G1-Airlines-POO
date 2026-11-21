package com.airlinespoo.airlines.repository;

import com.airlinespoo.airlines.model.Airline;
import com.airlinespoo.core.repository.Repository;
import java.util.List;
import java.util.Optional;

/** Operaciones que necesitamos para trabajar con las aerolíneas. */
public interface IAirlineRepository extends Repository<Airline, Integer> {
    Optional<Airline> findByName(String name);
    List<Airline> searchByName(String partialName);
}
