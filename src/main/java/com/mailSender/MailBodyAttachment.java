package com.mailSender;

import java.io.File;

import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.activation.FileDataSource;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeBodyPart;

public class MailBodyAttachment {

	public static MimeBodyPart attachemt() {
		MimeBodyPart attachmentBodyPart = new MimeBodyPart();
		try {
			String filePath = "C:\\Users\\eliya\\Desktop\\Resume\\Java_Developer_Resume_Eliya.pdf";
			File file = new File(filePath);
			if (!file.exists() || !file.canRead()) {
				throw new RuntimeException("Cannot read file: " + file.getAbsolutePath());
			}
			DataSource source = new FileDataSource(filePath);
			attachmentBodyPart.setDataHandler(new DataHandler(source));
			attachmentBodyPart.setFileName(file.getName());

		} catch (MessagingException e) {
			e.printStackTrace();
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
		return attachmentBodyPart;
	}

}
