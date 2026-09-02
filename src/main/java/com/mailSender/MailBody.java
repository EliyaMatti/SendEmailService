package com.mailSender;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class MailBody {

  private final EmailService emailService;

  public MailBody(EmailService emailService) {
    this.emailService = emailService;
  }

  public static String readFileContent(String filePath) {
    StringBuilder content = new StringBuilder();
    try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
      String line;
      while ((line = br.readLine()) != null) {
        content.append(line).append("\n");
      }
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
    return content.toString();
  }

  public static void modifyTextFileContent(String filePath, String keyword, String name) {
    StringBuilder content = new StringBuilder();
    try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
      String line;
      while ((line = br.readLine()) != null) {
        if (line.contains(keyword)) {
          content.append(line.replace(keyword, keyword + " " + name)).append("\n");
        } else {
          content.append(line).append("\n");
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
      writer.write(content.toString());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void sendPersonalizedEmails(String textFilePath, List<EmailRecipient> recipients) {
    String keyword = "Hi ";

    for (EmailRecipient recipient : recipients) {
      String tempFilePath = textFilePath + ".temp";
      System.out.println(tempFilePath);
      try {

        try (BufferedReader br = new BufferedReader(new FileReader(textFilePath));
            BufferedWriter bw = new BufferedWriter(new FileWriter(tempFilePath))) {
          String line;
          while ((line = br.readLine()) != null) {
            bw.write(line);
            bw.newLine();
          }
        }

        modifyTextFileContent(tempFilePath, keyword, recipient.getName());
        String emailBody = readFileContent(tempFilePath);
        if (emailBody != null) {
          emailService.sendEmail(recipient.getEmail(), emailBody);
        }
        new File(tempFilePath).delete();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}
