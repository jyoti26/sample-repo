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
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class NotificationService {

    @Autowired
    RedisTemplate redisTemplate;

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    KafkaTemplate<String,String> kafkaTemplate;

    int smsRetryCount;

    int emailRetryCount;

    @KafkaListener(containerFactory = "kafkaListenerContainerFactory",topics = KafkaConstants.SENDSMS)
    public void sendSmsConsumer(String message){
        if(redisTemplate.opsForHash().hasKey(message,"sendSms")){
            log.info("message already sent");
            return;
        }
        sendSms(message);
        log.info("sms sent successfully for order {} ",message);

    }

    @KafkaListener(containerFactory = "kafkaListenerContainerFactory",topics = KafkaConstants.RETRYSMS)
    public void retrySMSConsumer(String message){
        int retryCount = (Integer) redisTemplate.opsForHash().get(message,"retrySMS");
        Boolean isSMSsent = (Boolean) redisTemplate.opsForHash().get(message,"sendSMS"); //to create a lock (not req in kafka as one message is sent to one instance from a consumer group.)
        if(retryCount<=smsRetryCount && !isSMSsent){
            sendSms(message);
        }else{
            kafkaTemplate.send("failedSMS",message);
        }
    }

    private void sendSms(String orderId){
        redisTemplate.opsForHash().put(orderId,"sendSms","true"); //locked
        Order order = mongoTemplate.findOne(new Query(Criteria.where("_id").is(orderId)), Order.class, "Orders");
        try{
            log.info("sending message for order {} to {}...", orderId, order.getPhoneNo());
        }catch (Exception e){
            //if message sending fails:
            retryLogic(orderId,"retrySMS",KafkaConstants.RETRYSMS);
            // for new request only checking if that request exists or not. making it false so that we can differentiate between success and failed cases.
            redisTemplate.opsForHash().put(orderId,"sendSms","false"); //lock released
        }
    }

    @KafkaListener(containerFactory = "kafkaListenerContainerFactory",topics = KafkaConstants.SENDEMAIL)
    public void sendEmailConsumer(String message){
        Invoice invoice = mongoTemplate.findOne(new Query(Criteria.where("orderId").is(message)), Invoice.class, "Invoice");

        if(StringUtils.isEmpty(invoice.getAttachment()) && !redisTemplate.opsForHash().hasKey(invoice.getOrderId(),"sentInvocieNoAttachment")){
            try{
                log.info("sending email without attachment successfully for order {} to mailId {} ",message,invoice.getEmailId());
                redisTemplate.opsForHash().put(message,"sentInvocieNoAttachment",true);
            }catch (Exception e){
                redisTemplate.opsForHash().put(message,"sentInvocieNoAttachment",false);
                //not retrying  as this is already an extra mail.
            }
        }else if(redisTemplate.opsForHash().hasKey(message,"sentInvoiceAttachment")){
            log.info("email with attachment has already been sent for order {} ",invoice.getOrderId());
            return;
        }
        sendEmail(message,invoice);

    }

    @KafkaListener(containerFactory = "kafkaListenerContainerFactory",topics = KafkaConstants.RETRYEMAIL)
    public void retryEmailConsumer(String message){
        int retryCount = (Integer) redisTemplate.opsForHash().get(message,"retryEmail");
        boolean isEmailSent = (Boolean) redisTemplate.opsForHash().get(message,"sentInvoiceAttachment");
        if(retryCount<=emailRetryCount && !isEmailSent){
            Invoice invoice = mongoTemplate.findOne(new Query(Criteria.where("_id").is(message)), Invoice.class, "Invoice");
            sendEmail(message, invoice);
        }else{
            kafkaTemplate.send("failedSMS",message);
        }
    }

    private void sendEmail(String orderId, Invoice invoice){
        redisTemplate.opsForHash().put(orderId,"sentInvoiceAttachment",true); //locked
        try{
            log.info("sending email with attachment for order {} to email Id {}", invoice.getOrderId(),invoice.getEmailId());
        }catch (Exception e){
            retryLogic(orderId,"sentInvoiceAttachment",KafkaConstants.RETRYEMAIL);
            redisTemplate.opsForHash().put(orderId,"sentInvoiceAttachment",false); //lock released
        }
    }


    private void retryLogic(String message, String redisField, String kafkaConstant){
        redisTemplate.opsForHash().increment(message,redisField,1);
        kafkaTemplate.send(kafkaConstant,message);
    }


}
