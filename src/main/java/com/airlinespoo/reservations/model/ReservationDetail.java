package com.airlinespoo.reservations.model;

import com.airlinespoo.core.exception.ValidationException;
import com.airlinespoo.core.util.Validator;

/**
 * Detalle de una reserva: un pasajero con su silla (tabla {@code tblDetailsReservation}).
 *
 * <p>Forma parte de una {@code Reservation} por <b>composición</b>: nunca vive sola y no
 * guarda el vuelo, porque eso ya lo sabe la reserva que la contiene.</p>
 *
 * <p>Los datos del pasajero y la silla solo se cambian desde la reserva (sus setters son
 * de paquete), porque es ella quien verifica que no se repitan documentos ni sillas.</p>
 *
 * <p>Regla del documento: se quitan todos los espacios, se pasa a mayúsculas y solo se
 * admiten letras y dígitos.</p>
 */
public final class ReservationDetail {

    /** Longitud máxima del nombre completo del pasajero. */
    public static final int MAX_FULL_NAME_LENGTH = 50;
    /** Longitud máxima del documento del pasajero. */
    public static final int MAX_DOCUMENT_LENGTH = 20;

    private static final String ID_FIELD = "id del detalle de reserva";
    private static final String FULL_NAME_FIELD = "nombre completo del pasajero";
    private static final String DOCUMENT_FIELD = "documento del pasajero";
    private static final String SEAT_FIELD = "silla";
    private static final String DOCUMENT_PATTERN = "[A-Z0-9]+";

    private final int id;
    private String passengerFullName;
    private String passengerDocument;
    private Seat seat;

    /**
     * Crea el detalle validando todos sus datos.
     *
     * @param id                identificador asignado por el programa (mayor que cero)
     * @param passengerFullName nombre completo del pasajero, máximo {@value #MAX_FULL_NAME_LENGTH}
     * @param passengerDocument documento del pasajero, máximo {@value #MAX_DOCUMENT_LENGTH}
     * @param seat              silla asignada
     * @throws ValidationException si algún dato no es válido
     */
    public ReservationDetail(int id, String passengerFullName, String passengerDocument, Seat seat) {
        this.id = (int) Validator.requirePositive(id, ID_FIELD);
        setPassengerFullName(passengerFullName);
        setPassengerDocument(passengerDocument);
        setSeat(seat);
    }

    /**
     * Normaliza un documento con la misma regla que usa el detalle, para que otras clases
     * (la reserva, el servicio) comparen documentos de forma coherente.
     *
     * @param document documento escrito por el usuario
     * @return el documento sin espacios y en mayúsculas
     * @throws ValidationException si está vacío, es muy largo o tiene caracteres no permitidos
     */
    public static String normalizeDocument(String document) {
        String normalized = Validator.requireText(document, DOCUMENT_FIELD)
                .replace(" ", "")
                .toUpperCase();
        Validator.requireMaxLength(normalized, MAX_DOCUMENT_LENGTH, DOCUMENT_FIELD);
        if (!normalized.matches(DOCUMENT_PATTERN)) {
            throw new ValidationException(
                    "El campo '%s' solo admite letras y dígitos.".formatted(DOCUMENT_FIELD));
        }
        return normalized;
    }

    /**
     * @return el identificador del detalle; nunca cambia
     */
    public int getId() {
        return id;
    }

    /**
     * @return el nombre completo del pasajero
     */
    public String getPassengerFullName() {
        return passengerFullName;
    }

    /**
     * @return el documento del pasajero, normalizado
     */
    public String getPassengerDocument() {
        return passengerDocument;
    }

    /**
     * @return la silla asignada
     */
    public Seat getSeat() {
        return seat;
    }

    void setPassengerFullName(String passengerFullName) {
        this.passengerFullName = Validator.requireText(passengerFullName, FULL_NAME_FIELD, MAX_FULL_NAME_LENGTH);
    }

    void setPassengerDocument(String passengerDocument) {
        this.passengerDocument = normalizeDocument(passengerDocument);
    }

    void setSeat(Seat seat) {
        this.seat = Validator.requireNotNull(seat, SEAT_FIELD);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReservationDetail other)) {
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
        return "Pasajero #%d: %s (doc. %s) - silla %s".formatted(id, passengerFullName, passengerDocument, seat);
    }
}
