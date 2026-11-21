package com.airlinespoo.reservations.service;

import com.airlinespoo.reservations.model.Seat;

/**
 * Fila del manifiesto de un vuelo: un pasajero con su silla.
 *
 * <p>Solo agrupa datos de lectura que viajan juntos hacia la interfaz; no tiene lógica
 * de negocio.</p>
 *
 * @param passengerFullName nombre completo del pasajero
 * @param passengerDocument documento del pasajero
 * @param seat              silla asignada
 */
public record ManifestEntry(String passengerFullName, String passengerDocument, Seat seat) {
}
