package com.mailSender;

import jakarta.mail.MessagingException;
import java.io.File;
import java.util.List;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Attaches local files to a MIME message from paths on {@link
 * com.mailSender.campaign.EmailMessage}; it does not send mail.
 */
@Component
public class MailBodyAttachment {

  public void addAttachments(MimeMessageHelper helper, List<String> filePaths)
      throws MessagingException {
    if (filePaths == null || filePaths.isEmpty()) {
      return;
    }
    for (String filePath : filePaths) {
      if (filePath == null || filePath.isBlank()) {
        continue;
      }
      File file = new File(filePath);
      if (!file.exists() || !file.canRead()) {
        throw new RuntimeException("Cannot read file: " + file.getAbsolutePath());
      }
      helper.addAttachment(file.getName(), file);
    }
  }
}
