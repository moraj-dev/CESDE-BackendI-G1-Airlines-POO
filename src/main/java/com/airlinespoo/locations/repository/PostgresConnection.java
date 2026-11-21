package com.airlinespoo.locations.repository;

import com.airlinespoo.customers.repository.DatabaseConnection;
import java.sql.Connection;
import java.sql.SQLException;

/** Mantiene el nombre que usa ubicaciones, pero la conexión real es una sola. */
public class PostgresConnection {
    public Connection getConnection() throws SQLException {
        return DatabaseConnection.getConnection();
    }
}
