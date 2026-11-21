package com.airlinespoo.locations.repository;

import com.airlinespoo.core.repository.InMemoryRepository;
import com.airlinespoo.locations.model.Airport;
import java.util.List;
import java.util.UUID;

/** Aeropuertos en memoria; mantiene el contrato del repositorio original. */
public class InMemoryAirportRepository extends InMemoryRepository<Airport, UUID> implements IAirportRepository {
    @Override protected String entityName() { return "aeropuerto"; }
    @Override public List<Airport> listAirportsByCity(int cityId) {
        return findAll().stream().filter(a -> a.getCity().getId() == cityId).toList();
    }
    @Override public List<Airport> searchAirportsByName(String name) {
        return findAll().stream().filter(a -> a.getName().toLowerCase().contains(name.toLowerCase())).toList();
    }
    @Override public long countAirportsByCity(int cityId) { return listAirportsByCity(cityId).size(); }
    @Override public boolean existsByCityId(int cityId) { return countAirportsByCity(cityId) > 0; }
}
