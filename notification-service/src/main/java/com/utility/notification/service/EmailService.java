package com.utility.notification.service;

import com.utility.notification.dto.EmailRequest;

public interface EmailService {
	void sendEmail(EmailRequest emailRequest);
}
