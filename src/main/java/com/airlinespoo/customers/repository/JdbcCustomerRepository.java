package com.airlinespoo.customers.repository;

import com.airlinespoo.customers.model.Customer;
import com.airlinespoo.core.exception.AirlinesException;
import com.airlinespoo.core.exception.DuplicateException;
import java.sql.*;
import java.util.*;

/** Consultas de clientes. Todas abren conexión desde DatabaseConnection. */
public class JdbcCustomerRepository {
    public Customer create(Customer customer){
        try(Connection c=DatabaseConnection.getConnection();PreparedStatement ps=c.prepareStatement("INSERT INTO tblCustomers(CustomerId,FirstName,LastName) VALUES(?,?,?)")){
            ps.setString(1,customer.getId());ps.setString(2,customer.getFirstName());ps.setString(3,customer.getLastName());ps.executeUpdate();return customer;
        }catch(SQLException e){if("23505".equals(e.getSQLState()))throw new DuplicateException("cliente",customer.getId());throw error(e);}
    }
    public Optional<Customer> findById(String id){
        try(Connection c=DatabaseConnection.getConnection();PreparedStatement ps=c.prepareStatement("SELECT CustomerId,FirstName,LastName FROM tblCustomers WHERE CustomerId=?")){
            ps.setString(1,id);try(ResultSet rs=ps.executeQuery()){return rs.next()?Optional.of(read(rs)):Optional.empty();}
        }catch(SQLException e){throw error(e);}
    }
    public List<Customer> findAll(){
        try(Connection c=DatabaseConnection.getConnection();PreparedStatement ps=c.prepareStatement("SELECT CustomerId,FirstName,LastName FROM tblCustomers ORDER BY CustomerId");ResultSet rs=ps.executeQuery()){
            List<Customer> list=new ArrayList<>();while(rs.next())list.add(read(rs));return list;
        }catch(SQLException e){throw error(e);}
    }
    private static Customer read(ResultSet rs)throws SQLException{return new Customer(rs.getString("CustomerId"),rs.getString("FirstName"),rs.getString("LastName"));}
    private static AirlinesException error(SQLException e){return new AirlinesException("Error de PostgreSQL en clientes: "+e.getMessage(),e);}
}
