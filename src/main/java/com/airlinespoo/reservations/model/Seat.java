package com.airlinespoo.reservations.model;

import com.airlinespoo.core.exception.ValidationException;
import com.airlinespoo.core.util.Validator;

import java.util.Comparator;
import java.util.Objects;

/**
 * Silla de un vuelo: agrupa en un solo objeto la fila y el número que la base de datos
 * guarda en dos columnas ({@code RowNumber} y {@code SeatNumer}).
 *
 * <p>Es un <b>objeto de valor inmutable</b>: no tiene setters y dos sillas son iguales
 * cuando coinciden fila y número, sin importar cuántas veces se creen. Si un pasajero
 * cambia de silla, se reemplaza la silla completa.</p>
 *
 * <p><b>Decisión del equipo sobre la fila:</b> la fila son 1 o 2 <i>letras</i> (A, B... AA)
 * y el número va de 1 a {@value #MAX_SEAT_NUMBER}. La silla se muestra como número + fila,
 * por ejemplo {@code 12B}.</p>
 */
public final class Seat implements Comparable<Seat> {

    /** Longitud máxima de la fila (columna {@code CHAR(2)}). */
    public static final int MAX_ROW_LENGTH = 2;
    /** Número de silla más alto permitido. */
    public static final int MAX_SEAT_NUMBER = 99;

    private static final int MIN_SEAT_NUMBER = 1;
    private static final String ROW_PATTERN = "[A-Z]+";
    private static final String ROW_FIELD = "fila de la silla";
    private static final String NUMBER_FIELD = "número de la silla";

    private static final Comparator<Seat> ORDER = Comparator
            .comparingInt((Seat seat) -> seat.rowNumber.length())
            .thenComparing(seat -> seat.rowNumber)
            .thenComparingInt(seat -> seat.seatNumber);

    private final String rowNumber;
    private final int seatNumber;

    /**
     * Crea una silla validando fila y número.
     *
     * @param rowNumber  fila: 1 o 2 letras; se normaliza a mayúsculas y sin espacios
     * @param seatNumber número de la silla, entre 1 y {@value #MAX_SEAT_NUMBER}
     * @throws ValidationException si la fila o el número no son válidos
     */
    public Seat(String rowNumber, int seatNumber) {
        this.rowNumber = validateRow(rowNumber);
        this.seatNumber = (int) Validator.requireInRange(
                seatNumber, MIN_SEAT_NUMBER, MAX_SEAT_NUMBER, NUMBER_FIELD);
    }

    private static String validateRow(String rowNumber) {
        String row = Validator.requireText(rowNumber, ROW_FIELD).replace(" ", "").toUpperCase();
        Validator.requireMaxLength(row, MAX_ROW_LENGTH, ROW_FIELD);
        if (!row.matches(ROW_PATTERN)) {
            throw new ValidationException(
                    "El campo '%s' solo admite letras (por ejemplo, A o AB).".formatted(ROW_FIELD));
        }
        return row;
    }

    /**
     * @return la fila, en mayúsculas
     */
    public String getRowNumber() {
        return rowNumber;
    }

    /**
     * @return el número de la silla
     */
    public int getSeatNumber() {
        return seatNumber;
    }

    /**
     * Ordena por fila (A, B... Z, AA...) y luego por número.
     *
     * @param other silla con la que se compara
     * @return negativo, cero o positivo según el orden
     */
    @Override
    public int compareTo(Seat other) {
        return ORDER.compare(this, other);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Seat other)) {
            return false;
        }
        return seatNumber == other.seatNumber && rowNumber.equals(other.rowNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rowNumber, seatNumber);
    }

    /**
     * @return la silla como número + fila, por ejemplo {@code 12B}
     */
    @Override
    public String toString() {
        return seatNumber + rowNumber;
    }
}
