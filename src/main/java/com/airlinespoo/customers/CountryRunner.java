package com.airlinespoo.customers;

import com.airlinespoo.core.ui.ConsoleInput;
import com.airlinespoo.customers.repository.CountryRepository;
import com.airlinespoo.customers.service.CountryService;
import com.airlinespoo.customers.ui.CountryMenu;

public class CountryRunner {
    public static void main(String[] args) {
        CountryService service = new CountryService(new CountryRepository());
        new CountryMenu(service, new ConsoleInput()).run();
    }
}
