package com.airlinespoo.airlines.ui;

import com.airlinespoo.core.ui.ConsoleInput;
import com.airlinespoo.core.ui.Menu;
import java.util.List;

/** Menú general. Cada módulo maneja sus propias opciones. */
public class MainMenu {

    private final ConsoleInput input;
    private final List<Menu> menus;

    public MainMenu(ConsoleInput input, List<Menu> menus) {
        this.input = input;
        this.menus = List.copyOf(menus);
    }

    public void run() {
        while (true) {
            System.out.println("\n=== AIRLINES POO - POSTGRESQL ===");
            for (int i = 0; i < menus.size(); i++) {
                System.out.println((i + 1) + ". " + menus.get(i).title());
            }
            System.out.println("0. Salir");

            int option = input.readInt("Selecciona una opción", 0, menus.size());
            if (option == 0) {
                System.out.println("Hasta luego.");
                return;
            }

            try {
                menus.get(option - 1).run();
            } catch (RuntimeException e) {
                System.out.println("No se pudo completar: " + e.getMessage());
            }
        }
    }
}
