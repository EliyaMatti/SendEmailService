package com.mailSender;

public class EmailRecipient {
	private String email;
	private String name;

	public EmailRecipient(String email, String name) {
		this.email = email;
		this.name = name;
	}

	public String getEmail() {
		return email;
	}

	public String getName() {
		return name;
	}
}
