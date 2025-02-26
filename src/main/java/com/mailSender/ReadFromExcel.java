package com.mailSender;


import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ReadFromExcel {


	 public static List<EmailRecipient> readEmailsAndNamesFromExcel(String filePath) {
	        List<EmailRecipient> recipients = new ArrayList<>();

	        try (FileInputStream fis = new FileInputStream(filePath);
	             Workbook workbook = new XSSFWorkbook(fis)) {

	            Sheet sheet = workbook.getSheetAt(0); // Get the first sheet
	            for (Row row : sheet) {
	                Cell emailCell = row.getCell(0); // First column for email
	                Cell nameCell = row.getCell(1);  // Second column for name
	                if (emailCell != null && emailCell.getCellType() == CellType.STRING &&
	                    nameCell != null && nameCell.getCellType() == CellType.STRING) {
	                    String email = emailCell.getStringCellValue();
	                    String name = nameCell.getStringCellValue();
	                    recipients.add(new EmailRecipient(email, name));
	                }
	            }

	        } catch (IOException e) {
	            e.printStackTrace();
	        }

	        return recipients;
	    }

}
