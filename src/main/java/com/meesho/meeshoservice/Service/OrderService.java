package com.meesho.meeshoservice.Service;

import com.meesho.meeshoservice.Constants.KafkaConstants;
import com.meesho.meeshoservice.Models.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class OrderService {

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    KafkaTemplate<String,String> kafkaTemplate;

    public String createOrder(Order order){
        order.setOrderId("123455");
        mongoTemplate.save(order,"Orders");
        sendSms(order.getOrderId());
        sendInvoice(order.getOrderId());

        return "createdSuccessfully";
        //not handling
    }

    private void sendSms(String orderId){
        try {
            kafkaTemplate.send("topicName", orderId);
        }catch(Exception e){
            log.info("sending sms failed");
            //mongoTemplate.save(order,"FailedSms");
        }
    }

    private void sendInvoice(String orderId){
        try {
            kafkaTemplate.send(KafkaConstants.CREATEINVOICE, orderId);
        }catch (Exception e){
            log.info("sending invoice failed");
            //TODO: fallback logic
        }
    }


}
