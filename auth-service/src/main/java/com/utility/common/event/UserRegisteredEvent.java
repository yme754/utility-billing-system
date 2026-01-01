package com.utility.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisteredEvent {
	private String userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String address;
}
