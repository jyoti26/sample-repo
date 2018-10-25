package com.meesho.meeshoservice.Service;

import com.meesho.meeshoservice.Constants.KafkaConstants;
import com.meesho.meeshoservice.Models.Order;
import com.meesho.meeshoservice.Models.QueueFailure;
import com.meesho.meeshoservice.Repository.OrderRepo;
import com.meesho.meeshoservice.util.Utility;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;


@Service
@Slf4j
public class OrderService {

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    Utility utility;

    @Autowired
    OrderRepo orderRepo;

    public String createOrder(Order order){
        order.setOrderId("123455");
        orderRepo.save(order);
        sendSms(order.getOrderId());
        sendInvoice(order.getOrderId());

        return "createdSuccessfully";
        //not handling
    }

    private void sendSms(String orderId){
        utility.uploadinKafka(KafkaConstants.SENDSMS,orderId);
    }

    private void sendInvoice(String orderId){
        utility.uploadinKafka(KafkaConstants.CREATEINVOICE, orderId);
    }



}
