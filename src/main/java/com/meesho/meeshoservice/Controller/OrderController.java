package com.meesho.meeshoservice.Controller;

import com.meesho.meeshoservice.Models.Order;
import com.meesho.meeshoservice.Service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @RequestMapping(method = RequestMethod.POST,value = "/createOrder")
    public String createOrder(@RequestBody Order order){
        return orderService.createOrder(order);
    }

}
