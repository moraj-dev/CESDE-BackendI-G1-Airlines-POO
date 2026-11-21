package com.airlinespoo.reservations.repository;

import com.airlinespoo.core.repository.Repository;
import com.airlinespoo.reservations.model.Reservation;

import java.util.List;

/**
 * Contrato del repositorio de reservas (tablas {@code tblReservations} y
 * {@code tblDetailsReservation}).
 *
 * <p>Hereda el CRUD comun de {@link Repository} y agrega las busquedas propias del modulo.
 * El servicio depende de esta interfaz, no de una clase concreta: por eso se puede cambiar
 * la version en memoria ({@link InMemoryReservationRepository}) por la de base de datos
 * ({@code JdbcReservationRepository}) sin tocar el servicio ni el menu.</p>
 *
 * <p>Los detalles no tienen repositorio propio: se guardan y se eliminan junto con su
 * reserva (composicion).</p>
 */
public interface ReservationRepository extends Repository<Reservation, Integer> {

    /**
     * Equivale a {@code SELECT ... WHERE CustomerId = ?}.
     *
     * @param customerId documento del cliente
     * @return reservas del cliente (vacia si no tiene)
     */
    List<Reservation> findByCustomer(String customerId);

    /**
     * Equivale a {@code SELECT ... WHERE FlightId = ?}.
     *
     * @param flightId id del vuelo
     * @return reservas del vuelo (vacia si no tiene)
     */
    List<Reservation> findByFlight(int flightId);

    /**
     * Equivale a {@code SELECT 1 ... WHERE CustomerId = ?}.
     *
     * @param customerId documento del cliente
     * @return {@code true} si el cliente tiene al menos una reserva
     */
    boolean existsByCustomer(String customerId);

    /**
     * Equivale a {@code SELECT 1 ... WHERE FlightId = ?}.
     *
     * @param flightId id del vuelo
     * @return {@code true} si el vuelo tiene al menos una reserva
     */
    boolean existsByFlight(int flightId);
}
