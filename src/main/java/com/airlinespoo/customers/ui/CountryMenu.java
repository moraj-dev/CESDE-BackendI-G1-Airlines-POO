package com.airlinespoo.customers.ui;

import com.airlinespoo.core.exception.AirlinesException;
import com.airlinespoo.core.ui.ConsoleInput;
import com.airlinespoo.core.ui.Menu;
import com.airlinespoo.core.util.Validator;
import com.airlinespoo.customers.model.Country;
import com.airlinespoo.customers.service.CountryService;

import java.util.List;

public class CountryMenu implements Menu {

    private final CountryService countryService;
    private final ConsoleInput input;

    public CountryMenu(CountryService countryService, ConsoleInput input) {
        this.countryService = Validator.requireNotNull(countryService, "servicio de países");
        this.input = Validator.requireNotNull(input, "lector de consola");
    }

    @Override
    public String title() {
        return "Gestión de Países";
    }

    @Override
    public void run() {
        boolean exit = false;
        while (!exit) {
            System.out.println("\n=== GESTIÓN DE PAÍSES ===");
            System.out.println("1. Registrar país");
            System.out.println("2. Listar países");
            System.out.println("3. Buscar país por ID");
            System.out.println("4. Buscar país por nombre");
            System.out.println("5. Actualizar país");
            System.out.println("6. Eliminar país");
            System.out.println("0. Volver");

            int option = input.readInt("Seleccione una opción", 0, 6);
            try {
                switch (option) {
                    case 1 -> {
                        short id = (short) input.readInt("ID del país (ej. 57)", 1, Short.MAX_VALUE);
                        String name = input.readRequiredText("Nombre del país");
                        System.out.println("Registrado: " + countryService.createCountry(id, name));
                    }
                    case 2 -> {
                        List<Country> list = countryService.listCountries();
                        if (list.isEmpty()) System.out.println("No hay países registrados.");
                        else list.forEach(System.out::println);
                    }
                    case 3 -> {
                        short id = (short) input.readInt("ID del país", 1, Short.MAX_VALUE);
                        System.out.println(countryService.getCountry(id));
                    }
                    case 4 -> {
                        List<Country> matches = countryService.searchByName(input.readRequiredText("Texto a buscar"));
                        if (matches.isEmpty()) System.out.println("Sin coincidencias.");
                        else matches.forEach(System.out::println);
                    }
                    case 5 -> {
                        short id = (short) input.readInt("ID del país a modificar", 1, Short.MAX_VALUE);
                        String newName = input.readRequiredText("Nuevo nombre");
                        System.out.println("Actualizado: " + countryService.updateCountry(id, newName));
                    }
                    case 6 -> {
                        short id = (short) input.readInt("ID del país a eliminar", 1, Short.MAX_VALUE);
                        if (input.confirm("¿Eliminar el país #" + id + "?")) {
                            countryService.deleteCountry(id);
                            System.out.println("País eliminado.");
                        }
                    }
                    case 0 -> exit = true;
                }
            } catch (AirlinesException e) {
                System.out.println("ERROR: " + e.getMessage());
            }
            if (!exit) input.pause();
        }
    }
}
