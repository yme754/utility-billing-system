package com.utility.billing.repository;

import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

import com.utility.billing.entity.Bill;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface BillRepository extends ReactiveMongoRepository<Bill, String>{
	Flux<Bill> findByStatus(String status);
    Flux<Bill> findByConnectionId(String connectionId);
    Mono<Long> countByStatus(String status);
    @Aggregation("{ $match: { status: 'PAID' } }, { $group: { _id: null, total: { $sum: '$totalAmount' } } }")
    Mono<RevenueResult> sumTotalRevenue();

    class RevenueResult {
        private String id;
        private Double total;
        public Double getTotal() { 
        	return total; 
        }
        public void setTotal(Double total) { 
        	this.total = total; 
        }
    }
}
