package com.airlinespoo.customers.service;

import com.airlinespoo.core.exception.NotFoundException;
import com.airlinespoo.customers.model.Customer;
import com.airlinespoo.customers.repository.JdbcCustomerRepository;
import java.util.List;

/** Servicio de clientes. Ahora los datos se recuperan desde PostgreSQL. */
public class CustomerService {
    private final JdbcCustomerRepository repository = new JdbcCustomerRepository();
    public Customer createCustomer(String document,String firstName,String lastName){
        return repository.create(new Customer(document,firstName,lastName));
    }
    public Customer getCustomer(String document){
        return repository.findById(document).orElseThrow(()->new NotFoundException("cliente",document));
    }
    public List<Customer> listCustomers(){return repository.findAll();}
}
