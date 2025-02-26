package com.mailSender;

import java.util.List;

public class EmailSender {

    public static void main(String[] args) {
        String fromEmail = "eliyamatti1@gmail.com";
        String excelFilePath = "C:\\Users\\eliya\\Desktop\\Resume\\HRNames.xlsx";
        String textFilePath = "C:\\Users\\eliya\\Desktop\\Resume\\SAMPLE.txt";


        try {
            List<EmailRecipient> recipients = ReadFromExcel.readEmailsAndNamesFromExcel(excelFilePath);

            MailBody.sendPersonalizedEmails(fromEmail, textFilePath, recipients);

        
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }
}
