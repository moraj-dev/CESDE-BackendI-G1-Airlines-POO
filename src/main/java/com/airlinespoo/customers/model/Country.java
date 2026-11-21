package com.airlinespoo.customers.model;

import com.airlinespoo.core.model.Identifiable;
import com.airlinespoo.core.util.Validator;

import java.util.Objects;

public class Country implements Identifiable<Short> {

    public static final int MAX_NAME = 20;

    private final Short id;
    private String name;

    public Country(Short id, String name) {
        Validator.requireNotNull(id, "id del país");
        Validator.requirePositive(id, "id del país");
        this.id = id;
        setName(name);
    }

    @Override
    public Short getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = Validator.requireText(name, "nombre del país", MAX_NAME);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Country other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "[%d] %s".formatted(id, name);
    }
}
