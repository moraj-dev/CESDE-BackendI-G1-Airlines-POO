package com.airlinespoo.airlines.repository;

import com.airlinespoo.airlines.model.Airline;
import com.airlinespoo.core.repository.InMemoryRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AirlineRepository extends InMemoryRepository<Airline, Integer> implements IAirlineRepository {

    @Override
    protected String entityName() {
        return "aerolínea";
    }

    /**
     * Busca una aerolínea por su nombre exacto (ignorando mayúsculas/minúsculas).
     */
    public Optional<Airline> findByName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        String target = name.trim().toLowerCase();
        return findAll().stream()
            .filter(airline -> airline.getName() != null && airline.getName().toLowerCase().equals(target))
            .findFirst();
    }

    /**
     * Busca aerolíneas que contengan el texto ingresado (búsqueda parcial).
     */
    public List<Airline> searchByName(String partialName) {
        if (partialName == null) {
            return List.of();
        }
        String target = partialName.trim().toLowerCase();
        return findAll().stream()
            .filter(airline -> airline.getName() != null && airline.getName().toLowerCase().contains(target))
            .collect(Collectors.toList());
    }
}
