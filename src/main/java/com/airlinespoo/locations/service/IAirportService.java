package com.airlinespoo.locations.service;

import com.airlinespoo.locations.model.Airport;

import java.util.List;
import java.util.UUID;

/**
 * Operaciones de negocio para gestionar aeropuertos.
 */
public interface IAirportService {

    Airport createAirport(String name, int cityId);

    Airport getAirport(UUID id);

    List<Airport> listAirports();

    Airport updateAirport(UUID id, String name, int cityId);

    void deleteAirport(UUID id);

    List<Airport> listAirportsByCity(int cityId);

    List<Airport> searchAirportsByName(String name);

    long countAirportsByCity(int cityId);
}
