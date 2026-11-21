package com.airlinespoo.flights.service;

import com.airlinespoo.airlines.model.Airline;
import com.airlinespoo.airlines.service.AirlineService;
import com.airlinespoo.core.util.IdGenerator;
import com.airlinespoo.core.util.Validator;
import com.airlinespoo.core.exception.DuplicateException;
import com.airlinespoo.core.exception.NotFoundException;
import com.airlinespoo.core.exception.ValidationException;
import com.airlinespoo.flights.model.Flight;
import com.airlinespoo.flights.model.FlightReportRow;
import com.airlinespoo.flights.repository.FlightRepository;
import com.airlinespoo.locations.model.Airport;
import com.airlinespoo.locations.service.LocationService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;


public class FlightService {

    private static final int MIN_REPORT_YEAR = 2000;
    private static final int MAX_REPORT_YEAR = 2100;

    private final FlightRepository flightRepository;
    private final LocationService locationService;
    private final AirlineService airlineService;
    private final IdGenerator idGenerator;

    public FlightService(FlightRepository flightRepository, LocationService locationService,
                         AirlineService airlineService, IdGenerator idGenerator) {
        this.flightRepository = flightRepository;
        this.locationService = locationService;
        this.airlineService = airlineService;
        this.idGenerator = idGenerator;
    }


    public Flight createFlight(String flightName, Airport departure, Airport arrival,
                               LocalDate departureDate, LocalTime departureTime,
                               LocalDate arrivalDate, LocalTime arrivalTime, Airline airline) {
        String normalizedName = Flight.normalizeFlightName(flightName);
        if (flightRepository.findByFlightName(normalizedName).isPresent()) {
            throw new DuplicateException("Ya existe un vuelo con el código " + normalizedName + ".");
        }
        Flight flight = new Flight(idGenerator.next(), normalizedName,
                resolveAirport(departure, "aeropuerto de salida"),
                resolveAirport(arrival, "aeropuerto de llegada"),
                departureDate, departureTime, arrivalDate, arrivalTime,
                resolveAirline(airline));
        flightRepository.create(flight);
        return flight;
    }

    /** SELECT ... WHERE FlightId = ? */
    public Flight getFlight(int id) {
        return flightRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No existe un vuelo con id " + id + "."));
    }

    /** SELECT ... WHERE FlightName = ? */
    public Flight getFlightByName(String flightName) {
        String normalizedName = Flight.normalizeFlightName(flightName);
        return flightRepository.findByFlightName(normalizedName)
                .orElseThrow(() -> new NotFoundException("No existe un vuelo con el código " + normalizedName + "."));
    }

    /** Todos los vuelos ordenados por fecha y hora de salida. */
    public List<Flight> listFlights() {
        return sorted(flightRepository.findAll());
    }


    public Flight updateFlight(int id, String flightName, Airport departure, Airport arrival,
                               LocalDate departureDate, LocalTime departureTime,
                               LocalDate arrivalDate, LocalTime arrivalTime, Airline airline) {
        getFlight(id);
        String normalizedName = Flight.normalizeFlightName(flightName);
        flightRepository.findByFlightName(normalizedName).ifPresent(other -> {
            if (other.getId() != id) {
                throw new DuplicateException("Ya existe otro vuelo con el código " + normalizedName + ".");
            }
        });
        Flight updated = new Flight(id, normalizedName,
                resolveAirport(departure, "aeropuerto de salida"),
                resolveAirport(arrival, "aeropuerto de llegada"),
                departureDate, departureTime, arrivalDate, arrivalTime,
                resolveAirline(airline));
        flightRepository.update(updated);
        return updated;
    }


    public void deleteFlight(int id) {
        getFlight(id);
        flightRepository.deleteById(id);
    }


    /** Vuelos de una aerolínea existente, ordenados por salida. */
    public List<Flight> listFlightsByAirline(int airlineId) {
        airlineService.getAirline(airlineId);
        return sorted(flightRepository.findByAirline(airlineId));
    }

    /** Vuelos cuya salida cae en el rango (inclusive). El inicio no puede ser posterior al fin. */
    public List<Flight> listFlightsBetween(LocalDate start, LocalDate end) {
        Validator.requireNotNull(start, "fecha de inicio");
        Validator.requireNotNull(end, "fecha de fin");
        if (start.isAfter(end)) {
            throw new ValidationException("La fecha de inicio no puede ser posterior a la fecha de fin.");
        }
        return sorted(flightRepository.findByDepartureDateBetween(start, end));
    }

    /** Vuelos de una ruta; ambos aeropuertos deben existir. */
    public List<Flight> listFlightsByRoute(UUID departureId, UUID arrivalId) {
        locationService.getAirport(departureId);
        locationService.getAirport(arrivalId);
        return sorted(flightRepository.findByRoute(departureId, arrivalId));
    }

    /** Total de vuelos de una aerolínea. */
    public int countFlightsByAirline(int airlineId) {
        return flightRepository.countByAirline(airlineId);
    }

    /** ¿La aerolínea tiene vuelos? La usará el módulo 3 en la fase de integración. */
    public boolean airlineHasFlights(int airlineId) {
        return flightRepository.existsByAirline(airlineId);
    }

    /** ¿El aeropuerto tiene vuelos (de salida o llegada)? La usará el módulo 2 en la integración. */
    public boolean airportHasFlights(UUID airportId) {
        return flightRepository.existsByAirport(airportId);
    }

    /** ¿El vuelo ya salió? Útil para el módulo 5 (no reservar en vuelos que ya salieron). */
    public boolean hasFlightDeparted(int flightId) {
        return getFlight(flightId).hasDeparted(LocalDateTime.now());
    }


    public Map<Airline, List<FlightReportRow>> listFlightsByAirlineInMonth(int year, int month) {
        if (month < 1 || month > 12) {
            throw new ValidationException("El mes debe estar entre 1 y 12.");
        }
        if (year < MIN_REPORT_YEAR || year > MAX_REPORT_YEAR) {
            throw new ValidationException("El año debe estar entre " + MIN_REPORT_YEAR + " y " + MAX_REPORT_YEAR + ".");
        }
        YearMonth period = YearMonth.of(year, month);
        // TreeMap conserva el orden alfabético de las aerolíneas (sus nombres son únicos).
        Map<Airline, List<FlightReportRow>> report =
                new TreeMap<>(Comparator.comparing(Airline::getName, String.CASE_INSENSITIVE_ORDER));
        for (Flight flight : listFlights()) {
            if (YearMonth.from(flight.getDepartureDate()).equals(period)) {
                report.computeIfAbsent(flight.getAirline(), key -> new ArrayList<>())
                        .add(FlightReportRow.from(flight));
            }
        }
        return report;
    }

    // --------------------------------------------------------------- HELPERS

    private Airport resolveAirport(Airport airport, String field) {
        Validator.requireNotNull(airport, field);
        return locationService.getAirport(airport.getId());
    }

    private Airline resolveAirline(Airline airline) {
        Validator.requireNotNull(airline, "aerolínea");
        return airlineService.getAirline(airline.getId());
    }

    private List<Flight> sorted(List<Flight> flights) {
        List<Flight> copy = new ArrayList<>(flights);
        Collections.sort(copy);
        return copy;
    }
}
