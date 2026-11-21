
package com.airlinespoo.customers.repository;

import com.airlinespoo.customers.repository.DatabaseConnection;
import com.airlinespoo.core.exception.AirlinesException;
import com.airlinespoo.core.exception.DuplicateException;
import com.airlinespoo.core.exception.NotFoundException;
import com.airlinespoo.core.util.Validator;
import com.airlinespoo.customers.model.Country;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CountryRepository implements ICountryRepository {

    private static final String SELECT_COUNTRY =
        "SELECT countryid, name FROM tblcountries";

    private Connection openConnection() throws SQLException {
        return DatabaseConnection.getConnection();
    }

    @Override
    public Country create(Country country) {
        Validator.requireNotNull(country, "país");

        String sql = "INSERT INTO tblcountries (countryid, name) VALUES (?, ?)";

        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setShort(1, country.getId());
            ps.setString(2, country.getName());
            ps.executeUpdate();

            return country;

        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState())) {
                throw new DuplicateException("país", country.getId());
            }

            throw new AirlinesException(
                "Error en BD al guardar el país: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Country> findById(Short id) {
        if (id == null) return Optional.empty();

        return query(SELECT_COUNTRY + " WHERE countryid = ?", id)
            .stream().findFirst();
    }

    @Override
    public List<Country> findAll() {
        return query(SELECT_COUNTRY + " ORDER BY countryid");
    }

    @Override
    public Country update(Country country) {
        Validator.requireNotNull(country, "país");

        String sql = "UPDATE tblcountries SET name = ? WHERE countryid = ?";

        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, country.getName());
            ps.setShort(2, country.getId());

            if (ps.executeUpdate() == 0) {
                throw new NotFoundException("país", country.getId());
            }

            return country;

        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState())) {
                throw new DuplicateException("país", country.getId());
            }

            throw new AirlinesException(
                "Error en BD al actualizar el país: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteById(Short id) {
        if (id == null) return false;

        String sql = "DELETE FROM tblcountries WHERE countryid = ?";

        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setShort(1, id);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new AirlinesException(
                "Error en BD al eliminar el país: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsById(Short id) {
        return findById(id).isPresent();
    }

    @Override
    public long count() {
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(
                 "SELECT COUNT(*) FROM tblcountries");
             ResultSet rs = ps.executeQuery()) {

            rs.next();
            return rs.getLong(1);

        } catch (SQLException e) {
            throw new AirlinesException(
                "Error en BD al contar países: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Country> findByName(String name) {
        String clean = Validator.normalize(name);

        if (clean == null) return Optional.empty();

        return query(SELECT_COUNTRY + " WHERE LOWER(name) = LOWER(?)", clean)
            .stream().findFirst();
    }

    @Override
    public boolean existsByName(String name) {
        return findByName(name).isPresent();
    }

    @Override
    public List<Country> findByNameContaining(String text) {
        String clean = Validator.normalize(text);

        if (clean == null) return List.of();

        return query(
            SELECT_COUNTRY + " WHERE LOWER(name) LIKE ? ORDER BY countryid",
            "%" + clean.toLowerCase() + "%"
        );
    }

    private List<Country> query(String sql, Object... params) {
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }

            try (ResultSet rs = ps.executeQuery()) {
                List<Country> list = new ArrayList<>();

                while (rs.next()) {
                    list.add(new Country(
                        rs.getShort("countryid"),
                        rs.getString("name")
                    ));
                }

                return list;
            }

        } catch (SQLException e) {
            throw new AirlinesException(
                "Error en BD al consultar países: " + e.getMessage(), e);
        }
    }
}
