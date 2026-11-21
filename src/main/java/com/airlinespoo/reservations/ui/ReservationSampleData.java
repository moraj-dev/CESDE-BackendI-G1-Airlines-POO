package com.airlinespoo.reservations.ui;

import com.airlinespoo.customers.model.Customer;
import com.airlinespoo.customers.service.CustomerService;
import com.airlinespoo.flights.model.Flight;
import com.airlinespoo.flights.service.FlightService;
import com.airlinespoo.reservations.model.Seat;
import com.airlinespoo.reservations.service.PassengerData;
import com.airlinespoo.reservations.service.ReservationService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Carga reservas de muestra usando los clientes del Módulo 1 y los vuelos del Módulo 4.
 *
 * <p>Crea hasta {@value #SAMPLE_RESERVATIONS} reservas con 1 a {@value #MAX_SAMPLE_PASSENGERS}
 * pasajeros cada una. Cada reserva usa una fila distinta, así que nunca se repite una silla
 * en un mismo vuelo. Solo usa vuelos que aún no han salido, porque los demás no admiten
 * reservas. Todo pasa por el servicio, de modo que los generadores de ids quedan al día.</p>
 */
public final class ReservationSampleData {

    private static final int SAMPLE_RESERVATIONS = 4;
    private static final int MAX_SAMPLE_PASSENGERS = 3;
    private static final String[] ROWS = {"A", "B", "C", "D"};
    private static final String[][] PASSENGERS = {
            {"Laura Gómez", "1001001"},
            {"Andrés Ruiz", "1001002"},
            {"Sofía Herrera", "1001003"},
            {"Mateo Castro", "1001004"},
            {"Valentina Ríos", "1001005"},
            {"Samuel Ortiz", "1001006"},
    };

    private ReservationSampleData() {
        // Clase de datos de ejemplo: no se instancia.
    }

    /**
     * Carga las reservas de muestra. Debe llamarse después de cargar los datos de clientes
     * y de vuelos.
     *
     * @param reservationService servicio de reservas
     * @param customerService    servicio de clientes
     * @param flightService      servicio de vuelos
     * @return cuántas reservas se cargaron (0 si no hay clientes o vuelos futuros)
     */
    public static int load(ReservationService reservationService, CustomerService customerService,
                           FlightService flightService) {
        List<Customer> customers = customerService.listCustomers();
        List<Flight> flights = futureFlights(flightService.listFlights());
        if (customers.isEmpty() || flights.isEmpty()) {
            return 0;
        }
        for (int i = 0; i < SAMPLE_RESERVATIONS; i++) {
            Customer customer = customers.get(i % customers.size());
            Flight flight = flights.get(i % flights.size());
            reservationService.createReservation(customer.getId(), flight.getId(), passengersFor(i));
        }
        return SAMPLE_RESERVATIONS;
    }

    private static List<PassengerData> passengersFor(int reservationIndex) {
        int count = reservationIndex % MAX_SAMPLE_PASSENGERS + 1;
        List<PassengerData> passengers = new ArrayList<>();
        for (int seat = 1; seat <= count; seat++) {
            String[] person = PASSENGERS[(reservationIndex + seat) % PASSENGERS.length];
            passengers.add(new PassengerData(person[0], person[1], new Seat(ROWS[reservationIndex], seat)));
        }
        return passengers;
    }

    private static List<Flight> futureFlights(List<Flight> flights) {
        LocalDateTime now = LocalDateTime.now();
        List<Flight> result = new ArrayList<>();
        for (Flight flight : flights) {
            if (LocalDateTime.of(flight.getDepartureDate(), flight.getDepartureTime()).isAfter(now)) {
                result.add(flight);
            }
        }
        return result;
    }
}
