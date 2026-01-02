package com.utility.consumer.event;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import com.utility.common.event.UserRegisteredEvent;
import com.utility.consumer.service.ConsumerService;
import com.utility.consumer.dto.ConsumerDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class UserEventListener {

    private final ConsumerService consumerService;
    @KafkaListener(topics = "user-registered", groupId = "consumer-group")
    public void handleUserRegistered(UserRegisteredEvent event) {
        log.info("Received User Registered Event for userId: {}", event.getUserId());
        ConsumerDTO consumerDTO = new ConsumerDTO();
        consumerDTO.setUserId(event.getUserId());
        consumerDTO.setFirstName(event.getFirstName());
        consumerDTO.setLastName(event.getLastName());
        consumerDTO.setEmail(event.getEmail());
        consumerService.createProfile(consumerDTO)
            .subscribe(
                success -> log.info("Profile created successfully for user: {}", event.getUsername()),
                error -> log.error("Failed to create profile for user: {}", event.getUsername(), error)
            );
    }
}