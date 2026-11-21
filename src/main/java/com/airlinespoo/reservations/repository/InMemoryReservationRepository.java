package com.airlinespoo.reservations.repository;

import com.airlinespoo.core.repository.InMemoryRepository;
import com.airlinespoo.reservations.model.Reservation;

import java.util.ArrayList;
import java.util.List;

/**
 * Repositorio en memoria de reservas (tablas {@code tblReservations} y
 * {@code tblDetailsReservation}).
 *
 * <p>Los detalles <b>no</b> tienen repositorio propio: viven dentro de su reserva
 * (composición), así que se guardan y se eliminan junto con ella.</p>
 *
 * <p>Solo accede a los datos: no aplica reglas de negocio ni lanza "no encontrado".</p>
 *
 * <p>Implementa {@link ReservationRepository}; la version con base de datos es
 * {@code JdbcReservationRepository}.</p>
 */
public class InMemoryReservationRepository extends InMemoryRepository<Reservation, Integer>
        implements ReservationRepository {

    private static final String ENTITY_NAME = "reserva";

    @Override
    protected String entityName() {
        return ENTITY_NAME;
    }

    /**
     * Equivale a {@code SELECT … WHERE CustomerId = ?}.
     *
     * @param customerId documento del cliente
     * @return copia con las reservas del cliente (vacía si no tiene)
     */
    @Override
    public List<Reservation> findByCustomer(String customerId) {
        List<Reservation> result = new ArrayList<>();
        for (Reservation reservation : values()) {
            if (reservation.getCustomer().getId().equals(customerId)) {
                result.add(reservation);
            }
        }
        return result;
    }

    /**
     * Equivale a {@code SELECT … WHERE FlightId = ?}.
     *
     * @param flightId id del vuelo
     * @return copia con las reservas del vuelo (vacía si no tiene)
     */
    @Override
    public List<Reservation> findByFlight(int flightId) {
        List<Reservation> result = new ArrayList<>();
        for (Reservation reservation : values()) {
            if (reservation.getFlight().getId() == flightId) {
                result.add(reservation);
            }
        }
        return result;
    }

    /**
     * Equivale a {@code SELECT 1 … WHERE CustomerId = ?}.
     *
     * @param customerId documento del cliente
     * @return {@code true} si el cliente tiene al menos una reserva
     */
    @Override
    public boolean existsByCustomer(String customerId) {
        return !findByCustomer(customerId).isEmpty();
    }

    /**
     * Equivale a {@code SELECT 1 … WHERE FlightId = ?}.
     *
     * @param flightId id del vuelo
     * @return {@code true} si el vuelo tiene al menos una reserva
     */
    @Override
    public boolean existsByFlight(int flightId) {
        return !findByFlight(flightId).isEmpty();
    }
}
