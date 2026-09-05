package com.mailSender.contact;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mailSender.common.exception.ApiException;
import com.mailSender.common.security.AuthPrincipal;
import com.mailSender.config.ApplicationProfiles;
import com.mailSender.config.ExcelmailProperties;
import com.mailSender.excel.Contact;
import com.mailSender.excel.ExcelProcessingException;
import com.mailSender.excel.ExcelReadResult;
import com.mailSender.excel.ExcelReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Profile(ApplicationProfiles.API)
public class ContactImportService {

  private static final Logger log = LoggerFactory.getLogger(ContactImportService.class);

  private final ContactListService lists;
  private final ContactEntityRepository contacts;
  private final ContactListRepository listRepository;
  private final ExcelmailProperties properties;
  private final ObjectMapper objectMapper;

  public ContactImportService(
      ContactListService lists,
      ContactEntityRepository contacts,
      ContactListRepository listRepository,
      ExcelmailProperties properties,
      ObjectMapper objectMapper) {
    this.lists = lists;
    this.contacts = contacts;
    this.listRepository = listRepository;
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public ImportSummaryResponse upload(AuthPrincipal principal, UUID listId, MultipartFile file) {
    ContactList list = lists.requireList(principal, listId);
    if (file == null || file.isEmpty()) {
      throw new ApiException("FILE_REQUIRED", "An Excel or CSV file is required.", 400);
    }
    if (file.getSize() > properties.getUpload().getMaxFileBytes()) {
      throw new ApiException("FILE_TOO_LARGE", "The uploaded file exceeds the allowed size.", 413);
    }
    String original = safeFilename(file.getOriginalFilename());
    String lower = original.toLowerCase(Locale.ROOT);
    ExcelReadResult parsed;
    try {
      if (lower.endsWith(".xlsx")) {
        parsed = readXlsx(file);
      } else if (lower.endsWith(".csv")) {
        parsed = readCsv(file);
      } else {
        throw new ApiException(
            "UNSUPPORTED_FILE_TYPE", "Upload a .xlsx or .csv file.", 400);
      }
    } catch (ExcelProcessingException e) {
      throw new ApiException("INVALID_SPREADSHEET", e.getMessage(), 400);
    } catch (IllegalArgumentException e) {
      throw new ApiException("INVALID_SPREADSHEET", e.getMessage(), 400);
    } catch (IOException e) {
      throw new ApiException("INVALID_SPREADSHEET", "Unable to read the uploaded file.", 400);
    }

    contacts.deleteByContactListIdAndOrganizationId(list.getId(), list.getOrganizationId());
    List<ContactEntity> saved = new ArrayList<>();
    for (Contact contact : parsed.getContacts()) {
      ContactEntity entity = new ContactEntity();
      entity.setContactListId(list.getId());
      entity.setOrganizationId(list.getOrganizationId());
      entity.setEmail(contact.getEmail());
      entity.setName(contact.getName() == null || contact.getName().isBlank() ? "" : contact.getName());
      entity.setStatus(ContactStatus.ACTIVE);
      entity.setMetadataJson(toJson(extras(contact)));
      saved.add(entity);
    }
    contacts.saveAll(saved);

    list.setSourceFilename(original);
    list.setTotalContacts(saved.size());
    list.setPlaceholderKeys(toJson(new ArrayList<>(parsed.getPlaceholderKeys())));
    listRepository.saveAndFlush(list);
    log.info(
        "Contact import finished totalRows={} valid={} invalid={} duplicates={}",
        parsed.getTotalRows(),
        parsed.getValid(),
        parsed.getInvalid(),
        parsed.getDuplicates());
    return new ImportSummaryResponse(
        parsed.getTotalRows(),
        parsed.getValid(),
        parsed.getInvalid(),
        parsed.getDuplicates(),
        parsed.getRowErrors(),
        lists.toResponse(list));
  }

  private ExcelReadResult readXlsx(MultipartFile file) throws IOException {
    Path temp = Files.createTempFile("excelmail-import-", ".xlsx");
    try {
      file.transferTo(temp);
      return ExcelReader.read(temp.toString());
    } finally {
      Files.deleteIfExists(temp);
    }
  }

  private ExcelReadResult readCsv(MultipartFile file) throws IOException {
    try (InputStream in = file.getInputStream()) {
      return CsvContactReader.read(in);
    }
  }

  private static String safeFilename(String original) {
    if (original == null || original.isBlank()) {
      return "upload";
    }
    String name = Path.of(original).getFileName().toString();
    if (name.contains("..")) {
      throw new ApiException("UNSUPPORTED_FILE_TYPE", "Upload a .xlsx or .csv file.", 400);
    }
    return name;
  }

  private static Map<String, String> extras(Contact contact) {
    Map<String, String> extras = new LinkedHashMap<>();
    contact
        .getPlaceholders()
        .forEach(
            (key, value) -> {
              if (!"email".equals(key) && !"name".equals(key)) {
                extras.put(key, value);
              }
            });
    return extras;
  }

  private String toJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      return "[]";
    }
  }
}
