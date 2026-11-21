package com.airlinespoo.flights.repository;

import com.airlinespoo.core.repository.InMemoryRepository;
import com.airlinespoo.flights.model.Flight;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class FlightRepository extends InMemoryRepository<Flight, Integer> {
    @Override protected String entityName() { return "vuelo"; }

    /** SELECT ... WHERE FlightName = ? (el código ya viene normalizado). */
    public Optional<Flight> findByFlightName(String flightName) {
        for (Flight flight : values()) {
            if (flight.getFlightName().equals(flightName)) {
                return Optional.of(flight);
            }
        }
        return Optional.empty();
    }

    /** SELECT ... WHERE AirlineId = ? */
    public List<Flight> findByAirline(int airlineId) {
        List<Flight> result = new ArrayList<>();
        for (Flight flight : values()) {
            if (flight.getAirline().getId() == airlineId) {
                result.add(flight);
            }
        }
        return result;
    }

    /** SELECT ... WHERE DateDeparture BETWEEN ? AND ? (ambos extremos incluidos). */
    public List<Flight> findByDepartureDateBetween(LocalDate start, LocalDate end) {
        List<Flight> result = new ArrayList<>();
        for (Flight flight : values()) {
            LocalDate date = flight.getDepartureDate();
            if (!date.isBefore(start) && !date.isAfter(end)) {
                result.add(flight);
            }
        }
        return result;
    }

    /** SELECT ... WHERE Departure = ? AND Arrival = ? */
    public List<Flight> findByRoute(UUID departureId, UUID arrivalId) {
        List<Flight> result = new ArrayList<>();
        for (Flight flight : values()) {
            if (flight.getDeparture().getId().equals(departureId)
                    && flight.getArrival().getId().equals(arrivalId)) {
                result.add(flight);
            }
        }
        return result;
    }

    /** SELECT COUNT(*) ... WHERE AirlineId = ? */
    public int countByAirline(int airlineId) {
        return findByAirline(airlineId).size();
    }

    /** SELECT 1 ... WHERE AirlineId = ? */
    public boolean existsByAirline(int airlineId) {
        for (Flight flight : values()) {
            if (flight.getAirline().getId() == airlineId) {
                return true;
            }
        }
        return false;
    }

    /** SELECT 1 ... WHERE Departure = ? OR Arrival = ? */
    public boolean existsByAirport(UUID airportId) {
        for (Flight flight : values()) {
            if (flight.getDeparture().getId().equals(airportId)
                    || flight.getArrival().getId().equals(airportId)) {
                return true;
            }
        }
        return false;
    }
}
