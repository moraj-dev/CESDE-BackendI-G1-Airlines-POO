package com.airlinespoo.locations.service;

import com.airlinespoo.core.exception.DuplicateException;
import com.airlinespoo.core.exception.NotFoundException;
import com.airlinespoo.core.util.Validator;
import com.airlinespoo.locations.model.Airport;
import com.airlinespoo.locations.model.City;
import com.airlinespoo.locations.repository.IAirportRepository;
import com.airlinespoo.locations.repository.ICityRepository;

import java.util.List;
import java.util.UUID;

public class AirportService implements IAirportService {

    private final IAirportRepository airportRepository;
    private final ICityRepository cityRepository;

    public AirportService(IAirportRepository airportRepository, ICityRepository cityRepository) {
        this.airportRepository = Validator.requireNotNull(airportRepository, "repositorio de aeropuertos");
        this.cityRepository = Validator.requireNotNull(cityRepository, "repositorio de ciudades");
    }

    @Override
    public Airport createAirport(String name, int cityId) {
        String validName = Validator.requireText(name, "nombre del aeropuerto", 50);
        City city = getCity(cityId);
        checkUniqueName(validName, cityId, null);

        UUID id = UUID.randomUUID();
        Airport airport = new Airport(id, validName, city);
        return airportRepository.create(airport);
    }

    @Override
    public Airport getAirport(UUID id) {
        return airportRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("aeropuerto", id));
    }

    @Override
    public List<Airport> listAirports() {
        return airportRepository.findAll();
    }

    @Override
    public Airport updateAirport(UUID id, String name, int cityId) {
        Airport existingAirport = getAirport(id);
        String validName = Validator.requireText(name, "nombre del aeropuerto", 50);
        City city = getCity(cityId);
        checkUniqueName(validName, cityId, existingAirport.getId());

        Airport updatedAirport = new Airport(existingAirport.getId(), validName, city);
        return airportRepository.update(updatedAirport);
    }

    @Override
    public void deleteAirport(UUID id) {
        getAirport(id);
        if (!airportRepository.deleteById(id)) {
            throw new NotFoundException("aeropuerto", id);
        }
    }

    @Override
    public List<Airport> listAirportsByCity(int cityId) {
        getCity(cityId);
        return airportRepository.listAirportsByCity(cityId);
    }

    @Override
    public List<Airport> searchAirportsByName(String name) {
        String search = Validator.requireText(name, "nombre de búsqueda");
        return airportRepository.searchAirportsByName(search);
    }

    @Override
    public long countAirportsByCity(int cityId) {
        getCity(cityId);
        return airportRepository.countAirportsByCity(cityId);
    }

    private City getCity(int id) {
        return cityRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("ciudad", id));
    }

    private void checkUniqueName(String name, int cityId, UUID currentId) {
        List<Airport> airports = airportRepository.listAirportsByCity(cityId);
        for (Airport airport : airports) {
            boolean sameAirport = airport.getId().equals(currentId);
            if (!sameAirport && airport.getName().equals(name)) {
                throw new DuplicateException("Ya existe un aeropuerto con ese nombre en la ciudad.");
            }
        }
    }
}
