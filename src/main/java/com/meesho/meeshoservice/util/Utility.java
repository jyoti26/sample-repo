package com.meesho.meeshoservice.util;

import com.meesho.meeshoservice.Models.QueueFailure;
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
public class Utility {

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    KafkaTemplate<String,String> kafkaTemplate;

    public void uploadinKafka(String topicName, String orderId){
        ListenableFuture<SendResult<String,String>> future;
        try{
            future = kafkaTemplate.send(topicName,orderId);
            future.addCallback(
                    new ListenableFutureCallback<SendResult<String, String>>() {
                        @Override
                        public void onFailure(Throwable throwable) {
                            QueueFailure queueFailure = new QueueFailure(topicName,orderId);
                            mongoTemplate.save(queueFailure);
                        }

                        @Override
                        public void onSuccess(SendResult<String, String> stringStringSendResult) {
                            log.info("pushed successfully in kafka cache with topic {} for orderId {} ",topicName,orderId);
                        }
                    }
            );
        }
        catch (Exception e){
            log.error("pushing into the queue failed for topic {} and order {}", topicName,orderId);
            QueueFailure queueFailure = new QueueFailure(topicName,orderId);
            mongoTemplate.save(queueFailure);
        }
    }
}
