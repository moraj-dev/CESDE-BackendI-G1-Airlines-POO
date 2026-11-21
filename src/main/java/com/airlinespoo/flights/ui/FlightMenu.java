package com.airlinespoo.flights.ui;

import com.airlinespoo.airlines.model.Airline;
import com.airlinespoo.airlines.service.AirlineService;
import com.airlinespoo.core.ui.ConsoleInput;
import com.airlinespoo.core.ui.Menu;
import com.airlinespoo.core.exception.AirlinesException;
import com.airlinespoo.core.exception.BusinessRuleException;
import com.airlinespoo.flights.model.Flight;
import com.airlinespoo.flights.model.FlightReportRow;
import com.airlinespoo.flights.service.FlightService;
import com.airlinespoo.locations.model.Airport;
import com.airlinespoo.locations.service.LocationService;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;


public class FlightMenu implements Menu {

    private static final String TITLE = "Vuelos";
    private static final String DATE_HINT = " (formato aaaa-mm-dd)";
    private static final String TIME_HINT = " (formato HH:mm, 24 horas)";
    private static final int EXIT_OPTION = 0;
    private static final int MAX_OPTION = 8;
    private static final int MAX_ID = Integer.MAX_VALUE;
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final FlightService flightService;
    private final LocationService locationService;
    private final AirlineService airlineService;
    private final ConsoleInput input;

    public FlightMenu(FlightService flightService, LocationService locationService,
                      AirlineService airlineService, ConsoleInput input) {
        this.flightService = flightService;
        this.locationService = locationService;
        this.airlineService = airlineService;
        this.input = input;
    }

    @Override
    public String title() {
        return TITLE;
    }

    @Override
    public void run() {
        boolean back = false;
        while (!back) {
            printOptions();
            int option = input.readInt("Elige una opción", EXIT_OPTION, MAX_OPTION);
            try {
                back = execute(option);
            } catch (AirlinesException e) {
                System.out.println("No se pudo completar la operación: " + e.getMessage());
            }
        }
    }

    private void printOptions() {
        System.out.println();
        System.out.println("=== " + TITLE + " ===");
        System.out.println("1. Programar vuelo");
        System.out.println("2. Consultar por código");
        System.out.println("3. Listar vuelos");
        System.out.println("4. Reprogramar / actualizar vuelo");
        System.out.println("5. Cancelar vuelo");
        System.out.println("6. Listar por aerolínea");
        System.out.println("7. Listar por rango de fechas");
        System.out.println("8. Reporte mensual por aerolínea");
        System.out.println("0. Volver");
    }

    /** @return true si el usuario eligió volver */
    private boolean execute(int option) {
        switch (option) {
            case 1 -> scheduleFlight();
            case 2 -> findByName();
            case 3 -> printFlights(flightService.listFlights());
            case 4 -> updateFlight();
            case 5 -> cancelFlight();
            case 6 -> listByAirline();
            case 7 -> listByDateRange();
            case 8 -> monthlyReport();
            default -> {
                return true;
            }
        }
        return false;
    }


    private void scheduleFlight() {
        FlightData data = readFlightData();
        Flight flight = flightService.createFlight(data.name, data.departure, data.arrival,
                data.departureDate, data.departureTime, data.arrivalDate, data.arrivalTime, data.airline);
        System.out.println("Vuelo programado:");
        printFlight(flight);
    }

    private void findByName() {
        String name = input.readRequiredText("Código del vuelo");
        printFlight(flightService.getFlightByName(name));
    }

    private void updateFlight() {
        int id = input.readInt("Id del vuelo a actualizar", 1, MAX_ID);
        Flight current = flightService.getFlight(id);
        System.out.println("Vuelo actual:");
        printFlight(current);
        System.out.println("Ingresa los nuevos datos:");
        FlightData data = readFlightData();
        Flight updated = flightService.updateFlight(id, data.name, data.departure, data.arrival,
                data.departureDate, data.departureTime, data.arrivalDate, data.arrivalTime, data.airline);
        System.out.println("Vuelo actualizado:");
        printFlight(updated);
    }

    private void cancelFlight() {
        int id = input.readInt("Id del vuelo a cancelar", 1, MAX_ID);
        Flight flight = flightService.getFlight(id);
        printFlight(flight);
        if (input.confirm("¿Seguro que deseas cancelar este vuelo?")) {
            flightService.deleteFlight(id);
            System.out.println("Vuelo cancelado.");
        } else {
            System.out.println("Operación cancelada.");
        }
    }

    private void listByAirline() {
        Airline airline = chooseAirline();
        printFlights(flightService.listFlightsByAirline(airline.getId()));
    }

    private void listByDateRange() {
        LocalDate start = input.readDate("Fecha de inicio" + DATE_HINT);
        LocalDate end = input.readDate("Fecha de fin" + DATE_HINT);
        printFlights(flightService.listFlightsBetween(start, end));
    }

    private void monthlyReport() {
        int year = input.readInt("Año", 2000, 2100);
        int month = input.readInt("Mes (1-12)", 1, 12);
        Map<Airline, List<FlightReportRow>> report = flightService.listFlightsByAirlineInMonth(year, month);
        if (report.isEmpty()) {
            System.out.println("Sin vuelos en ese periodo.");
            return;
        }
        for (Map.Entry<Airline, List<FlightReportRow>> group : report.entrySet()) {
            System.out.println();
            System.out.println("Aerolínea: " + group.getKey().getName());
            for (FlightReportRow row : group.getValue()) {
                System.out.printf("  %-8s %s (%s) %s -> %s (%s) %s%n",
                        row.flightName(),
                        row.departureAirport(), row.departureCity(), row.departure().format(DATE_TIME_FORMAT),
                        row.arrivalAirport(), row.arrivalCity(), row.arrival().format(DATE_TIME_FORMAT));
            }
        }
    }

    // ----------------------------------------------------------- lectura

    /** Datos que pide el menú; el servicio decide si son válidos. */
    private static final class FlightData {
        String name;
        Airport departure;
        Airport arrival;
        LocalDate departureDate;
        LocalTime departureTime;
        LocalDate arrivalDate;
        LocalTime arrivalTime;
        Airline airline;
    }

    private FlightData readFlightData() {
        FlightData data = new FlightData();
        data.name = input.readRequiredText("Código del vuelo (máx. " + Flight.MAX_FLIGHT_NAME_LENGTH + " caracteres)");
        data.airline = chooseAirline();
        System.out.println("Aeropuerto de salida:");
        data.departure = chooseAirport();
        System.out.println("Aeropuerto de llegada:");
        data.arrival = chooseAirport();
        data.departureDate = input.readDate("Fecha de salida" + DATE_HINT);
        data.departureTime = input.readTime("Hora de salida" + TIME_HINT);
        data.arrivalDate = input.readDate("Fecha de llegada" + DATE_HINT);
        data.arrivalTime = input.readTime("Hora de llegada" + TIME_HINT);
        return data;
    }

    private Airline chooseAirline() {
        List<Airline> airlines = airlineService.listAirlines();
        if (airlines.isEmpty()) {
            throw new BusinessRuleException("No hay aerolíneas registradas.");
        }
        System.out.println("Aerolíneas:");
        for (int i = 0; i < airlines.size(); i++) {
            System.out.println((i + 1) + ". " + airlines.get(i).getName());
        }
        return airlines.get(input.readInt("Elige el número de la aerolínea", 1, airlines.size()) - 1);
    }

    private Airport chooseAirport() {
        List<Airport> airports = locationService.listAirports();
        if (airports.isEmpty()) {
            throw new BusinessRuleException("No hay aeropuertos registrados.");
        }
        for (int i = 0; i < airports.size(); i++) {
            Airport airport = airports.get(i);
            System.out.println((i + 1) + ". " + airport.getName() + " (" + airport.getCity().getName() + ")");
        }
        return airports.get(input.readInt("Elige el número del aeropuerto", 1, airports.size()) - 1);
    }

    // -------------------------------------------------------- presentación

    private void printFlights(List<Flight> flights) {
        if (flights.isEmpty()) {
            System.out.println("No hay vuelos para mostrar.");
            return;
        }
        for (Flight flight : flights) {
            printFlight(flight);
        }
    }

    private void printFlight(Flight flight) {
        System.out.printf("[%d] %-8s %s | %s (%s) -> %s (%s) | salida %s | llegada %s | duración %s%n",
                flight.getId(), flight.getFlightName(), flight.getAirline().getName(),
                flight.getDeparture().getName(), flight.getDeparture().getCity().getName(),
                flight.getArrival().getName(), flight.getArrival().getCity().getName(),
                flight.getDepartureDateTime().format(DATE_TIME_FORMAT),
                flight.getArrivalDateTime().format(DATE_TIME_FORMAT),
                formatDuration(flight.getDuration()));
    }

    /** Muestra la duración en horas y minutos, no en segundos. */
    private static String formatDuration(Duration duration) {
        return duration.toHours() + " h " + duration.toMinutesPart() + " min";
    }
}
