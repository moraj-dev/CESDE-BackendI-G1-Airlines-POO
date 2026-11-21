package com.airlinespoo.reservations.ui;

import com.airlinespoo.core.exception.AirlinesException;
import com.airlinespoo.core.ui.ConsoleInput;
import com.airlinespoo.core.ui.Menu;
import com.airlinespoo.core.util.Validator;
import com.airlinespoo.customers.model.Customer;
import com.airlinespoo.customers.service.CustomerService;
import com.airlinespoo.flights.model.Flight;
import com.airlinespoo.flights.service.FlightService;
import com.airlinespoo.reservations.model.Reservation;
import com.airlinespoo.reservations.model.ReservationDetail;
import com.airlinespoo.reservations.model.Seat;
import com.airlinespoo.reservations.service.ManifestEntry;
import com.airlinespoo.reservations.service.PassengerData;
import com.airlinespoo.reservations.service.ReservationService;

import java.util.ArrayList;
import java.util.List;

/**
 * Menú de consola del módulo de reservas.
 *
 * <p>Separa "pedir datos" de "llamar al servicio": primero lee y confirma lo que escribe
 * el usuario y solo al final llama a {@link ReservationService}. Captura las excepciones
 * del sistema y muestra su mensaje; un dato mal digitado nunca termina el programa.</p>
 *
 * <p>Usa los servicios de clientes y vuelos solo para buscar y mostrar el cliente y el
 * vuelo antes de confirmar; nunca habla con un repositorio.</p>
 */
public class ReservationMenu implements Menu {

    private static final String TITLE = "Reservas y pasajeros";

    private static final int OPTION_EXIT = 0;
    private static final int OPTION_CREATE = 1;
    private static final int OPTION_ADD_PASSENGER = 2;
    private static final int OPTION_REMOVE_PASSENGER = 3;
    private static final int OPTION_CHANGE_SEAT = 4;
    private static final int OPTION_UPDATE_PASSENGER = 5;
    private static final int OPTION_CHANGE_FLIGHT = 6;
    private static final int OPTION_CANCEL = 7;
    private static final int OPTION_BY_CUSTOMER = 8;
    private static final int OPTION_LIST_ALL = 9;
    private static final int OPTION_OCCUPIED_SEATS = 10;
    private static final int OPTION_MANIFEST = 11;
    private static final int LAST_OPTION = OPTION_MANIFEST;

    private static final int MAX_ID = Integer.MAX_VALUE;
    private static final String MANIFEST_FORMAT = "%-6s %-50s %-20s%n";

    private final ConsoleInput input;
    private final ReservationService reservationService;
    private final CustomerService customerService;
    private final FlightService flightService;

    /**
     * @param input              lector de consola compartido
     * @param reservationService servicio de reservas
     * @param customerService    servicio de clientes, para buscar y mostrar al cliente
     * @param flightService      servicio de vuelos, para buscar y mostrar el vuelo
     */
    public ReservationMenu(ConsoleInput input, ReservationService reservationService,
                           CustomerService customerService, FlightService flightService) {
        this.input = Validator.requireNotNull(input, "lector de consola");
        this.reservationService = Validator.requireNotNull(reservationService, "servicio de reservas");
        this.customerService = Validator.requireNotNull(customerService, "servicio de clientes");
        this.flightService = Validator.requireNotNull(flightService, "servicio de vuelos");
    }

    /**
     * @return el título que muestra el menú principal
     */
    @Override
    public String title() {
        return TITLE;
    }

    /**
     * Muestra las opciones y ejecuta la elegida hasta que el usuario elige volver.
     */
    @Override
    public void run() {
        int option;
        do {
            printOptions();
            option = input.readInt("Elige una opción", OPTION_EXIT, LAST_OPTION);
            if (option != OPTION_EXIT) {
                runSafely(option);
            }
        } while (option != OPTION_EXIT);
    }

    private void printOptions() {
        System.out.println();
        System.out.println("=== " + TITLE + " ===");
        System.out.println(OPTION_CREATE + ". Crear reserva");
        System.out.println(OPTION_ADD_PASSENGER + ". Agregar pasajero");
        System.out.println(OPTION_REMOVE_PASSENGER + ". Quitar pasajero");
        System.out.println(OPTION_CHANGE_SEAT + ". Cambiar silla");
        System.out.println(OPTION_UPDATE_PASSENGER + ". Actualizar datos de un pasajero");
        System.out.println(OPTION_CHANGE_FLIGHT + ". Cambiar el vuelo de una reserva");
        System.out.println(OPTION_CANCEL + ". Cancelar reserva");
        System.out.println(OPTION_BY_CUSTOMER + ". Consultar reservas de un cliente");
        System.out.println(OPTION_LIST_ALL + ". Listar todas las reservas");
        System.out.println(OPTION_OCCUPIED_SEATS + ". Ver sillas ocupadas de un vuelo");
        System.out.println(OPTION_MANIFEST + ". Ver manifiesto del vuelo");
        System.out.println(OPTION_EXIT + ". Volver");
    }

    private void runSafely(int option) {
        try {
            execute(option);
        } catch (AirlinesException e) {
            System.out.println("No se pudo completar la operación: " + e.getMessage());
        }
    }

    private void execute(int option) {
        switch (option) {
            case OPTION_CREATE -> createReservation();
            case OPTION_ADD_PASSENGER -> addPassenger();
            case OPTION_REMOVE_PASSENGER -> removePassenger();
            case OPTION_CHANGE_SEAT -> changeSeat();
            case OPTION_UPDATE_PASSENGER -> updatePassenger();
            case OPTION_CHANGE_FLIGHT -> changeFlight();
            case OPTION_CANCEL -> cancelReservation();
            case OPTION_BY_CUSTOMER -> listByCustomer();
            case OPTION_LIST_ALL -> listAll();
            case OPTION_OCCUPIED_SEATS -> showOccupiedSeats();
            case OPTION_MANIFEST -> showManifest();
            default -> System.out.println("Opción no válida.");
        }
    }

    // ---------------------------------------------------------------- Opciones

    /**
     * Flujo en pasos: cliente → vuelo → pasajeros → confirmación. Nada se guarda hasta el
     * final; si algo falla o el usuario no confirma, se descarta todo lo capturado.
     */
    private void createReservation() {
        Customer customer = askCustomer();
        if (customer == null) {
            return;
        }
        Flight flight = askFlight();
        if (flight == null) {
            return;
        }
        int count = input.readInt("¿Cuántos pasajeros?", Reservation.MIN_PASSENGERS, Reservation.MAX_PASSENGERS);
        List<PassengerData> passengers = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            System.out.println("-- Pasajero " + i + " de " + count);
            passengers.add(askPassenger());
        }
        if (!input.confirm("¿Guardar la reserva?")) {
            System.out.println("Reserva descartada.");
            return;
        }
        Reservation reservation = reservationService.createReservation(customer.getId(), flight.getId(), passengers);
        System.out.println("Reserva creada: " + reservation);
    }

    private void addPassenger() {
        Reservation reservation = askReservation();
        ReservationDetail detail = reservationService.addPassenger(reservation.getId(), askPassenger());
        System.out.println("Pasajero agregado: " + detail);
    }

    private void removePassenger() {
        Reservation reservation = askReservation();
        ReservationDetail detail = askDetail(reservation);
        if (input.confirm("¿Quitar a " + detail.getPassengerFullName() + "?")) {
            reservationService.removePassenger(reservation.getId(), detail.getId());
            System.out.println("Pasajero quitado.");
        }
    }

    private void changeSeat() {
        Reservation reservation = askReservation();
        ReservationDetail detail = askDetail(reservation);
        Seat newSeat = askSeat();
        reservationService.changeSeat(reservation.getId(), detail.getId(), newSeat);
        System.out.println("Silla actualizada a " + newSeat + ".");
    }

    private void updatePassenger() {
        Reservation reservation = askReservation();
        ReservationDetail detail = askDetail(reservation);
        String fullName = input.readRequiredText("Nuevo nombre completo");
        String document = input.readRequiredText("Nuevo documento");
        reservationService.updatePassenger(reservation.getId(), detail.getId(), fullName, document);
        System.out.println("Pasajero actualizado.");
    }

    private void changeFlight() {
        Reservation reservation = askReservation();
        Flight flight = askFlight();
        if (flight != null) {
            reservationService.changeReservationFlight(reservation.getId(), flight.getId());
            System.out.println("Vuelo de la reserva actualizado a " + flight.getFlightName() + ".");
        }
    }

    private void cancelReservation() {
        Reservation reservation = askReservation();
        if (input.confirm("¿Cancelar la reserva #" + reservation.getId() + " con todos sus pasajeros?")) {
            reservationService.cancelReservation(reservation.getId());
            System.out.println("Reserva cancelada. Sus sillas quedaron libres.");
        }
    }

    private void listByCustomer() {
        String document = input.readRequiredText("Documento del cliente");
        printReservations(reservationService.listReservationsByCustomer(document));
    }

    private void listAll() {
        printReservations(reservationService.listReservations());
    }

    private void showOccupiedSeats() {
        Flight flight = findFlightByCode();
        List<Seat> seats = reservationService.listOccupiedSeats(flight.getId());
        if (seats.isEmpty()) {
            System.out.println("El vuelo " + flight.getFlightName() + " no tiene sillas ocupadas.");
            return;
        }
        System.out.println("Sillas ocupadas del vuelo " + flight.getFlightName() + ": " + seats);
    }

    private void showManifest() {
        Flight flight = findFlightByCode();
        List<ManifestEntry> manifest = reservationService.getFlightManifest(flight.getId());
        System.out.println("Manifiesto del vuelo " + describeFlight(flight));
        if (manifest.isEmpty()) {
            System.out.println("El vuelo no tiene pasajeros.");
            return;
        }
        System.out.printf(MANIFEST_FORMAT, "Silla", "Pasajero", "Documento");
        for (ManifestEntry entry : manifest) {
            System.out.printf(MANIFEST_FORMAT, entry.seat(), entry.passengerFullName(), entry.passengerDocument());
        }
        System.out.println("Total de pasajeros: " + manifest.size());
    }

    // ---------------------------------------------------------------- Lectura de datos

    private Customer askCustomer() {
        String document = input.readRequiredText("Documento del cliente");
        Customer customer = customerService.getCustomer(document);
        boolean confirmed = input.confirm("Cliente: " + describeCustomer(customer) + ". ¿Es correcto?");
        return confirmed ? customer : null;
    }

    private Flight askFlight() {
        Flight flight = findFlightByCode();
        boolean confirmed = input.confirm("Vuelo " + describeFlight(flight) + ". ¿Es correcto?");
        return confirmed ? flight : null;
    }

    private Flight findFlightByCode() {
        String code = input.readRequiredText("Código del vuelo");
        return flightService.getFlightByName(code);
    }

    private Reservation askReservation() {
        int id = input.readInt("Número de la reserva", 1, MAX_ID);
        Reservation reservation = reservationService.getReservation(id);
        System.out.println(reservation);
        return reservation;
    }

    private ReservationDetail askDetail(Reservation reservation) {
        List<ReservationDetail> details = reservation.getDetails();
        for (int i = 0; i < details.size(); i++) {
            System.out.println((i + 1) + ". " + details.get(i));
        }
        int position = input.readInt("Elige el pasajero", 1, details.size());
        return details.get(position - 1);
    }

    private PassengerData askPassenger() {
        String fullName = input.readRequiredText("Nombre completo del pasajero");
        String document = input.readRequiredText("Documento del pasajero");
        return new PassengerData(fullName, document, askSeat());
    }

    private Seat askSeat() {
        String row = input.readRequiredText("Fila de la silla (1 o 2 letras, por ejemplo B)");
        int number = input.readInt("Número de la silla", 1, Seat.MAX_SEAT_NUMBER);
        return new Seat(row, number);
    }

    // ---------------------------------------------------------------- Presentación

    private void printReservations(List<Reservation> reservations) {
        if (reservations.isEmpty()) {
            System.out.println("No hay reservas para mostrar.");
            return;
        }
        for (Reservation reservation : reservations) {
            System.out.println(reservation + " | " + describeFlight(reservation.getFlight()));
            for (ReservationDetail detail : reservation.getDetails()) {
                System.out.println("    " + detail);
            }
        }
    }

    private static String describeCustomer(Customer customer) {
        return customer.getFirstName() + " " + customer.getLastName();
    }

    private static String describeFlight(Flight flight) {
        return "%s: %s -> %s, sale %s %s".formatted(
                flight.getFlightName(),
                flight.getDeparture().getName(),
                flight.getArrival().getName(),
                flight.getDepartureDate(),
                flight.getDepartureTime());
    }
}
