package com.airlinespoo.locations.model;

import com.airlinespoo.core.model.Identifiable;
import com.airlinespoo.core.util.Validator;

import java.util.UUID;

/**
 * Aeropuerto con identificador inmutable, nombre y ciudad obligatorios.
 */
public class Airport implements Identifiable<UUID> {

    private final UUID id;
    private String name;
    private City city;

    public Airport(UUID id, String name, City city) {
        this.id = Validator.requireNotNull(id, "id del aeropuerto");
        setName(name);
        setCity(city);
    }

    @Override
    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = Validator.requireText(name, "nombre del aeropuerto", 50);
    }

    public City getCity() {
        return city;
    }

    public void setCity(City city) {
        this.city = Validator.requireNotNull(city, "ciudad del aeropuerto");
    }
}
