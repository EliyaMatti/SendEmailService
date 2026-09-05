package com.mailSender.contact;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mailSender.common.exception.ApiException;
import com.mailSender.common.response.PageResponse;
import com.mailSender.common.security.AuthPrincipal;
import com.mailSender.config.ApplicationProfiles;
import com.mailSender.organization.TenantService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile(ApplicationProfiles.API)
public class ContactListService {

  private static final Logger log = LoggerFactory.getLogger(ContactListService.class);
  private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};
  private static final TypeReference<Map<String, String>> STRING_MAP = new TypeReference<>() {};

  private final ContactListRepository lists;
  private final ContactEntityRepository contacts;
  private final TenantService tenants;
  private final ObjectMapper objectMapper;

  public ContactListService(
      ContactListRepository lists,
      ContactEntityRepository contacts,
      TenantService tenants,
      ObjectMapper objectMapper) {
    this.lists = lists;
    this.contacts = contacts;
    this.tenants = tenants;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public ContactListResponse create(AuthPrincipal principal, CreateContactListRequest request) {
    UUID organizationId = requireOrg(principal);
    ContactList list = new ContactList();
    list.setOrganizationId(organizationId);
    list.setName(request.getName().trim());
    list.setTotalContacts(0);
    lists.saveAndFlush(list);
    log.info("Contact list created");
    return toResponse(list);
  }

  @Transactional(readOnly = true)
  public PageResponse<ContactListResponse> list(AuthPrincipal principal, Pageable pageable) {
    UUID organizationId = requireOrg(principal);
    Page<ContactListResponse> page =
        lists.findByOrganizationId(organizationId, pageable).map(this::toResponse);
    return PageResponse.from(page);
  }

  @Transactional(readOnly = true)
  public ContactListResponse get(AuthPrincipal principal, UUID id) {
    return toResponse(requireList(principal, id));
  }

  @Transactional
  public void delete(AuthPrincipal principal, UUID id) {
    ContactList list = requireList(principal, id);
    lists.delete(list);
    log.info("Contact list deleted");
  }

  @Transactional(readOnly = true)
  public PageResponse<ContactResponse> listContacts(
      AuthPrincipal principal, UUID listId, Pageable pageable) {
    ContactList list = requireList(principal, listId);
    Page<ContactResponse> page =
        contacts
            .findByOrganizationIdAndContactListId(list.getOrganizationId(), list.getId(), pageable)
            .map(this::toContactResponse);
    return PageResponse.from(page);
  }

  ContactList requireList(AuthPrincipal principal, UUID id) {
    UUID organizationId = requireOrg(principal);
    return lists
        .findByIdAndOrganizationId(id, organizationId)
        .orElseThrow(
            () -> new ApiException("CONTACT_LIST_NOT_FOUND", "The contact list was not found.", 404));
  }

  UUID requireOrg(AuthPrincipal principal) {
    tenants.requireMembership(principal.userId(), principal.organizationId());
    return principal.organizationId();
  }

  ContactListResponse toResponse(ContactList list) {
    return new ContactListResponse(
        list.getId(),
        list.getOrganizationId(),
        list.getName(),
        list.getSourceFilename(),
        list.getTotalContacts(),
        readKeys(list.getPlaceholderKeys()),
        list.getCreatedAt(),
        list.getUpdatedAt());
  }

  private ContactResponse toContactResponse(ContactEntity entity) {
    return new ContactResponse(
        entity.getId(),
        entity.getContactListId(),
        entity.getEmail(),
        entity.getName(),
        entity.getStatus(),
        readMetadata(entity.getMetadataJson()),
        entity.getCreatedAt());
  }

  private List<String> readKeys(String json) {
    if (json == null || json.isBlank()) {
      return List.of();
    }
    try {
      return objectMapper.readValue(json, STRING_LIST);
    } catch (JsonProcessingException e) {
      return List.of();
    }
  }

  private Map<String, String> readMetadata(String json) {
    if (json == null || json.isBlank()) {
      return Map.of();
    }
    try {
      return objectMapper.readValue(json, STRING_MAP);
    } catch (JsonProcessingException e) {
      return Map.of();
    }
  }
}
