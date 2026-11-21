package com.airlinespoo.reservations.service;

import com.airlinespoo.core.exception.BusinessRuleException;
import com.airlinespoo.core.exception.DuplicateException;
import com.airlinespoo.core.exception.NotFoundException;
import com.airlinespoo.core.exception.ValidationException;
import com.airlinespoo.core.util.IdGenerator;
import com.airlinespoo.core.util.Validator;
import com.airlinespoo.customers.model.Customer;
import com.airlinespoo.customers.service.CustomerService;
import com.airlinespoo.flights.model.Flight;
import com.airlinespoo.flights.service.FlightService;
import com.airlinespoo.reservations.model.Reservation;
import com.airlinespoo.reservations.model.ReservationDetail;
import com.airlinespoo.reservations.model.Seat;
import com.airlinespoo.reservations.repository.ReservationRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Reglas de negocio de reservas, pasajeros, sillas y manifiesto del vuelo.
 *
 * <p>Su regla central es la de la silla: una silla no puede asignarse a dos pasajeros del
 * mismo vuelo. Ningún objeto solo puede validarla, porque depende de <b>todas</b> las
 * reservas del vuelo; por eso vive aquí.</p>
 *
 * <p>Recibe por el constructor el repositorio, los servicios de clientes y vuelos y los
 * generadores de ids (inyección de dependencias manual). Consulta clientes y vuelos a
 * través de sus servicios, nunca de sus repositorios. No imprime ni lee de la consola.</p>
 */
public class ReservationService {

    private static final String RESERVATION_ENTITY = "reserva";
    private static final String PASSENGER_ENTITY = "pasajero de la reserva";

    private static final Comparator<Reservation> BY_FLIGHT_DEPARTURE =
            Comparator.comparing((Reservation reservation) -> departureOf(reservation.getFlight()))
                    .thenComparing(Reservation::getId);

    private final ReservationRepository repository;
    private final CustomerService customerService;
    private final FlightService flightService;
    private final IdGenerator reservationIds;
    private final IdGenerator detailIds;

    /**
     * @param repository      repositorio de reservas
     * @param customerService servicio del Módulo 1, para consultar clientes
     * @param flightService   servicio del Módulo 4, para consultar vuelos
     * @param reservationIds  generador de ids de reservas (arranca en 1, paso 1)
     * @param detailIds       generador de ids de detalles (arranca en 1, paso 1)
     * @throws ValidationException si alguna dependencia es nula
     */
    public ReservationService(ReservationRepository repository, CustomerService customerService,
                              FlightService flightService, IdGenerator reservationIds,
                              IdGenerator detailIds) {
        this.repository = Validator.requireNotNull(repository, "repositorio de reservas");
        this.customerService = Validator.requireNotNull(customerService, "servicio de clientes");
        this.flightService = Validator.requireNotNull(flightService, "servicio de vuelos");
        this.reservationIds = Validator.requireNotNull(reservationIds, "generador de ids de reservas");
        this.detailIds = Validator.requireNotNull(detailIds, "generador de ids de detalles");
    }

    // ---------------------------------------------------------------- Crear

    /**
     * Crea una reserva con sus pasajeros (INSERT en {@code tblReservations} y uno en
     * {@code tblDetailsReservation} por pasajero).
     *
     * @param customerId documento del cliente
     * @param flightId   id del vuelo
     * @param passengers pasajeros con su silla: al menos uno
     * @return la reserva creada
     * @throws NotFoundException     si el cliente o el vuelo no existen
     * @throws BusinessRuleException si el vuelo ya salió, una silla está ocupada en el vuelo
     *                               o se supera el máximo de pasajeros
     * @throws ValidationException   si no hay pasajeros o algún dato no es válido
     * @throws DuplicateException    si un documento se repite en la reserva
     */
    public Reservation createReservation(String customerId, int flightId, List<PassengerData> passengers) {
        Customer customer = customerService.getCustomer(customerId);
        Flight flight = requireNotDeparted(flightService.getFlight(flightId));
        requirePassengers(passengers);
        List<ReservationDetail> details = new ArrayList<>();
        for (PassengerData passenger : passengers) {
            ReservationDetail detail = toDetail(passenger);
            requireSeatFree(flightId, detail.getSeat(), 0);
            details.add(detail);
        }
        // Se consulta el id sin avanzar el generador: si la reserva no es válida, el id no se pierde.
        Reservation reservation = new Reservation(reservationIds.peekNext(), customer, flight, details);
        repository.create(reservation);
        reservationIds.next();
        return reservation;
    }

    /**
     * Agrega un pasajero a una reserva existente.
     *
     * @param reservationId id de la reserva
     * @param passenger     datos del pasajero y su silla
     * @return el detalle creado
     * @throws NotFoundException     si la reserva no existe
     * @throws BusinessRuleException si la silla está ocupada en el vuelo o se supera el máximo
     * @throws DuplicateException    si el documento ya está en la reserva
     */
    public ReservationDetail addPassenger(int reservationId, PassengerData passenger) {
        Reservation reservation = getReservation(reservationId);
        ReservationDetail detail = toDetail(passenger);
        requireSeatFree(flightIdOf(reservation), detail.getSeat(), 0);
        reservation.addPassenger(detail);
        repository.update(reservation);
        return detail;
    }

    // ---------------------------------------------------------------- Leer

    /**
     * @param reservationId id de la reserva
     * @return la reserva
     * @throws NotFoundException si no existe
     */
    public Reservation getReservation(int reservationId) {
        return repository.findById(reservationId)
                .orElseThrow(() -> new NotFoundException(RESERVATION_ENTITY, reservationId));
    }

    /**
     * @return todas las reservas, ordenadas por id
     */
    public List<Reservation> listReservations() {
        List<Reservation> reservations = repository.findAll();
        reservations.sort(Comparator.comparing(Reservation::getId));
        return reservations;
    }

    /**
     * @param reservationId id de la reserva
     * @return los pasajeros de la reserva, en una lista de solo lectura
     * @throws NotFoundException si la reserva no existe
     */
    public List<ReservationDetail> listPassengers(int reservationId) {
        return getReservation(reservationId).getDetails();
    }

    /**
     * @param customerId documento del cliente
     * @return las reservas del cliente, ordenadas por fecha y hora de salida del vuelo
     * @throws NotFoundException si el cliente no existe
     */
    public List<Reservation> listReservationsByCustomer(String customerId) {
        Customer customer = customerService.getCustomer(customerId);
        List<Reservation> reservations = repository.findByCustomer(customer.getId());
        reservations.sort(BY_FLIGHT_DEPARTURE);
        return reservations;
    }

    /**
     * @param flightId id del vuelo
     * @return las reservas del vuelo, ordenadas por id
     * @throws NotFoundException si el vuelo no existe
     */
    public List<Reservation> listReservationsByFlight(int flightId) {
        flightService.getFlight(flightId);
        List<Reservation> reservations = repository.findByFlight(flightId);
        reservations.sort(Comparator.comparing(Reservation::getId));
        return reservations;
    }

    // ---------------------------------------------------------------- Actualizar

    /**
     * Cambia el vuelo de una reserva. Todas sus sillas deben estar libres en el nuevo vuelo.
     *
     * @param reservationId id de la reserva
     * @param newFlightId   id del nuevo vuelo
     * @return la reserva actualizada
     * @throws NotFoundException     si la reserva o el vuelo no existen
     * @throws BusinessRuleException si el vuelo ya salió o alguna silla está ocupada en él
     */
    public Reservation changeReservationFlight(int reservationId, int newFlightId) {
        Reservation reservation = getReservation(reservationId);
        Flight newFlight = requireNotDeparted(flightService.getFlight(newFlightId));
        for (Seat seat : reservation.getSeats()) {
            requireSeatFree(newFlightId, seat, reservationId);
        }
        reservation.changeFlight(newFlight);
        return repository.update(reservation);
    }

    /**
     * Cambia la silla de un pasajero. La nueva silla debe estar libre en el vuelo; elegir
     * la misma silla que ya tiene no cambia nada.
     *
     * @param reservationId id de la reserva
     * @param detailId      id del pasajero dentro de la reserva
     * @param newSeat       nueva silla
     * @throws NotFoundException     si la reserva o el pasajero no existen
     * @throws BusinessRuleException si la silla está ocupada en el vuelo
     */
    public void changeSeat(int reservationId, int detailId, Seat newSeat) {
        Reservation reservation = getReservation(reservationId);
        ReservationDetail detail = requireDetail(reservation, detailId);
        Validator.requireNotNull(newSeat, "silla");
        if (detail.getSeat().equals(newSeat)) {
            return;
        }
        requireSeatFree(flightIdOf(reservation), newSeat, 0);
        reservation.changeSeat(detailId, newSeat);
        repository.update(reservation);
    }

    /**
     * Actualiza el nombre y el documento de un pasajero.
     *
     * @param reservationId id de la reserva
     * @param detailId      id del pasajero dentro de la reserva
     * @param fullName      nuevo nombre completo
     * @param document      nuevo documento
     * @throws NotFoundException   si la reserva o el pasajero no existen
     * @throws DuplicateException  si el documento es de otro pasajero de la reserva
     * @throws ValidationException si algún dato no es válido
     */
    public void updatePassenger(int reservationId, int detailId, String fullName, String document) {
        Reservation reservation = getReservation(reservationId);
        reservation.updatePassenger(detailId, fullName, document);
        repository.update(reservation);
    }

    // ---------------------------------------------------------------- Eliminar

    /**
     * Quita un pasajero de la reserva.
     *
     * @param reservationId id de la reserva
     * @param detailId      id del pasajero dentro de la reserva
     * @return el pasajero quitado
     * @throws NotFoundException     si la reserva o el pasajero no existen
     * @throws BusinessRuleException si es el único pasajero de la reserva
     */
    public ReservationDetail removePassenger(int reservationId, int detailId) {
        Reservation reservation = getReservation(reservationId);
        ReservationDetail removed = reservation.removePassenger(detailId);
        repository.update(reservation);
        return removed;
    }

    /**
     * Cancela la reserva: la elimina con todos sus pasajeros y libera sus sillas.
     *
     * @param reservationId id de la reserva
     * @throws NotFoundException si la reserva no existe
     */
    public void cancelReservation(int reservationId) {
        if (!repository.deleteById(reservationId)) {
            throw new NotFoundException(RESERVATION_ENTITY, reservationId);
        }
    }

    // ---------------------------------------------------------------- Complementarios

    /**
     * Lo usará Clientes en la fase de integración.
     *
     * @param customerId documento del cliente
     * @return {@code true} si el cliente tiene reservas
     */
    public boolean customerHasReservations(String customerId) {
        return repository.existsByCustomer(customerId);
    }

    /**
     * Lo usará Vuelos en la fase de integración.
     *
     * @param flightId id del vuelo
     * @return {@code true} si el vuelo tiene reservas
     */
    public boolean flightHasReservations(int flightId) {
        return repository.existsByFlight(flightId);
    }

    /**
     * Regla central de las sillas: revisa todas las reservas del vuelo.
     *
     * @param flightId id del vuelo
     * @param seat     silla a consultar
     * @return {@code true} si algún pasajero del vuelo ya tiene esa silla
     * @throws NotFoundException si el vuelo no existe
     */
    public boolean isSeatOccupied(int flightId, Seat seat) {
        flightService.getFlight(flightId);
        Validator.requireNotNull(seat, "silla");
        return occupiedSeats(flightId, 0).contains(seat);
    }

    /**
     * @param flightId id del vuelo
     * @return las sillas ocupadas del vuelo, ordenadas por fila y número
     * @throws NotFoundException si el vuelo no existe
     */
    public List<Seat> listOccupiedSeats(int flightId) {
        flightService.getFlight(flightId);
        List<Seat> seats = new ArrayList<>(occupiedSeats(flightId, 0));
        seats.sort(Comparator.naturalOrder());
        return seats;
    }

    /**
     * @param flightId id del vuelo
     * @return el total de pasajeros entre todas las reservas del vuelo
     * @throws NotFoundException si el vuelo no existe
     */
    public int countPassengersByFlight(int flightId) {
        flightService.getFlight(flightId);
        int total = 0;
        for (Reservation reservation : repository.findByFlight(flightId)) {
            total += reservation.getPassengerCount();
        }
        return total;
    }

    /**
     * Lista de pasajeros del vuelo (manifiesto).
     *
     * @param flightId id del vuelo
     * @return nombre, documento y silla de cada pasajero, ordenado por silla
     * @throws NotFoundException si el vuelo no existe
     */
    public List<ManifestEntry> getFlightManifest(int flightId) {
        flightService.getFlight(flightId);
        List<ManifestEntry> manifest = new ArrayList<>();
        for (Reservation reservation : repository.findByFlight(flightId)) {
            for (ReservationDetail detail : reservation.getDetails()) {
                manifest.add(new ManifestEntry(
                        detail.getPassengerFullName(), detail.getPassengerDocument(), detail.getSeat()));
            }
        }
        manifest.sort(Comparator.comparing(ManifestEntry::seat));
        return manifest;
    }

    // ---------------------------------------------------------------- Apoyo

    private static void requirePassengers(List<PassengerData> passengers) {
        Validator.requireNotNull(passengers, "pasajeros");
        if (passengers.size() < Reservation.MIN_PASSENGERS) {
            throw new ValidationException(
                    "La reserva debe tener al menos %d pasajero.".formatted(Reservation.MIN_PASSENGERS));
        }
        if (passengers.size() > Reservation.MAX_PASSENGERS) {
            throw new BusinessRuleException(
                    "La reserva no puede tener más de %d pasajeros.".formatted(Reservation.MAX_PASSENGERS));
        }
    }

    private ReservationDetail toDetail(PassengerData passenger) {
        Validator.requireNotNull(passenger, "pasajero");
        return new ReservationDetail(detailIds.next(), passenger.fullName(), passenger.document(), passenger.seat());
    }

    private static ReservationDetail requireDetail(Reservation reservation, int detailId) {
        return reservation.findDetail(detailId)
                .orElseThrow(() -> new NotFoundException(PASSENGER_ENTITY, detailId));
    }

    /**
     * Rechaza la silla si la ocupa algún pasajero del vuelo, sin contar la reserva indicada.
     *
     * @param excludedReservationId reserva que no se cuenta (0 para contar todas)
     */
    private void requireSeatFree(int flightId, Seat seat, int excludedReservationId) {
        if (occupiedSeats(flightId, excludedReservationId).contains(seat)) {
            throw new BusinessRuleException(
                    "La silla %s ya está ocupada en este vuelo.".formatted(seat));
        }
    }

    private Set<Seat> occupiedSeats(int flightId, int excludedReservationId) {
        Set<Seat> seats = new HashSet<>();
        for (Reservation reservation : repository.findByFlight(flightId)) {
            if (reservation.getId() != excludedReservationId) {
                seats.addAll(reservation.getSeats());
            }
        }
        return seats;
    }

    private static Flight requireNotDeparted(Flight flight) {
        if (!departureOf(flight).isAfter(LocalDateTime.now())) {
            throw new BusinessRuleException(
                    "El vuelo %s ya salió; no admite reservas.".formatted(flight.getFlightName()));
        }
        return flight;
    }

    private static int flightIdOf(Reservation reservation) {
        return reservation.getFlight().getId();
    }

    private static LocalDateTime departureOf(Flight flight) {
        return LocalDateTime.of(flight.getDepartureDate(), flight.getDepartureTime());
    }
}
