package com.airlinespoo.airlines.ui;

import com.airlinespoo.airlines.model.Airline;
import com.airlinespoo.airlines.service.AirlineService;
import com.airlinespoo.core.exception.AirlinesException;
import com.airlinespoo.core.ui.ConsoleInput;
import com.airlinespoo.core.ui.Menu;
import java.util.List;

/** Menú de aerolíneas. Usa el mismo lector del resto del programa. */
public class AirlineMenu implements Menu {

    private final AirlineService airlineService;
    private final ConsoleInput input;

    public AirlineMenu(AirlineService airlineService, ConsoleInput input) {
        this.airlineService = airlineService;
        this.input = input;
    }

    @Override
    public String title() {
        return "Aerolíneas";
    }

    @Override
    public void run() {
        int option;
        do {
            System.out.println("\n--- MENÚ DE AEROLÍNEAS ---");
            System.out.println("1. Registrar aerolínea");
            System.out.println("2. Buscar por ID");
            System.out.println("3. Listar aerolíneas");
            System.out.println("4. Renombrar aerolínea");
            System.out.println("5. Eliminar aerolínea");
            System.out.println("6. Buscar por nombre");
            System.out.println("0. Volver al menú principal");
            option = input.readInt("Seleccione una opción", 0, 6);

            try {
                switch (option) {
                    case 1 -> registerAirline();
                    case 2 -> findAirlineById();
                    case 3 -> listAllAirlines();
                    case 4 -> renameAirline();
                    case 5 -> deleteAirline();
                    case 6 -> searchAirlines();
                    case 0 -> System.out.println("Volviendo al menú principal...");
                }
            } catch (AirlinesException | IllegalArgumentException e) {
                System.out.println("No se pudo completar: " + e.getMessage());
            }
        } while (option != 0);
    }

    private void registerAirline() {
        String name = input.readRequiredText("Nombre de la aerolínea");
        Airline created = airlineService.createAirline(name);
        System.out.println("Aerolínea registrada: " + created);
    }

    private void findAirlineById() {
        int id = input.readInt("ID de la aerolínea", 1, Integer.MAX_VALUE);
        System.out.println(airlineService.getAirline(id));
    }

    private void listAllAirlines() {
        List<Airline> airlines = airlineService.listAirlines();
        if (airlines.isEmpty()) {
            System.out.println("No hay aerolíneas registradas.");
        } else {
            for (Airline airline : airlines) {
                System.out.println(airline.getId() + " - " + airline.getName());
            }
        }
        System.out.println("Total: " + airlineService.countAirlines());
    }

    private void renameAirline() {
        int id = input.readInt("ID que desea renombrar", 1, Integer.MAX_VALUE);
        String name = input.readRequiredText("Nuevo nombre");
        System.out.println("Aerolínea actualizada: " + airlineService.renameAirline(id, name));
    }

    private void deleteAirline() {
        int id = input.readInt("ID que desea eliminar", 1, Integer.MAX_VALUE);
        airlineService.deleteAirline(id);
        System.out.println("Aerolínea eliminada.");
    }

    private void searchAirlines() {
        String text = input.readRequiredText("Nombre o parte del nombre");
        List<Airline> found = airlineService.searchAirlinesByName(text);
        if (found.isEmpty()) {
            System.out.println("No se encontraron aerolíneas.");
        } else {
            for (Airline airline : found) {
                System.out.println(airline.getId() + " - " + airline.getName());
            }
        }
    }
}
