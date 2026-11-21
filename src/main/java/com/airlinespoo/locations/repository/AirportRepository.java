package com.airlinespoo.locations.repository;

import com.airlinespoo.core.exception.AirlinesException;
import com.airlinespoo.core.exception.DuplicateException;
import com.airlinespoo.core.exception.NotFoundException;
import com.airlinespoo.core.util.Validator;
import com.airlinespoo.locations.model.Airport;
import com.airlinespoo.locations.model.City;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class AirportRepository implements IAirportRepository {

    private static final String AIRPORT_SELECT =
            "SELECT a.AirportId, a.name AS airport_name, a.CityId, c.name AS city_name "
                    + "FROM tblAirports a LEFT JOIN tblCities c ON a.CityId = c.CityId";

    private final PostgresConnection database;

    public AirportRepository(PostgresConnection database) {
        this.database = Validator.requireNotNull(database, "conexión de datos");
    }

    @Override
    public Airport create(Airport airport) {
        Validator.requireNotNull(airport, "aeropuerto");
        String sql = "INSERT INTO tblAirports (AirportId, name, CityId) VALUES (?, ?, ?)";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, airport.getId());
            statement.setString(2, airport.getName());
            statement.setInt(3, airport.getCity().getId());
            statement.executeUpdate();
            return airport;
        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState())) {
                throw new DuplicateException("Ya existe un aeropuerto con los datos proporcionados.");
            }
            throw new AirlinesException("No se pudo guardar el aeropuerto.", e);
        }
    }

    @Override
    public Optional<Airport> findById(UUID id) {
        Validator.requireNotNull(id, "id del aeropuerto");
        String sql = AIRPORT_SELECT + " WHERE a.AirportId = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(readAirport(result)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new AirlinesException("No se pudo consultar el aeropuerto.", e);
        }
    }

    @Override
    public List<Airport> findAll() {
        String sql = AIRPORT_SELECT + " ORDER BY a.name";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            return readAirports(result);
        } catch (SQLException e) {
            throw new AirlinesException("No se pudieron listar los aeropuertos.", e);
        }
    }

    @Override
    public Airport update(Airport airport) {
        Validator.requireNotNull(airport, "aeropuerto");
        String sql = "UPDATE tblAirports SET name = ?, CityId = ? WHERE AirportId = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, airport.getName());
            statement.setInt(2, airport.getCity().getId());
            statement.setObject(3, airport.getId());
            if (statement.executeUpdate() == 0) {
                throw new NotFoundException("aeropuerto", airport.getId());
            }
            return airport;
        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState())) {
                throw new DuplicateException("Ya existe un aeropuerto con los datos proporcionados.");
            }
            throw new AirlinesException("No se pudo actualizar el aeropuerto.", e);
        }
    }

    @Override
    public boolean deleteById(UUID id) {
        Validator.requireNotNull(id, "id del aeropuerto");
        String sql = "DELETE FROM tblAirports WHERE AirportId = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new AirlinesException("No se pudo eliminar el aeropuerto.", e);
        }
    }

    @Override
    public boolean existsById(UUID id) {
        Validator.requireNotNull(id, "id del aeropuerto");
        String sql = "SELECT 1 FROM tblAirports WHERE AirportId = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        } catch (SQLException e) {
            throw new AirlinesException("No se pudo verificar el aeropuerto.", e);
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM tblAirports";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            result.next();
            return result.getLong(1);
        } catch (SQLException e) {
            throw new AirlinesException("No se pudieron contar los aeropuertos.", e);
        }
    }

    @Override
    public List<Airport> listAirportsByCity(int cityId) {
        String sql = AIRPORT_SELECT + " WHERE a.CityId = ? ORDER BY a.name";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, cityId);
            try (ResultSet result = statement.executeQuery()) {
                return readAirports(result);
            }
        } catch (SQLException e) {
            throw new AirlinesException("No se pudieron listar los aeropuertos de la ciudad.", e);
        }
    }

    @Override
    public List<Airport> searchAirportsByName(String name) {
        Validator.requireNotNull(name, "nombre de búsqueda");
        String sql = AIRPORT_SELECT + " WHERE a.name ILIKE ? ESCAPE '!' ORDER BY a.name";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, "%" + escapeLike(name) + "%");
            try (ResultSet result = statement.executeQuery()) {
                return readAirports(result);
            }
        } catch (SQLException e) {
            throw new AirlinesException("No se pudieron buscar los aeropuertos.", e);
        }
    }

    @Override
    public long countAirportsByCity(int cityId) {
        String sql = "SELECT COUNT(*) FROM tblAirports WHERE CityId = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, cityId);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getLong(1);
            }
        } catch (SQLException e) {
            throw new AirlinesException("No se pudieron contar los aeropuertos de la ciudad.", e);
        }
    }

    @Override
    public boolean existsByCityId(int cityId) {
        String sql = "SELECT 1 FROM tblAirports WHERE CityId = ? LIMIT 1";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, cityId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        } catch (SQLException e) {
            throw new AirlinesException("No se pudo verificar si la ciudad tiene aeropuertos.", e);
        }
    }

    private Airport readAirport(ResultSet result) throws SQLException {
        int cityId = result.getInt("CityId");
        String cityName = result.getString("city_name");
        if (result.wasNull()) {
            throw new AirlinesException("El aeropuerto no tiene una ciudad válida.");
        }
        City city = new City(cityId, cityName);
        return new Airport(result.getObject("AirportId", UUID.class),
                result.getString("airport_name"), city);
    }

    private List<Airport> readAirports(ResultSet result) throws SQLException {
        List<Airport> airports = new ArrayList<>();
        while (result.next()) {
            airports.add(readAirport(result));
        }
        return airports;
    }

    private String escapeLike(String text) {
        return text.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
