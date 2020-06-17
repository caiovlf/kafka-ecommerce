package com.caiovlf.ecommerce;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

public class FraudDetectorService {

    private final KafkaDispatcher<Order> orderDispatcher = new KafkaDispatcher<>();

    public static void main(String[] args) {
        var fraudService = new FraudDetectorService();
        try(var service = new KafkaService<>(FraudDetectorService.class.getSimpleName(),
                "ECOMMERCE_NEW_ORDER",
                fraudService::parse,
                Order.class,
                Map.of())){
            service.run();
        }
    }

    private void parse(ConsumerRecord<String, Order> record) throws ExecutionException, InterruptedException {
        System.out.println("--------------------------------------");
        System.out.println("Processing new order, checking for fraud");
        System.out.println("Key: " + record.key());
        System.out.println("Value: " + record.value());
        System.out.println("Partition: " + record.partition());
        System.out.println("OffSet: " + record.offset());
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            //ignoring
            e.printStackTrace();
        }

        //simulating a fraud
        var order = record.value();
        if(isFraud(order)){
            System.err.println("Order is a fraud!!! " +order);
            orderDispatcher.send("ECOMMERCE_ORDER_REJECTED", order.getUserId(),order);
        }else{
            System.out.println("Order has been approved! "+order);
            orderDispatcher.send("ECOMMERCE_ORDER_APPROVED", order.getUserId(),order);
        }
    }

    private boolean isFraud(Order order) {
        return order.getAmount().compareTo(new BigDecimal("45000")) >= 0;
    }

    private static Properties properties() {
        var properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, FraudDetectorService.class.getSimpleName());
        properties.setProperty(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "1");
        return properties;
    }

}
