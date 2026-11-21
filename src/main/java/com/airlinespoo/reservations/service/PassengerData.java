package com.airlinespoo.reservations.service;

import com.airlinespoo.reservations.model.Seat;

/**
 * Datos de un pasajero que todavía no forma parte de una reserva.
 *
 * <p>La interfaz los entrega al servicio, y el servicio crea el
 * {@link com.airlinespoo.reservations.model.ReservationDetail} con un id del generador.
 * No tiene lógica: las validaciones las hace el detalle al crearse.</p>
 *
 * @param fullName nombre completo del pasajero
 * @param document documento del pasajero
 * @param seat     silla solicitada
 */
public record PassengerData(String fullName, String document, Seat seat) {
}
