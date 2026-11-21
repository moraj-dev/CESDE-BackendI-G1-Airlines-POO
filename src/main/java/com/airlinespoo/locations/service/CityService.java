package com.airlinespoo.locations.service;

import com.airlinespoo.core.exception.BusinessRuleException;
import com.airlinespoo.core.exception.DuplicateException;
import com.airlinespoo.core.exception.NotFoundException;
import com.airlinespoo.core.util.IdGenerator;
import com.airlinespoo.core.util.Validator;
import com.airlinespoo.locations.model.City;
import com.airlinespoo.locations.repository.IAirportRepository;
import com.airlinespoo.locations.repository.ICityRepository;

import java.util.List;

public class CityService implements ICityService {

    private final ICityRepository cityRepository;
    private final IAirportRepository airportRepository;
    private final IdGenerator idGenerator;

    public CityService(ICityRepository cityRepository, IAirportRepository airportRepository) {
        this.cityRepository = Validator.requireNotNull(cityRepository, "repositorio de ciudades");
        this.airportRepository = Validator.requireNotNull(airportRepository, "repositorio de aeropuertos");
        this.idGenerator = new IdGenerator(1, 1);
    }

    @Override
    public City createCity(String name) {
        String validName = Validator.requireText(name, "nombre de la ciudad", 20);
        checkUniqueName(validName, null);

        List<City> cities = cityRepository.findAll();
        for (City city : cities) {
            if (city.getId() == Integer.MAX_VALUE) {
                throw new BusinessRuleException("No quedan identificadores disponibles para ciudades.");
            }
            idGenerator.syncWith(city.getId());
        }

        int id = idGenerator.next();
        City city = new City(id, validName);
        return cityRepository.create(city);
    }

    @Override
    public City getCity(int id) {
        return cityRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("ciudad", id));
    }

    @Override
    public List<City> listCities() {
        return cityRepository.findAll();
    }

    @Override
    public City updateCity(int id, String name) {
        City existingCity = getCity(id);
        String validName = Validator.requireText(name, "nombre de la ciudad", 20);
        checkUniqueName(validName, existingCity.getId());

        City updatedCity = new City(existingCity.getId(), validName);
        return cityRepository.update(updatedCity);
    }

    @Override
    public void deleteCity(int id) {
        getCity(id);
        if (airportRepository.existsByCityId(id)) {
            throw new BusinessRuleException("No se puede eliminar una ciudad que tiene aeropuertos.");
        }
        if (!cityRepository.deleteById(id)) {
            throw new NotFoundException("ciudad", id);
        }
    }

    @Override
    public List<City> searchCitiesByName(String name) {
        String search = Validator.requireText(name, "nombre de búsqueda");
        return cityRepository.searchCitiesByName(search);
    }

    private void checkUniqueName(String name, Integer currentId) {
        List<City> cities = cityRepository.findAll();
        for (City city : cities) {
            boolean sameCity = city.getId().equals(currentId);
            if (!sameCity && city.getName().equalsIgnoreCase(name)) {
                throw new DuplicateException("Ya existe una ciudad con el nombre '" + name + "'.");
            }
        }
    }
}
