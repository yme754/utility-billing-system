package com.utility.meter.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class JwtPropertiesTest {

	    @Test
	    void gettersAndSetters_workCorrectly() {
	        JwtProperties props = new JwtProperties();
	        props.setSecret("abc123");
	        props.setExpiration(3600L);

	        assertEquals("abc123", props.getSecret());
	        assertEquals(3600L, props.getExpiration());
	    
	    }
}