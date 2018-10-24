package com.meesho.meeshoservice.Models;

import lombok.Data;

@Data
public class Invoice {
    private String invoiceId;

    private String orderId;

    private Double finalPrice;

    private String attachment;

    private String emailId;
}
