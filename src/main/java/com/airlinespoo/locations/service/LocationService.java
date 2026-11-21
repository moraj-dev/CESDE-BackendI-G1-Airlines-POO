package com.airlinespoo.locations.service;

import com.airlinespoo.locations.model.Airport;
import java.util.List;
import java.util.UUID;

/** Une las consultas de aeropuertos que necesita el módulo de vuelos. */
public class LocationService {
    private final IAirportService airports;
    public LocationService(IAirportService airports) { this.airports = airports; }
    public Airport getAirport(UUID id) { return airports.getAirport(id); }
    public List<Airport> listAirports() { return airports.listAirports(); }
}
