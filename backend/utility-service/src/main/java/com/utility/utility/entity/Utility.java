package com.utility.utility.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "utilities")
public class Utility {
	@Id
	private String Id;
	@Indexed(unique = true)
	private String name;
	private String unitOfMeasure;
	private boolean active;
}
