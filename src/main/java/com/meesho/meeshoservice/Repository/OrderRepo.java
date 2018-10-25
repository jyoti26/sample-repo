package com.meesho.meeshoservice.Repository;

import com.meesho.meeshoservice.Models.Order;
import org.springframework.data.mongodb.repository.MongoRepository;


public interface OrderRepo extends MongoRepository<Order,String> {


}
