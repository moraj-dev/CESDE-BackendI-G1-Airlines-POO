package com.airlinespoo.customers.model;

import com.airlinespoo.core.model.Identifiable;
import com.airlinespoo.core.util.Validator;
import java.util.Objects;

/** Datos básicos del cliente necesarios para reservas. */
public class Customer implements Identifiable<String> {
    private final String id;
    private String firstName;
    private String lastName;

    public Customer(String id, String firstName, String lastName) {
        this.id = Validator.requireText(id, "documento");
        this.firstName = Validator.requireText(firstName, "nombre");
        this.lastName = Validator.requireText(lastName, "apellido");
    }
    @Override public String getId() { return id; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    @Override public boolean equals(Object other) { return other instanceof Customer c && id.equals(c.id); }
    @Override public int hashCode() { return Objects.hash(id); }
    @Override public String toString() { return firstName + " " + lastName + " (" + id + ")"; }
}
