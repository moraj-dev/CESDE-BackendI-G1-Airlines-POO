package com.airlinespoo.customers.service;

import com.airlinespoo.core.exception.DuplicateException;
import com.airlinespoo.core.exception.NotFoundException;
import com.airlinespoo.core.util.Validator;
import com.airlinespoo.customers.model.Country;
import com.airlinespoo.customers.repository.ICountryRepository;

import java.util.List;

public class CountryService {

    private final ICountryRepository countryRepository;

    public CountryService(ICountryRepository countryRepository) {
        this.countryRepository = Validator.requireNotNull(countryRepository, "repositorio de países");
    }

    public Country createCountry(short id, String name) {
        if (countryRepository.existsById(id)) {
            throw new DuplicateException("país", id);
        }
        String cleanName = Validator.requireText(name, "nombre del país", Country.MAX_NAME);
        if (countryRepository.existsByName(cleanName)) {
            throw new DuplicateException("Ya existe un país llamado '" + cleanName + "'.");
        }
        return countryRepository.create(new Country(id, cleanName));
    }

    public Country getCountry(short id) {
        return countryRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("país", id));
    }

    public List<Country> listCountries() {
        return countryRepository.findAll();
    }

    public List<Country> searchByName(String text) {
        return countryRepository.findByNameContaining(text);
    }

    public Country updateCountry(short id, String newName) {
        Country existing = getCountry(id);
        String cleanName = Validator.requireText(newName, "nombre del país", Country.MAX_NAME);

        countryRepository.findByName(cleanName).ifPresent(other -> {
            if (!other.getId().equals(id)) {
                throw new DuplicateException("Ya existe otro país llamado '" + cleanName + "'.");
            }
        });

        existing.setName(cleanName);
        return countryRepository.update(existing);
    }

    public void deleteCountry(short id) {
        if (!countryRepository.deleteById(id)) {
            throw new NotFoundException("país", id);
        }
    }
}
