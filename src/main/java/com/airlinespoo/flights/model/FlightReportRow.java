package com.airlinespoo.flights.model;

import java.time.LocalDateTime;


public record FlightReportRow(
        String airlineName,
        String flightName,
        String departureCity,
        String departureAirport,
        LocalDateTime departure,
        String arrivalCity,
        String arrivalAirport,
        LocalDateTime arrival) {

    /** Construye la fila a partir de un vuelo. */
    public static FlightReportRow from(Flight flight) {
        return new FlightReportRow(
                flight.getAirline().getName(),
                flight.getFlightName(),
                flight.getDeparture().getCity().getName(),
                flight.getDeparture().getName(),
                flight.getDepartureDateTime(),
                flight.getArrival().getCity().getName(),
                flight.getArrival().getName(),
                flight.getArrivalDateTime());
    }
}
