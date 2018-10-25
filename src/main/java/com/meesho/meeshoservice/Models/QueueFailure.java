package com.meesho.meeshoservice.Models;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
@Data
public class QueueFailure {

    public QueueFailure(String topicName, String orderId){
        super();
        this.orderId = orderId;
        this.topicName = topicName;
    }

    private String topicName;
    private String orderId;
}
