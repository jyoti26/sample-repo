package com.meesho.meeshoservice.Models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Order {

    @Id
    private String orderId;
    private String orderState;
    private String invoiceId;

    private String phoneNo;

    private String email;

}
