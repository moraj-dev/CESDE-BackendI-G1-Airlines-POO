package com.airlinespoo.locations.repository;

import com.airlinespoo.core.repository.InMemoryRepository;
import com.airlinespoo.locations.model.City;
import java.util.List;

/** Ciudades en memoria para la demostración. */
public class InMemoryCityRepository extends InMemoryRepository<City, Integer> implements ICityRepository {
    @Override protected String entityName() { return "ciudad"; }
    @Override public List<City> searchCitiesByName(String text) {
        return findAll().stream().filter(c -> c.getName().toLowerCase().contains(text.toLowerCase())).toList();
    }
}
