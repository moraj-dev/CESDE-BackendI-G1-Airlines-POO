package com.airlinespoo.locations.model;

import com.airlinespoo.core.model.Identifiable;
import com.airlinespoo.core.util.Validator;

/**
 * Ciudad identificada por un número inmutable y un nombre obligatorio.
 */
public class City implements Identifiable<Integer> {

    private final int id;
    private String name;

    public City(int id, String name) {
        this.id = (int) Validator.requirePositive(id, "id de la ciudad");
        setName(name);
    }

    @Override
    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = Validator.requireText(name, "nombre de la ciudad", 20);
    }
}
