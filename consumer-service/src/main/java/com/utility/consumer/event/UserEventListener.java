package com.utility.consumer.event;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.utility.common.event.UserRegisteredEvent;
import com.utility.consumer.entity.Consumer;
import com.utility.consumer.repository.ConsumerRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserEventListener {
	private final ConsumerRepository consumerRepo;
    @KafkaListener(topics = "user-registered", groupId = "consumer-group")
    public void handleUserRegistration(UserRegisteredEvent event) {
        log.info("Received Event: Create Profile for {}", event.getUsername());
        consumerRepo.findByUserId(event.getUserId())
            .hasElement()
            .flatMap(exists -> {
                if (exists) {
                    log.info("Profile already exists for {}", event.getUserId());
                    return Mono.empty();
                }
                Consumer consumer = new Consumer();
                consumer.setUserId(event.getUserId());
                consumer.setFirstName(event.getFirstName());
                consumer.setLastName(event.getLastName());
                consumer.setEmail(event.getEmail());
                consumer.setAddress(event.getAddress());
                consumer.setPhoneNumber(""); 
                return consumerRepo.save(consumer).doOnSuccess(c -> log.info("Auto-created profile: {}", c.getId()));
            })
            .subscribe();
    }
}
