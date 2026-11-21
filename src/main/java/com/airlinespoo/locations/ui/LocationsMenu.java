package com.airlinespoo.locations.ui;

import com.airlinespoo.core.exception.AirlinesException;
import com.airlinespoo.core.ui.ConsoleInput;
import com.airlinespoo.core.ui.Menu;
import com.airlinespoo.core.util.Validator;
import com.airlinespoo.locations.model.Airport;
import com.airlinespoo.locations.model.City;
import com.airlinespoo.locations.service.IAirportService;
import com.airlinespoo.locations.service.ICityService;

import java.util.List;
import java.util.UUID;

public class LocationsMenu implements Menu {

    private final ConsoleInput input;
    private final ICityService cityService;
    private final IAirportService airportService;

    public LocationsMenu(ConsoleInput input, ICityService cityService, IAirportService airportService) {
        this.input = Validator.requireNotNull(input, "lector de consola");
        this.cityService = Validator.requireNotNull(cityService, "servicio de ciudades");
        this.airportService = Validator.requireNotNull(airportService, "servicio de aeropuertos");
    }

    @Override
    public String title() {
        return "Ciudades y aeropuertos";
    }

    @Override
    public void run() {
        int option;
        do {
            System.out.println("\n--- Ciudades y aeropuertos ---");
            System.out.println("1. Gestionar ciudades");
            System.out.println("2. Gestionar aeropuertos");
            System.out.println("0. Volver");
            option = input.readInt("Selecciona una opción", 0, 2);
            switch (option) {
                case 1:
                    cityMenu();
                    break;
                case 2:
                    airportMenu();
                    break;
                case 0:
                    break;
            }
        } while (option != 0);
    }

    private void cityMenu() {
        int option;
        do {
            System.out.println("\n--- Ciudades ---");
            System.out.println("1. Registrar ciudad");
            System.out.println("2. Consultar ciudad por ID");
            System.out.println("3. Listar ciudades");
            System.out.println("4. Actualizar ciudad");
            System.out.println("5. Eliminar ciudad");
            System.out.println("6. Buscar ciudades por nombre");
            System.out.println("0. Volver");
            option = input.readInt("Selecciona una opción", 0, 6);
            try {
                switch (option) {
                    case 1:
                        createCity();
                        break;
                    case 2:
                        getCity();
                        break;
                    case 3:
                        List<City> cities = cityService.listCities();
                        showCities(cities);
                        break;
                    case 4:
                        updateCity();
                        break;
                    case 5:
                        deleteCity();
                        break;
                    case 6:
                        searchCities();
                        break;
                    case 0:
                        break;
                }
            } catch (AirlinesException e) {
                System.out.println("Error: " + e.getMessage());
            }
        } while (option != 0);
    }

    private void airportMenu() {
        int option;
        do {
            System.out.println("\n--- Aeropuertos ---");
            System.out.println("1. Registrar aeropuerto");
            System.out.println("2. Consultar aeropuerto por ID");
            System.out.println("3. Listar aeropuertos");
            System.out.println("4. Actualizar aeropuerto");
            System.out.println("5. Eliminar aeropuerto");
            System.out.println("6. Listar aeropuertos por ciudad");
            System.out.println("7. Buscar aeropuertos por nombre");
            System.out.println("8. Contar aeropuertos por ciudad");
            System.out.println("0. Volver");
            option = input.readInt("Selecciona una opción", 0, 8);
            try {
                switch (option) {
                    case 1:
                        createAirport();
                        break;
                    case 2:
                        getAirport();
                        break;
                    case 3:
                        List<Airport> airports = airportService.listAirports();
                        showAirports(airports);
                        break;
                    case 4:
                        updateAirport();
                        break;
                    case 5:
                        deleteAirport();
                        break;
                    case 6:
                        listAirportsByCity();
                        break;
                    case 7:
                        searchAirports();
                        break;
                    case 8:
                        countAirportsByCity();
                        break;
                    case 0:
                        break;
                }
            } catch (AirlinesException e) {
                System.out.println("Error: " + e.getMessage());
            }
        } while (option != 0);
    }

    private void createCity() {
        String name = input.readRequiredText("Nombre");
        City city = cityService.createCity(name);
        showCity(city);
    }

    private void getCity() {
        int id = readCityId();
        City city = cityService.getCity(id);
        showCity(city);
    }

    private void updateCity() {
        int id = readCityId();
        String name = input.readRequiredText("Nuevo nombre");
        City city = cityService.updateCity(id, name);
        showCity(city);
    }

    private void deleteCity() {
        int id = readCityId();
        cityService.deleteCity(id);
        System.out.println("Ciudad eliminada.");
    }

    private void searchCities() {
        String name = input.readRequiredText("Nombre a buscar");
        List<City> cities = cityService.searchCitiesByName(name);
        showCities(cities);
    }

    private void createAirport() {
        String name = input.readRequiredText("Nombre");
        int cityId = readCityId();
        Airport airport = airportService.createAirport(name, cityId);
        showAirport(airport);
    }

    private void getAirport() {
        UUID id = readAirportId();
        Airport airport = airportService.getAirport(id);
        showAirport(airport);
    }

    private void updateAirport() {
        UUID id = readAirportId();
        String name = input.readRequiredText("Nuevo nombre");
        int cityId = readCityId();
        Airport airport = airportService.updateAirport(id, name, cityId);
        showAirport(airport);
    }

    private void deleteAirport() {
        UUID id = readAirportId();
        airportService.deleteAirport(id);
        System.out.println("Aeropuerto eliminado.");
    }

    private void listAirportsByCity() {
        int cityId = readCityId();
        List<Airport> airports = airportService.listAirportsByCity(cityId);
        showAirports(airports);
    }

    private void searchAirports() {
        String name = input.readRequiredText("Nombre a buscar");
        List<Airport> airports = airportService.searchAirportsByName(name);
        showAirports(airports);
    }

    private void countAirportsByCity() {
        int cityId = readCityId();
        long count = airportService.countAirportsByCity(cityId);
        System.out.println("Cantidad de aeropuertos: " + count);
    }

    private int readCityId() {
        return input.readInt("ID de la ciudad", 1, Integer.MAX_VALUE);
    }

    private UUID readAirportId() {
        while (true) {
            String value = input.readRequiredText("ID del aeropuerto (UUID)");
            try {
                UUID id = UUID.fromString(value);
                if (id.toString().equalsIgnoreCase(value)) {
                    return id;
                }
            } catch (IllegalArgumentException e) {
                // Se solicita de nuevo el identificador.
            }
            System.out.println("Escribe un UUID válido.");
        }
    }

    private void showCity(City city) {
        System.out.println("ID: " + city.getId() + " | Ciudad: " + city.getName());
    }

    private void showCities(List<City> cities) {
        if (cities.isEmpty()) {
            System.out.println("No se encontraron ciudades.");
        } else {
            for (City city : cities) {
                showCity(city);
            }
        }
    }

    private void showAirport(Airport airport) {
        System.out.println("ID: " + airport.getId() + " | Aeropuerto: " + airport.getName()
                + " | Ciudad: " + airport.getCity().getName()
                + " (ID: " + airport.getCity().getId() + ")");
    }

    private void showAirports(List<Airport> airports) {
        if (airports.isEmpty()) {
            System.out.println("No se encontraron aeropuertos.");
        } else {
            for (Airport airport : airports) {
                showAirport(airport);
            }
        }
    }
}
