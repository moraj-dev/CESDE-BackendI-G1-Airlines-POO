package com.airlinespoo.reservations.repository;

import com.airlinespoo.core.exception.AirlinesException;
import com.airlinespoo.core.exception.DuplicateException;
import com.airlinespoo.core.exception.NotFoundException;
import com.airlinespoo.core.util.Validator;
import com.airlinespoo.customers.model.Customer;
import com.airlinespoo.flights.model.Flight;
import com.airlinespoo.reservations.model.Reservation;
import com.airlinespoo.reservations.model.ReservationDetail;
import com.airlinespoo.reservations.model.Seat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Repositorio de reservas con JDBC (tablas {@code tblReservations} y
 * {@code tblDetailsReservation}). Implementa el mismo contrato que la versión en memoria,
 * así que el servicio no nota el cambio.
 *
 * <p><b>Composición en SQL:</b> una reserva y sus detalles se guardan juntos. Por eso
 * {@code create}, {@code update} y {@code deleteById} trabajan sobre las dos tablas dentro de
 * una <b>transacción</b>: o se guardan todos los cambios, o ninguno.</p>
 *
 * <p><b>La conexión no se crea aquí:</b> la entrega quien arma la aplicación (Main), a través
 * de {@link ConnectionSource}. Así este módulo no conoce la URL, el usuario ni la contraseña,
 * y el equipo puede usar una sola clase de conexión para todos los módulos.</p>
 *
 * <p>Las tablas solo guardan {@code CustomerId} y {@code FlightId}. Para reconstruir los
 * objetos {@link Customer} y {@link Flight} el repositorio recibe dos funciones de búsqueda
 * (por ejemplo {@code customerService::getCustomer} y {@code flightService::getFlight}).
 * Así no depende de las clases concretas de los Módulos 1 y 4.</p>
 *
 * <p>Las columnas conservan los errores del esquema ({@code PassangerFullname},
 * {@code PassagerDocument}, {@code SeatNumer}); en Java los atributos están bien escritos.
 * Los nombres van sin comillas, por eso el mismo SQL sirve en PostgreSQL y en SQL Server.</p>
 */
public class JdbcReservationRepository implements ReservationRepository {

    private static final String ENTITY_NAME = "reserva";

    private static final String SELECT_RESERVATIONS =
            "SELECT ReservationId, CustomerId, FlightId FROM tblReservations";
    private static final String SELECT_DETAILS =
            "SELECT DetailId, PassangerFullname, PassagerDocument, SeatNumer, RowNumber "
                    + "FROM tblDetailsReservation WHERE ReservationId = ? ORDER BY DetailId";
    private static final String INSERT_RESERVATION =
            "INSERT INTO tblReservations (ReservationId, CustomerId, FlightId) VALUES (?, ?, ?)";
    private static final String INSERT_DETAIL =
            "INSERT INTO tblDetailsReservation "
                    + "(DetailId, ReservationId, PassangerFullname, PassagerDocument, SeatNumer, RowNumber) "
                    + "VALUES (?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_RESERVATION =
            "UPDATE tblReservations SET CustomerId = ?, FlightId = ? WHERE ReservationId = ?";
    private static final String DELETE_DETAILS =
            "DELETE FROM tblDetailsReservation WHERE ReservationId = ?";
    private static final String DELETE_RESERVATION =
            "DELETE FROM tblReservations WHERE ReservationId = ?";

    /**
     * Entrega conexiones abiertas a la base de datos. Quien arma la aplicación decide cuál usar,
     * por ejemplo {@code conexionDelEquipo::getConnection}.
     */
    @FunctionalInterface
    public interface ConnectionSource {
        Connection get() throws SQLException;
    }

    private final ConnectionSource connectionSource;
    private final Function<String, Customer> customerLookup;
    private final Function<Integer, Flight> flightLookup;

    /** Fila leída de {@code tblReservations}, antes de convertirla en objeto. */
    private record ReservationRow(int id, String customerId, int flightId) {
    }

    /**
     * @param connectionSource entrega las conexiones a la base de datos; por ejemplo
     *                         {@code conexionDelEquipo::getConnection}
     * @param customerLookup busca un cliente por su id (documento); por ejemplo
     *                       {@code customerService::getCustomer}
     * @param flightLookup   busca un vuelo por su id; por ejemplo {@code flightService::getFlight}
     */
    public JdbcReservationRepository(ConnectionSource connectionSource,
                                     Function<String, Customer> customerLookup,
                                     Function<Integer, Flight> flightLookup) {
        this.connectionSource = Validator.requireNotNull(connectionSource, "fuente de conexiones");
        this.customerLookup = Validator.requireNotNull(customerLookup, "búsqueda de clientes");
        this.flightLookup = Validator.requireNotNull(flightLookup, "búsqueda de vuelos");
    }

    // ---------------------------------------------------------------- Create

    /** INSERT en tblReservations y un INSERT en tblDetailsReservation por pasajero. */
    @Override
    public Reservation create(Reservation reservation) {
        Validator.requireNotNull(reservation, ENTITY_NAME);
        try (Connection conn = connectionSource.get()) {
            conn.setAutoCommit(false);
            try {
                if (exists(conn, "SELECT 1 FROM tblReservations WHERE ReservationId = ?", reservation.getId())) {
                    throw new DuplicateException(ENTITY_NAME, reservation.getId());
                }
                try (PreparedStatement ps = conn.prepareStatement(INSERT_RESERVATION)) {
                    ps.setInt(1, reservation.getId());
                    ps.setString(2, reservation.getCustomer().getId());
                    ps.setInt(3, reservation.getFlight().getId());
                    ps.executeUpdate();
                }
                insertDetails(conn, reservation);
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();   // si algo falla, no queda nada a medias
                throw e;
            }
        } catch (SQLException e) {
            throw databaseError("guardar la reserva", e);
        }
        return reservation;
    }

    // ---------------------------------------------------------------- Read

    /** SELECT de la reserva y SELECT de sus detalles. */
    @Override
    public Optional<Reservation> findById(Integer id) {
        List<Reservation> found = query(SELECT_RESERVATIONS + " WHERE ReservationId = ?", id);
        return found.isEmpty() ? Optional.empty() : Optional.of(found.get(0));
    }

    @Override
    public List<Reservation> findAll() {
        return query(SELECT_RESERVATIONS + " ORDER BY ReservationId", null);
    }

    @Override
    public List<Reservation> findByCustomer(String customerId) {
        return query(SELECT_RESERVATIONS + " WHERE CustomerId = ? ORDER BY ReservationId", customerId);
    }

    @Override
    public List<Reservation> findByFlight(int flightId) {
        return query(SELECT_RESERVATIONS + " WHERE FlightId = ? ORDER BY ReservationId", flightId);
    }

    @Override
    public boolean existsById(Integer id) {
        return exists("SELECT 1 FROM tblReservations WHERE ReservationId = ?", id);
    }

    @Override
    public boolean existsByCustomer(String customerId) {
        return exists("SELECT 1 FROM tblReservations WHERE CustomerId = ?", customerId);
    }

    @Override
    public boolean existsByFlight(int flightId) {
        return exists("SELECT 1 FROM tblReservations WHERE FlightId = ?", flightId);
    }

    @Override
    public long count() {
        try (Connection conn = connectionSource.get();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM tblReservations");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        } catch (SQLException e) {
            throw databaseError("contar las reservas", e);
        }
    }

    // ---------------------------------------------------------------- Update

    /**
     * UPDATE de la reserva y reemplazo de sus detalles (DELETE + INSERT). Es la forma más
     * simple de reflejar en la base de datos agregar, quitar o cambiar pasajeros.
     */
    @Override
    public Reservation update(Reservation reservation) {
        Validator.requireNotNull(reservation, ENTITY_NAME);
        try (Connection conn = connectionSource.get()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(UPDATE_RESERVATION)) {
                    ps.setString(1, reservation.getCustomer().getId());
                    ps.setInt(2, reservation.getFlight().getId());
                    ps.setInt(3, reservation.getId());
                    if (ps.executeUpdate() == 0) {
                        throw new NotFoundException(ENTITY_NAME, reservation.getId());
                    }
                }
                deleteDetails(conn, reservation.getId());
                insertDetails(conn, reservation);
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw databaseError("actualizar la reserva", e);
        }
        return reservation;
    }

    // ---------------------------------------------------------------- Delete

    /** Primero los detalles y luego la reserva: la clave foránea no tiene CASCADE. */
    @Override
    public boolean deleteById(Integer id) {
        try (Connection conn = connectionSource.get()) {
            conn.setAutoCommit(false);
            try {
                deleteDetails(conn, id);
                int deleted;
                try (PreparedStatement ps = conn.prepareStatement(DELETE_RESERVATION)) {
                    ps.setInt(1, id);
                    deleted = ps.executeUpdate();
                }
                conn.commit();
                return deleted > 0;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw databaseError("eliminar la reserva", e);
        }
    }

    // ---------------------------------------------------------------- Ids

    /**
     * Mayor id de reserva guardado (0 si no hay). Úsalo al arrancar para sincronizar el
     * generador: {@code reservationIds.syncWith(repo.maxReservationId())}.
     */
    public int maxReservationId() {
        return maxOf("SELECT COALESCE(MAX(ReservationId), 0) FROM tblReservations");
    }

    /** Mayor id de detalle guardado (0 si no hay). Sincroniza el generador de detalles. */
    public int maxDetailId() {
        return maxOf("SELECT COALESCE(MAX(DetailId), 0) FROM tblDetailsReservation");
    }

    // ---------------------------------------------------------------- Apoyo

    private List<Reservation> query(String sql, Object parameter) {
        try (Connection conn = connectionSource.get()) {
            List<ReservationRow> rows = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                if (parameter != null) {
                    ps.setObject(1, parameter);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        rows.add(new ReservationRow(
                                rs.getInt("ReservationId"),
                                rs.getString("CustomerId"),
                                rs.getInt("FlightId")));
                    }
                }
            }
            List<Reservation> reservations = new ArrayList<>();
            for (ReservationRow row : rows) {
                reservations.add(new Reservation(
                        row.id(),
                        customerLookup.apply(row.customerId()),
                        flightLookup.apply(row.flightId()),
                        loadDetails(conn, row.id())));
            }
            return reservations;
        } catch (SQLException e) {
            throw databaseError("consultar las reservas", e);
        }
    }

    private List<ReservationDetail> loadDetails(Connection conn, int reservationId) throws SQLException {
        List<ReservationDetail> details = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT_DETAILS)) {
            ps.setInt(1, reservationId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // RowNumber es CHAR(2): la base de datos rellena con espacios ("A "), Seat los quita.
                    Seat seat = new Seat(rs.getString("RowNumber"), rs.getInt("SeatNumer"));
                    details.add(new ReservationDetail(
                            rs.getInt("DetailId"),
                            rs.getString("PassangerFullname"),
                            rs.getString("PassagerDocument"),
                            seat));
                }
            }
        }
        return details;
    }

    private void insertDetails(Connection conn, Reservation reservation) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT_DETAIL)) {
            for (ReservationDetail detail : reservation.getDetails()) {
                ps.setInt(1, detail.getId());
                ps.setInt(2, reservation.getId());
                ps.setString(3, detail.getPassengerFullName());
                ps.setString(4, detail.getPassengerDocument());
                ps.setInt(5, detail.getSeat().getSeatNumber());
                ps.setString(6, detail.getSeat().getRowNumber());
                ps.addBatch();   // se envían todos los INSERT juntos
            }
            ps.executeBatch();
        }
    }

    private void deleteDetails(Connection conn, int reservationId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(DELETE_DETAILS)) {
            ps.setInt(1, reservationId);
            ps.executeUpdate();
        }
    }

    private boolean exists(String sql, Object parameter) {
        try (Connection conn = connectionSource.get()) {
            return exists(conn, sql, parameter);
        } catch (SQLException e) {
            throw databaseError("consultar las reservas", e);
        }
    }

    private boolean exists(Connection conn, String sql, Object parameter) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, parameter);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private int maxOf(String sql) {
        try (Connection conn = connectionSource.get();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw databaseError("consultar los ids", e);
        }
    }

    /**
     * Convierte el error de JDBC (checked) en una excepción del sistema (unchecked) que el
     * menú ya sabe mostrar.
     */
    private static AirlinesException databaseError(String action, SQLException e) {
        return new AirlinesException("Error de base de datos al " + action + ": " + e.getMessage(), e);
    }
}
