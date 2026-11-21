package com.airlinespoo.customers.repository;

import com.airlinespoo.core.repository.InMemoryRepository;
import com.airlinespoo.customers.model.Country;
import java.util.List;
import java.util.Optional;

/** Países guardados en memoria para ejecutar sin PostgreSQL. */
public class InMemoryCountryRepository extends InMemoryRepository<Country, Short> implements ICountryRepository {
    @Override protected String entityName() { return "país"; }
    @Override public Optional<Country> findByName(String name) {
        return findAll().stream().filter(c -> c.getName().equalsIgnoreCase(name)).findFirst();
    }
    @Override public boolean existsByName(String name) { return findByName(name).isPresent(); }
    @Override public List<Country> findByNameContaining(String text) {
        return findAll().stream().filter(c -> c.getName().toLowerCase().contains(text.toLowerCase())).toList();
    }
}
