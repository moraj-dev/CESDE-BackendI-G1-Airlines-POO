package com.airlinespoo.airlines.repository;

import com.airlinespoo.airlines.model.Airline;
import com.airlinespoo.customers.repository.DatabaseConnection;
import com.airlinespoo.core.exception.AirlinesException;
import java.sql.*;
import java.util.*;

/** Usa la misma interfaz del repositorio anterior, pero guarda en PostgreSQL. */
public class JdbcAirlineRepository extends AirlineRepository {
    @Override public Airline create(Airline a) {
        try (Connection c=DatabaseConnection.getConnection(); PreparedStatement ps=c.prepareStatement("INSERT INTO tblAirlines(AirlineId,Name) VALUES (?,?)")) {
            ps.setInt(1,a.getId()); ps.setString(2,a.getName());ps.executeUpdate();return a;
        } catch(SQLException e){throw error(e);}
    }
    @Override public Optional<Airline> findById(Integer id){
        try(Connection c=DatabaseConnection.getConnection();PreparedStatement ps=c.prepareStatement("SELECT AirlineId,Name FROM tblAirlines WHERE AirlineId=?")){
            ps.setInt(1,id);try(ResultSet rs=ps.executeQuery()){return rs.next()?Optional.of(read(rs)):Optional.empty();}
        }catch(SQLException e){throw error(e);}
    }
    @Override public List<Airline> findAll(){
        try(Connection c=DatabaseConnection.getConnection();PreparedStatement ps=c.prepareStatement("SELECT AirlineId,Name FROM tblAirlines ORDER BY AirlineId");ResultSet rs=ps.executeQuery()){
            List<Airline> result=new ArrayList<>();while(rs.next())result.add(read(rs));return result;
        }catch(SQLException e){throw error(e);}
    }
    @Override public Airline update(Airline a){
        try(Connection c=DatabaseConnection.getConnection();PreparedStatement ps=c.prepareStatement("UPDATE tblAirlines SET Name=? WHERE AirlineId=?")){
            ps.setString(1,a.getName());ps.setInt(2,a.getId());if(ps.executeUpdate()==0)throw new IllegalArgumentException("Aerolínea no encontrada: "+a.getId());return a;
        }catch(SQLException e){throw error(e);}
    }
    @Override public boolean deleteById(Integer id){
        try(Connection c=DatabaseConnection.getConnection();PreparedStatement ps=c.prepareStatement("DELETE FROM tblAirlines WHERE AirlineId=?")){
            ps.setInt(1,id);return ps.executeUpdate()>0;
        }catch(SQLException e){throw error(e);}
    }
    @Override public boolean existsById(Integer id){return findById(id).isPresent();}
    @Override public long count(){return findAll().size();}
    @Override public Optional<Airline> findByName(String name){return findAll().stream().filter(a->a.getName().equalsIgnoreCase(name.trim())).findFirst();}
    @Override public List<Airline> searchByName(String name){return findAll().stream().filter(a->a.getName().toLowerCase().contains(name.trim().toLowerCase())).toList();}
    private static Airline read(ResultSet r)throws SQLException{return new Airline(r.getInt("AirlineId"),r.getString("Name"));}
    private static AirlinesException error(SQLException e){return new AirlinesException("Error de PostgreSQL en aerolíneas: "+e.getMessage(),e);}
}
