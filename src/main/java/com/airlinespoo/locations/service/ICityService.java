package com.airlinespoo.locations.service;

import com.airlinespoo.locations.model.City;

import java.util.List;

/**
 * Operaciones de negocio para gestionar ciudades.
 */
public interface ICityService {

    City createCity(String name);

    City getCity(int id);

    List<City> listCities();

    City updateCity(int id, String name);

    void deleteCity(int id);

    List<City> searchCitiesByName(String name);
}
