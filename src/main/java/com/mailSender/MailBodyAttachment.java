package com.mailSender;

import jakarta.mail.MessagingException;
import java.io.File;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
public class MailBodyAttachment {

  private final MailAppProperties mailAppProperties;

  public MailBodyAttachment(MailAppProperties mailAppProperties) {
    this.mailAppProperties = mailAppProperties;
  }

  public void addAttachment(MimeMessageHelper helper) throws MessagingException {
    String filePath = mailAppProperties.getAttachmentPath();
    if (filePath == null || filePath.isBlank()) {
      return;
    }
    File file = new File(filePath);
    if (!file.exists() || !file.canRead()) {
      throw new RuntimeException("Cannot read file: " + file.getAbsolutePath());
    }
    helper.addAttachment(file.getName(), file);
  }
}
