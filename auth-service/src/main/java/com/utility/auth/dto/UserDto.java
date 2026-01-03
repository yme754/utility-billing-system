package com.utility.auth.dto;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserDto {
	private String id;
    private String username;
    private String email;
    private List<String> roles;
    private String status;
}
