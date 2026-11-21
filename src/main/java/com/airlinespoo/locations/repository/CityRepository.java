package com.airlinespoo.locations.repository;

import com.airlinespoo.core.exception.AirlinesException;
import com.airlinespoo.core.exception.DuplicateException;
import com.airlinespoo.core.exception.NotFoundException;
import com.airlinespoo.core.util.Validator;
import com.airlinespoo.locations.model.City;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CityRepository implements ICityRepository {

    private final PostgresConnection database;

    public CityRepository(PostgresConnection database) {
        this.database = Validator.requireNotNull(database, "conexión de datos");
    }

    @Override
    public City create(City city) {
        Validator.requireNotNull(city, "ciudad");
        String sql = "INSERT INTO tblCities (CityId, name) VALUES (?, ?)";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, city.getId());
            statement.setString(2, city.getName());
            statement.executeUpdate();
            return city;
        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState())) {
                throw new DuplicateException("Ya existe una ciudad con los datos proporcionados.");
            }
            throw new AirlinesException("No se pudo guardar la ciudad.", e);
        }
    }

    @Override
    public Optional<City> findById(Integer id) {
        Validator.requireNotNull(id, "id de la ciudad");
        String sql = "SELECT CityId, name FROM tblCities WHERE CityId = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(readCity(result)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new AirlinesException("No se pudo consultar la ciudad.", e);
        }
    }

    @Override
    public List<City> findAll() {
        String sql = "SELECT CityId, name FROM tblCities ORDER BY name";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            return readCities(result);
        } catch (SQLException e) {
            throw new AirlinesException("No se pudieron listar las ciudades.", e);
        }
    }

    @Override
    public City update(City city) {
        Validator.requireNotNull(city, "ciudad");
        String sql = "UPDATE tblCities SET name = ? WHERE CityId = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, city.getName());
            statement.setInt(2, city.getId());
            if (statement.executeUpdate() == 0) {
                throw new NotFoundException("ciudad", city.getId());
            }
            return city;
        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState())) {
                throw new DuplicateException("Ya existe una ciudad con los datos proporcionados.");
            }
            throw new AirlinesException("No se pudo actualizar la ciudad.", e);
        }
    }

    @Override
    public boolean deleteById(Integer id) {
        Validator.requireNotNull(id, "id de la ciudad");
        String sql = "DELETE FROM tblCities WHERE CityId = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new AirlinesException("No se pudo eliminar la ciudad.", e);
        }
    }

    @Override
    public boolean existsById(Integer id) {
        Validator.requireNotNull(id, "id de la ciudad");
        String sql = "SELECT 1 FROM tblCities WHERE CityId = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        } catch (SQLException e) {
            throw new AirlinesException("No se pudo verificar la ciudad.", e);
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM tblCities";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            result.next();
            return result.getLong(1);
        } catch (SQLException e) {
            throw new AirlinesException("No se pudieron contar las ciudades.", e);
        }
    }

    @Override
    public List<City> searchCitiesByName(String name) {
        Validator.requireNotNull(name, "nombre de búsqueda");
        String sql = "SELECT CityId, name FROM tblCities WHERE name ILIKE ? ESCAPE '!' ORDER BY name";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, "%" + escapeLike(name) + "%");
            try (ResultSet result = statement.executeQuery()) {
                return readCities(result);
            }
        } catch (SQLException e) {
            throw new AirlinesException("No se pudieron buscar las ciudades.", e);
        }
    }

    private City readCity(ResultSet result) throws SQLException {
        return new City(result.getInt("CityId"), result.getString("name"));
    }

    private List<City> readCities(ResultSet result) throws SQLException {
        List<City> cities = new ArrayList<>();
        while (result.next()) {
            cities.add(readCity(result));
        }
        return cities;
    }

    private String escapeLike(String text) {
        return text.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
