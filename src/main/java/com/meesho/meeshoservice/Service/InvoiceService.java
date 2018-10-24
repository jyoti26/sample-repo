package com.meesho.meeshoservice.Service;

import com.meesho.meeshoservice.Constants.KafkaConstants;
import com.meesho.meeshoservice.Models.Invoice;
import com.meesho.meeshoservice.Models.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class InvoiceService {

    @Autowired
    KafkaTemplate<String,String> kafkaTemplate;

    @Autowired
    RedisTemplate redisTemplate;

    @Autowired
    MongoTemplate mongoTemplate;

    int invoiceRetryCount;

    @KafkaListener(containerFactory = "kafkaListenerContainerFactory",topics = KafkaConstants.CREATEINVOICE)
    public void createInvoiceConsumer(String message){
        if(redisTemplate.opsForHash().hasKey(message,"invoiceGenerated")){
            return;
        }
        createInvoice(message);
        sendEmail(message);
    }

    @KafkaListener(containerFactory = "kafkaListenerContainerFactory",groupId = "invoice",topics = KafkaConstants.CREATEINVOICE)
    public void retryInvoiceConsumer(String message){
        int retryCount = (Integer) redisTemplate.opsForHash().get(message,"retryInvoice");
        Boolean isInvoiceGenerated = (Boolean) redisTemplate.opsForHash().get(message,"invoiceGenerated");
        if(retryCount<=invoiceRetryCount && !isInvoiceGenerated){
            createInvoice(message);
        }else{
            log.info("invoice sent is failed");
            kafkaTemplate.send("failedInvoice",message);
        }

    }

    private void createInvoice(String message){
        redisTemplate.opsForHash().put(message,"invoiceGenerated",true);
        try {
            Order order = mongoTemplate.findOne(new Query(Criteria.where("_id").is(message)), Order.class, "Orders");
            Invoice invoice = new Invoice();
            invoice.setInvoiceId("123456");
            invoice.setOrderId(order.getOrderId());
            invoice.setAttachment("abcd");
            invoice.setEmailId(order.getEmail());
            log.info("invoice created successfully for order {} ", message);
            mongoTemplate.save(invoice, "Invoice");
            //if in case invoice generation fails
        }catch (Exception e){
            redisTemplate.opsForHash().put(message,"invoiceGenerated",false);
            retryLogic(message,"retryInvoice",KafkaConstants.RETRYINVOICE);
        }
    }

    private void retryLogic(String message, String redisField, String kafkaConstant){
        redisTemplate.opsForHash().increment(message,redisField,1);
        kafkaTemplate.send(kafkaConstant,message);
    }



    public void sendEmail(String message){
        kafkaTemplate.send(KafkaConstants.SENDEMAIL,message);
    }


}
