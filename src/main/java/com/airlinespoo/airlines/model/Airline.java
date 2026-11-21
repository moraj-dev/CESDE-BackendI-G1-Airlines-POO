package com.airlinespoo.airlines.model;

import com.airlinespoo.core.model.Identifiable;
import com.airlinespoo.core.util.Validator;
import java.util.Objects;

public class Airline implements Identifiable<Integer> {

    private final Integer id;
    private String name;

    public Airline(Integer id, String name) {
        this.id = Validator.requireNotNull(id, "id de airline");
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
        this.name = Validator.requireText(name, "nombre de airline");
        Validator.requireMaxLength(this.name, 50, "nombre de airline");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Airline airline = (Airline) o;
        return Objects.equals(id, airline.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Airline{id=" + id + ", name='" + name + "'}";
    }
}
