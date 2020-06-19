package com.caiovlf.ecommerce;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException; 

public class CreateUserService {

    private final Connection connection;

    CreateUserService() throws SQLException {
        String url = "jdbc:sqlite:users_database.db";
        this.connection = DriverManager.getConnection(url);
        connection.createStatement().execute("CREATE TABLE IF NOT EXISTS Users (" +
                "uuid varchar(200) primary key, " +
                "email varchar(200))");
    }

    public static void main(String[] args) throws SQLException {
        var createUserService = new CreateUserService();
        try(var service = new KafkaService<>(CreateUserService.class.getSimpleName(),
                "ECOMMERCE_NEW_ORDER",
                createUserService::parse,
                Order.class,
                Map.of())){
            service.run();
        }
    }

    private void parse(ConsumerRecord<String, Order> record) throws ExecutionException, InterruptedException, SQLException {
        System.out.println("--------------------------------------");
        System.out.println("Processing new order, checking for new user");
        System.out.println("Value: " + record.value());

        var order = record.value();
        if(isNewUser(order.getEmail())){
            insertNewUser(order.getEmail());
        }

    }

    private void insertNewUser(String email) throws SQLException {
        var insert = connection.prepareStatement("insert into Users (uuid, email) " +
                "values (?,?)");
        String uuid = UUID.randomUUID().toString();
        insert.setString(1, uuid);
        insert.setString(2, email);
        insert.execute();
        System.out.println("Usuário:" +uuid + " - email: "+email + " adicionado");
    }

    private boolean isNewUser(String email) throws SQLException {
        var exists = connection.prepareStatement("select uuid from Users " +
                "where email = ? limit 1");
        exists.setString(1,email);
        var results = exists.executeQuery();
        return !results.next();
    }

}
