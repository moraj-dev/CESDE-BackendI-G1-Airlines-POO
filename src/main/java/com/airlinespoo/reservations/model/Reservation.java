package com.airlinespoo.reservations.model;

import com.airlinespoo.core.exception.BusinessRuleException;
import com.airlinespoo.core.exception.DuplicateException;
import com.airlinespoo.core.exception.NotFoundException;
import com.airlinespoo.core.exception.ValidationException;
import com.airlinespoo.core.model.Identifiable;
import com.airlinespoo.core.util.Validator;
import com.airlinespoo.customers.model.Customer;
import com.airlinespoo.flights.model.Flight;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Reserva de un cliente sobre un vuelo (tabla {@code tblReservations}).
 *
 * <p>Es la <b>dueña</b> de sus detalles ({@link ReservationDetail}): esto es composición.
 * Si la reserva se elimina, sus detalles dejan de existir; por eso los pasajeros solo se
 * agregan, cambian o quitan a través de los métodos de la reserva.</p>
 *
 * <p>Reglas internas que verifica la propia reserva:</p>
 * <ul>
 *     <li>Debe tener entre {@value #MIN_PASSENGERS} y {@value #MAX_PASSENGERS} pasajeros.</li>
 *     <li>Un mismo documento de pasajero no se repite dentro de la reserva.</li>
 *     <li>Una misma silla no se repite dentro de la reserva.</li>
 *     <li>No se puede quitar el último pasajero.</li>
 * </ul>
 *
 * <p>La regla de que una silla no esté ocupada en <i>otras</i> reservas del mismo vuelo no
 * la puede verificar un solo objeto: la valida {@code ReservationService}.</p>
 */
public final class Reservation implements Identifiable<Integer> {

    /** Cantidad mínima de pasajeros de una reserva. */
    public static final int MIN_PASSENGERS = 1;
    /** Cantidad máxima de pasajeros de una reserva (decisión del equipo). */
    public static final int MAX_PASSENGERS = 9;

    private static final String ID_FIELD = "id de la reserva";
    private static final String CUSTOMER_FIELD = "cliente";
    private static final String FLIGHT_FIELD = "vuelo";
    private static final String DETAILS_FIELD = "pasajeros";
    private static final String DETAIL_FIELD = "pasajero";
    private static final String DETAIL_ENTITY = "pasajero de la reserva";

    private final int id;
    private final Customer customer;
    private Flight flight;
    private final List<ReservationDetail> details = new ArrayList<>();

    /**
     * Crea una reserva con sus pasajeros iniciales.
     *
     * @param id       identificador asignado por el programa (mayor que cero)
     * @param customer cliente que hace la reserva
     * @param flight   vuelo reservado
     * @param details  pasajeros iniciales: al menos {@value #MIN_PASSENGERS}
     * @throws ValidationException   si falta algún dato o no hay pasajeros
     * @throws DuplicateException    si un documento de pasajero se repite
     * @throws BusinessRuleException si una silla se repite o se supera el máximo
     */
    public Reservation(int id, Customer customer, Flight flight, List<ReservationDetail> details) {
        this.id = (int) Validator.requirePositive(id, ID_FIELD);
        this.customer = Validator.requireNotNull(customer, CUSTOMER_FIELD);
        changeFlight(flight);
        Validator.requireNotNull(details, DETAILS_FIELD);
        if (details.size() < MIN_PASSENGERS) {
            throw new ValidationException(
                    "La reserva debe tener al menos %d pasajero.".formatted(MIN_PASSENGERS));
        }
        for (ReservationDetail detail : details) {
            addPassenger(detail);
        }
    }

    /**
     * @return el identificador de la reserva; nunca cambia
     */
    @Override
    public Integer getId() {
        return id;
    }

    /**
     * @return el cliente que hizo la reserva
     */
    public Customer getCustomer() {
        return customer;
    }

    /**
     * @return el vuelo reservado
     */
    public Flight getFlight() {
        return flight;
    }

    /**
     * Cambia el vuelo de la reserva. Que el vuelo exista, no haya salido y tenga libres las
     * sillas de esta reserva lo verifica el servicio antes de llamar a este método.
     *
     * @param flight nuevo vuelo
     * @throws ValidationException si el vuelo es nulo
     */
    public void changeFlight(Flight flight) {
        this.flight = Validator.requireNotNull(flight, FLIGHT_FIELD);
    }

    /**
     * Devuelve los pasajeros en una lista de solo lectura: modificarla desde afuera no
     * afecta a la reserva.
     *
     * @return copia inmutable de los detalles
     */
    public List<ReservationDetail> getDetails() {
        return List.copyOf(details);
    }

    /**
     * @return la cantidad de pasajeros de la reserva
     */
    public int getPassengerCount() {
        return details.size();
    }

    /**
     * @return las sillas que ocupan los pasajeros de esta reserva (copia)
     */
    public Set<Seat> getSeats() {
        Set<Seat> seats = new HashSet<>();
        for (ReservationDetail detail : details) {
            seats.add(detail.getSeat());
        }
        return seats;
    }

    /**
     * Busca un pasajero de la reserva por el id del detalle.
     *
     * @param detailId id del detalle
     * @return el detalle o vacío si no pertenece a esta reserva
     */
    public Optional<ReservationDetail> findDetail(int detailId) {
        for (ReservationDetail detail : details) {
            if (detail.getId() == detailId) {
                return Optional.of(detail);
            }
        }
        return Optional.empty();
    }

    /**
     * Agrega un pasajero a la reserva.
     *
     * @param detail pasajero con su silla
     * @throws ValidationException   si el detalle es nulo
     * @throws DuplicateException    si el id del detalle o el documento ya están en la reserva
     * @throws BusinessRuleException si la silla ya está en la reserva o se supera el máximo
     */
    public void addPassenger(ReservationDetail detail) {
        Validator.requireNotNull(detail, DETAIL_FIELD);
        if (details.size() >= MAX_PASSENGERS) {
            throw new BusinessRuleException(
                    "La reserva no puede tener más de %d pasajeros.".formatted(MAX_PASSENGERS));
        }
        if (findDetail(detail.getId()).isPresent()) {
            throw new DuplicateException(DETAIL_ENTITY, detail.getId());
        }
        requireDocumentAvailable(detail.getPassengerDocument(), detail.getId());
        requireSeatAvailable(detail.getSeat(), detail.getId());
        details.add(detail);
    }

    /**
     * Quita un pasajero de la reserva.
     *
     * @param detailId id del detalle a quitar
     * @return el detalle quitado
     * @throws NotFoundException     si el pasajero no pertenece a la reserva
     * @throws BusinessRuleException si es el último pasajero
     */
    public ReservationDetail removePassenger(int detailId) {
        ReservationDetail detail = requireDetail(detailId);
        if (details.size() <= MIN_PASSENGERS) {
            throw new BusinessRuleException(
                    "No se puede quitar el último pasajero. Si ya no se necesita, cancela la reserva.");
        }
        details.remove(detail);
        return detail;
    }

    /**
     * Cambia la silla de un pasajero, reemplazando la silla completa.
     *
     * @param detailId id del detalle
     * @param newSeat  nueva silla
     * @throws NotFoundException     si el pasajero no pertenece a la reserva
     * @throws BusinessRuleException si otro pasajero de la reserva ya tiene esa silla
     */
    public void changeSeat(int detailId, Seat newSeat) {
        ReservationDetail detail = requireDetail(detailId);
        Validator.requireNotNull(newSeat, "silla");
        requireSeatAvailable(newSeat, detailId);
        detail.setSeat(newSeat);
    }

    /**
     * Actualiza el nombre y el documento de un pasajero.
     *
     * @param detailId id del detalle
     * @param fullName nuevo nombre completo
     * @param document nuevo documento
     * @throws NotFoundException  si el pasajero no pertenece a la reserva
     * @throws DuplicateException si el documento ya es de otro pasajero de la reserva
     */
    public void updatePassenger(int detailId, String fullName, String document) {
        ReservationDetail detail = requireDetail(detailId);
        String normalizedDocument = ReservationDetail.normalizeDocument(document);
        requireDocumentAvailable(normalizedDocument, detailId);
        detail.setPassengerFullName(fullName);
        detail.setPassengerDocument(normalizedDocument);
    }

    private ReservationDetail requireDetail(int detailId) {
        return findDetail(detailId).orElseThrow(() -> new NotFoundException(DETAIL_ENTITY, detailId));
    }

    private void requireDocumentAvailable(String document, int ownDetailId) {
        for (ReservationDetail other : details) {
            if (other.getId() != ownDetailId && other.getPassengerDocument().equals(document)) {
                throw new DuplicateException(
                        "El pasajero con documento %s ya está en la reserva.".formatted(document));
            }
        }
    }

    private void requireSeatAvailable(Seat seat, int ownDetailId) {
        for (ReservationDetail other : details) {
            if (other.getId() != ownDetailId && other.getSeat().equals(seat)) {
                throw new BusinessRuleException(
                        "La silla %s ya está asignada a otro pasajero de la reserva.".formatted(seat));
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Reservation other)) {
            return false;
        }
        return id == other.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public String toString() {
        return "Reserva #%d - cliente %s - vuelo %s - %d pasajero(s)"
                .formatted(id, customer.getId(), flight.getFlightName(), details.size());
    }
}
