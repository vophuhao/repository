package vn.iostar.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import vn.iostar.model.MailBody;

@Service
public class EmailService {

	private final JavaMailSender javaMailSender;

	public EmailService (JavaMailSender javaMailSender) {
		this.javaMailSender = javaMailSender;
	}

	public void sendSimpleMessage (MailBody mailBody) {
		SimpleMailMessage message = new SimpleMailMessage();
		message.setTo(mailBody.to());
		message.setFrom("trantiendat1679@gmail.com");
		message.setSubject(mailBody.subject());
		message.setText(mailBody.text());
		
		javaMailSender.send(message);
	}
}
