package com.utility.billing.repository;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

import com.utility.billing.entity.Bill;

@Repository
public interface BillRepository extends ReactiveMongoRepository<Bill, String>{
}
