package com.airlinespoo.flights.model;

import com.airlinespoo.airlines.model.Airline;
import com.airlinespoo.core.model.Identifiable;
import com.airlinespoo.core.util.Validator;
import com.airlinespoo.core.exception.ValidationException;
import com.airlinespoo.locations.model.Airport;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;


public class Flight implements Identifiable<Integer>, Comparable<Flight> {

    /** Longitud máxima del código de vuelo (VARCHAR(8) en el modelo). */
    public static final int MAX_FLIGHT_NAME_LENGTH = 8;
    private static final String FLIGHT_NAME_FIELD = "código del vuelo";
    private static final String FLIGHT_NAME_PATTERN = "[A-Z0-9]+";

    private final int id;
    private String flightName;
    private Airport departure;
    private Airport arrival;
    private LocalDate departureDate;
    private LocalTime departureTime;
    private LocalDate arrivalDate;
    private LocalTime arrivalTime;
    private Airline airline;

    public Flight(int id, String flightName, Airport departure, Airport arrival,
                  LocalDate departureDate, LocalTime departureTime,
                  LocalDate arrivalDate, LocalTime arrivalTime, Airline airline) {
        if (id <= 0) {
            throw new ValidationException("El id del vuelo debe ser positivo.");
        }
        this.id = id;
        setFlightName(flightName);
        setRoute(departure, arrival);
        setSchedule(departureDate, departureTime, arrivalDate, arrivalTime);
        setAirline(airline);
    }


    public static String normalizeFlightName(String rawFlightName) {
        Validator.requireText(rawFlightName, FLIGHT_NAME_FIELD);
        String normalized = Validator.normalize(rawFlightName).toUpperCase();
        Validator.requireText(normalized, FLIGHT_NAME_FIELD, MAX_FLIGHT_NAME_LENGTH);
        if (!normalized.matches(FLIGHT_NAME_PATTERN)) {
            throw new ValidationException("El código del vuelo solo puede tener letras y dígitos, sin espacios.");
        }
        return normalized;
    }

    @Override
    public Integer getId() {
        return id;
    }

    public String getFlightName() {
        return flightName;
    }

    public Airport getDeparture() {
        return departure;
    }

    public Airport getArrival() {
        return arrival;
    }

    public LocalDate getDepartureDate() {
        return departureDate;
    }

    public LocalTime getDepartureTime() {
        return departureTime;
    }

    public LocalDate getArrivalDate() {
        return arrivalDate;
    }

    public LocalTime getArrivalTime() {
        return arrivalTime;
    }

    public Airline getAirline() {
        return airline;
    }

    /** Cambia el código del vuelo con la misma validación del constructor. */
    public void setFlightName(String flightName) {
        this.flightName = normalizeFlightName(flightName);
    }

    /** Cambia la aerolínea (obligatoria). */
    public void setAirline(Airline airline) {
        Validator.requireNotNull(airline, "aerolínea");
        this.airline = airline;
    }

    /** Cambia la ruta; salida y llegada son obligatorias y deben ser distintas. */
    public void setRoute(Airport departure, Airport arrival) {
        Validator.requireNotNull(departure, "aeropuerto de salida");
        Validator.requireNotNull(arrival, "aeropuerto de llegada");
        if (departure.equals(arrival)) {
            throw new ValidationException("El aeropuerto de salida y el de llegada deben ser distintos.");
        }
        this.departure = departure;
        this.arrival = arrival;
    }

    /**
     * Cambia el horario completo. Se valida fecha Y hora juntas: un vuelo nocturno
     * puede llegar al día siguiente.
     */
    public void setSchedule(LocalDate departureDate, LocalTime departureTime,
                            LocalDate arrivalDate, LocalTime arrivalTime) {
        Validator.requireNotNull(departureDate, "fecha de salida");
        Validator.requireNotNull(departureTime, "hora de salida");
        Validator.requireNotNull(arrivalDate, "fecha de llegada");
        Validator.requireNotNull(arrivalTime, "hora de llegada");
        LocalDateTime departureAt = LocalDateTime.of(departureDate, departureTime);
        LocalDateTime arrivalAt = LocalDateTime.of(arrivalDate, arrivalTime);
        if (!arrivalAt.isAfter(departureAt)) {
            throw new ValidationException("La llegada debe ser posterior a la salida.");
        }
        this.departureDate = departureDate;
        this.departureTime = departureTime;
        this.arrivalDate = arrivalDate;
        this.arrivalTime = arrivalTime;
    }

    /** Salida completa (fecha + hora). */
    public LocalDateTime getDepartureDateTime() {
        return LocalDateTime.of(departureDate, departureTime);
    }

    /** Llegada completa (fecha + hora). */
    public LocalDateTime getArrivalDateTime() {
        return LocalDateTime.of(arrivalDate, arrivalTime);
    }

    /** Duración del vuelo. */
    public Duration getDuration() {
        return Duration.between(getDepartureDateTime(), getArrivalDateTime());
    }

    /** Indica si el vuelo ya salió respecto del momento dado. */
    public boolean hasDeparted(LocalDateTime now) {
        Validator.requireNotNull(now, "momento de referencia");
        return !getDepartureDateTime().isAfter(now);
    }

    /** Ordena por fecha y hora de salida; el id desempata. */
    @Override
    public int compareTo(Flight other) {
        int byDeparture = getDepartureDateTime().compareTo(other.getDepartureDateTime());
        return byDeparture != 0 ? byDeparture : Integer.compare(id, other.id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Flight)) {
            return false;
        }
        return id == ((Flight) o).id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public String toString() {
        return "Flight{id=" + id + ", código=" + flightName
                + ", aerolínea=" + airline.getName()
                + ", " + departure.getName() + " -> " + arrival.getName()
                + ", salida=" + getDepartureDateTime()
                + ", llegada=" + getArrivalDateTime() + "}";
    }
}
