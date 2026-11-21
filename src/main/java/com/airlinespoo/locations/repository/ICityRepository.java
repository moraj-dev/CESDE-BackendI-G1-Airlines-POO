package com.airlinespoo.locations.repository;

import com.airlinespoo.core.repository.Repository;
import com.airlinespoo.locations.model.City;

import java.util.List;

public interface ICityRepository extends Repository<City, Integer> {

    List<City> searchCitiesByName(String name);
}
