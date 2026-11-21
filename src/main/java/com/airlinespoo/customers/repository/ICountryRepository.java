package com.airlinespoo.customers.repository;

import com.airlinespoo.core.repository.Repository;
import com.airlinespoo.customers.model.Country;

import java.util.List;
import java.util.Optional;

public interface ICountryRepository extends Repository<Country, Short> {

    Optional<Country> findByName(String name);

    boolean existsByName(String name);

    List<Country> findByNameContaining(String text);
}
