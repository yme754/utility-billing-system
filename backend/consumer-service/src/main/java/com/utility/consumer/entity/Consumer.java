package com.utility.consumer.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "consumers")
public class Consumer {
	@Id
	private String id;
	@Indexed(unique = true)
	private String userId;
	private String firstName;
	private String lastName;
	private String phoneNumber;
	private String email;
	private String address;
	private boolean active;
	private String profileImageUrl;
}
