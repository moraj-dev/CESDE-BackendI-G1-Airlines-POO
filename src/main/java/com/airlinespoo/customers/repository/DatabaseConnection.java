package com.airlinespoo.customers.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Conexion que usan los repositorios del proyecto. */
public final class DatabaseConnection {

    // Datos de la base creada en pgAdmin.
    private static final String URL = "jdbc:postgresql://localhost:5432/airlinespoo";
    private static final String USER = "postgres";
    private static final String PASSWORD = "123";

    private DatabaseConnection() {
    }

    // Abre una conexion nueva cuando un repositorio la necesita.
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
