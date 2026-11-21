package com.airlinespoo.airlines.service;

/** Datos iniciales para probar el módulo de aerolíneas. */
public class AirlineSampleData {

    public static void loadIfEmpty(AirlineService airlines) {
        // Con PostgreSQL no se deben volver a cargar en cada arranque.
        if (airlines.countAirlines() != 0) return;

        airlines.createAirline("Aerolínea Andina");
        airlines.createAirline("Aerolínea del Norte");
        airlines.createAirline("Aerolínea del Sur");
    }
}
