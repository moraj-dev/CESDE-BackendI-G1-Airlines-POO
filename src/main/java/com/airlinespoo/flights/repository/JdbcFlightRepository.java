package com.airlinespoo.flights.repository;

import com.airlinespoo.airlines.repository.AirlineRepository;
import com.airlinespoo.customers.repository.DatabaseConnection;
import com.airlinespoo.core.exception.AirlinesException;
import com.airlinespoo.flights.model.Flight;
import com.airlinespoo.locations.repository.IAirportRepository;
import java.sql.*;
import java.util.*;

/** Los métodos existentes se mantienen; únicamente cambia de dónde salen los vuelos. */
public class JdbcFlightRepository extends FlightRepository {
    private final AirlineRepository airlines;
    private final IAirportRepository airports;
    private static final String COLUMNS="FlightId,FlightName,DepartureAirportId,ArrivalAirportId,DepartureDate,DepartureTime,ArrivalDate,ArrivalTime,AirlineId";
    public JdbcFlightRepository(AirlineRepository airlines,IAirportRepository airports){this.airlines=airlines;this.airports=airports;}
    @Override public Flight create(Flight f){
        try(Connection c=DatabaseConnection.getConnection();PreparedStatement ps=c.prepareStatement("INSERT INTO tblFlights("+COLUMNS+") VALUES(?,?,?,?,?,?,?,?,?)")){
            bind(ps,f);ps.executeUpdate();return f;
        }catch(SQLException e){throw error(e);}
    }
    @Override public Flight update(Flight f){
        String q="UPDATE tblFlights SET FlightName=?,DepartureAirportId=?,ArrivalAirportId=?,DepartureDate=?,DepartureTime=?,ArrivalDate=?,ArrivalTime=?,AirlineId=? WHERE FlightId=?";
        try(Connection c=DatabaseConnection.getConnection();PreparedStatement ps=c.prepareStatement(q)){
            ps.setString(1,f.getFlightName());ps.setObject(2,f.getDeparture().getId());ps.setObject(3,f.getArrival().getId());
            ps.setObject(4,f.getDepartureDate());ps.setObject(5,f.getDepartureTime());ps.setObject(6,f.getArrivalDate());ps.setObject(7,f.getArrivalTime());
            ps.setInt(8,f.getAirline().getId());ps.setInt(9,f.getId());if(ps.executeUpdate()==0)throw new IllegalArgumentException("Vuelo no encontrado");return f;
        }catch(SQLException e){throw error(e);}
    }
    @Override public boolean deleteById(Integer id){
        try(Connection c=DatabaseConnection.getConnection();PreparedStatement ps=c.prepareStatement("DELETE FROM tblFlights WHERE FlightId=?")){
            ps.setInt(1,id);return ps.executeUpdate()>0;
        }catch(SQLException e){throw error(e);}
    }
    @Override public Optional<Flight> findById(Integer id){
        try(Connection c=DatabaseConnection.getConnection();PreparedStatement ps=c.prepareStatement("SELECT "+COLUMNS+" FROM tblFlights WHERE FlightId=?")){
            ps.setInt(1,id);try(ResultSet rs=ps.executeQuery()){return rs.next()?Optional.of(read(rs)):Optional.empty();}
        }catch(SQLException e){throw error(e);}
    }
    @Override public List<Flight> findAll(){
        try(Connection c=DatabaseConnection.getConnection();PreparedStatement ps=c.prepareStatement("SELECT "+COLUMNS+" FROM tblFlights ORDER BY FlightId");ResultSet rs=ps.executeQuery()){
            List<Flight> list=new ArrayList<>();while(rs.next())list.add(read(rs));return list;
        }catch(SQLException e){throw error(e);}
    }
    @Override public boolean existsById(Integer id){return findById(id).isPresent();}
    @Override public long count(){return findAll().size();}
    @Override public Optional<Flight> findByFlightName(String name){return findAll().stream().filter(f->f.getFlightName().equals(name)).findFirst();}
    @Override public List<Flight> findByAirline(int id){return findAll().stream().filter(f->f.getAirline().getId()==id).toList();}
    @Override public List<Flight> findByDepartureDateBetween(java.time.LocalDate start,java.time.LocalDate end){return findAll().stream().filter(f->!f.getDepartureDate().isBefore(start)&&!f.getDepartureDate().isAfter(end)).toList();}
    @Override public List<Flight> findByRoute(UUID from,UUID to){return findAll().stream().filter(f->f.getDeparture().getId().equals(from)&&f.getArrival().getId().equals(to)).toList();}
    @Override public int countByAirline(int id){return findByAirline(id).size();}
    @Override public boolean existsByAirline(int id){return countByAirline(id)>0;}
    @Override public boolean existsByAirport(UUID id){return findAll().stream().anyMatch(f->f.getDeparture().getId().equals(id)||f.getArrival().getId().equals(id));}
    private void bind(PreparedStatement ps,Flight f)throws SQLException{
        ps.setInt(1,f.getId());ps.setString(2,f.getFlightName());ps.setObject(3,f.getDeparture().getId());ps.setObject(4,f.getArrival().getId());
        ps.setObject(5,f.getDepartureDate());ps.setObject(6,f.getDepartureTime());ps.setObject(7,f.getArrivalDate());ps.setObject(8,f.getArrivalTime());ps.setInt(9,f.getAirline().getId());
    }
    private Flight read(ResultSet rs)throws SQLException{
        UUID from=rs.getObject("DepartureAirportId",UUID.class),to=rs.getObject("ArrivalAirportId",UUID.class);
        int id=rs.getInt("FlightId"),airline=rs.getInt("AirlineId");
        return new Flight(id,rs.getString("FlightName"),
            airports.findById(from).orElseThrow(()->new IllegalStateException("Aeropuerto no encontrado: "+from)),
            airports.findById(to).orElseThrow(()->new IllegalStateException("Aeropuerto no encontrado: "+to)),
            rs.getObject("DepartureDate",java.time.LocalDate.class),rs.getObject("DepartureTime",java.time.LocalTime.class),
            rs.getObject("ArrivalDate",java.time.LocalDate.class),rs.getObject("ArrivalTime",java.time.LocalTime.class),
            airlines.findById(airline).orElseThrow(()->new IllegalStateException("Aerolínea no encontrada: "+airline)));
    }
    private static AirlinesException error(SQLException e){return new AirlinesException("Error de PostgreSQL en vuelos: "+e.getMessage(),e);}
}
