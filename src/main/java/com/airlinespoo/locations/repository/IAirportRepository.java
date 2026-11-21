package com.airlinespoo.locations.repository;

import com.airlinespoo.core.repository.Repository;
import com.airlinespoo.locations.model.Airport;

import java.util.List;
import java.util.UUID;

public interface IAirportRepository extends Repository<Airport, UUID> {

    List<Airport> listAirportsByCity(int cityId);

    List<Airport> searchAirportsByName(String name);

    long countAirportsByCity(int cityId);

    boolean existsByCityId(int cityId);
}
