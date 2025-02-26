package com.mailSender;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class MailBody {

	 public static String readFileContent(String filePath) {
		//String filePath = "C:\\Users\\eliya\\Desktop\\Resume\\SAMPLE.txt";
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
	 
		/*
		 * public static void appendNamesToTextFile(String[] names, String filePath) {
		 * try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath,
		 * true))) { for (String name : names) { writer.write(name); writer.newLine(); }
		 * } catch (IOException e) { e.printStackTrace(); } }
		 */
	 
	 public static void modifyTextFileContent( String filePath ,String keyword, String name) {
			//String filePath = "C:\\Users\\eliya\\Desktop\\Resume\\SAMPLE.txt";
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
	 
	 public static void sendPersonalizedEmails(String from, String textFilePath, List<EmailRecipient> recipients) {
	        String keyword = "Hi "; // Word after which names will be appended

	        for (EmailRecipient recipient : recipients) {
	            // Create a temporary file for each email to avoid modifying the original file
	            String tempFilePath = textFilePath + ".temp";
	            System.out.println(tempFilePath);
	            try {
	                // Copy original file to temporary file
	                try (BufferedReader br = new BufferedReader(new FileReader(textFilePath));
	                     BufferedWriter bw = new BufferedWriter(new FileWriter(tempFilePath))) {
	                    String line;
	                    while ((line = br.readLine()) != null) {
	                        bw.write(line);
	                        bw.newLine();
	                    }
	                }

	                // Modify the temporary file content
	                modifyTextFileContent(tempFilePath, keyword, recipient.getName());

	                // Read the updated content from the temporary file
	                String emailBody = readFileContent(tempFilePath);

	                if (emailBody != null) {
	                    // Send the email
	                   MessageBody.sendEmail(from, recipient.getEmail(), emailBody);
	                }

	                // Delete the temporary file
	                new File(tempFilePath).delete();
	            } catch (IOException e) {
	                e.printStackTrace();
	            }
	        }
	    }
}
