package com.mailSender.mailtemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mailSender.common.exception.ApiException;
import com.mailSender.common.response.PageResponse;
import com.mailSender.common.security.AuthPrincipal;
import com.mailSender.config.ApplicationProfiles;
import com.mailSender.contact.ContactList;
import com.mailSender.contact.ContactListRepository;
import com.mailSender.organization.TenantService;
import com.mailSender.template.TemplateValidationException;
import com.mailSender.template.TemplateValidator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile(ApplicationProfiles.API)
public class TemplateService {

  private static final Logger log = LoggerFactory.getLogger(TemplateService.class);
  private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};

  private final StoredEmailTemplateRepository templates;
  private final ContactListRepository lists;
  private final TenantService tenants;
  private final ObjectMapper objectMapper;

  public TemplateService(
      StoredEmailTemplateRepository templates,
      ContactListRepository lists,
      TenantService tenants,
      ObjectMapper objectMapper) {
    this.templates = templates;
    this.lists = lists;
    this.tenants = tenants;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public TemplateResponse create(AuthPrincipal principal, UpsertTemplateRequest request) {
    UUID orgId = org(principal);
    validate(request, orgId);
    StoredEmailTemplate entity = new StoredEmailTemplate();
    entity.setOrganizationId(orgId);
    apply(entity, request);
    templates.saveAndFlush(entity);
    log.info("Email template created");
    return toResponse(entity);
  }

  @Transactional(readOnly = true)
  public PageResponse<TemplateResponse> list(AuthPrincipal principal, Pageable pageable) {
    UUID orgId = org(principal);
    return PageResponse.from(
        templates.findByOrganizationId(orgId, pageable).map(this::toResponse));
  }

  @Transactional(readOnly = true)
  public TemplateResponse get(AuthPrincipal principal, UUID id) {
    return toResponse(require(principal, id));
  }

  @Transactional
  public TemplateResponse update(AuthPrincipal principal, UUID id, UpsertTemplateRequest request) {
    StoredEmailTemplate entity = require(principal, id);
    validate(request, entity.getOrganizationId());
    apply(entity, request);
    templates.saveAndFlush(entity);
    log.info("Email template updated");
    return toResponse(entity);
  }

  @Transactional
  public void delete(AuthPrincipal principal, UUID id) {
    templates.delete(require(principal, id));
    log.info("Email template deleted");
  }

  public StoredEmailTemplate require(AuthPrincipal principal, UUID id) {
    UUID orgId = org(principal);
    return templates
        .findByIdAndOrganizationId(id, orgId)
        .orElseThrow(
            () -> new ApiException("TEMPLATE_NOT_FOUND", "The email template was not found.", 404));
  }

  public void validateAgainstList(String subject, String body, ContactList list) {
    Set<String> keys = new LinkedHashSet<>();
    keys.add("email");
    keys.add("name");
    if (list.getPlaceholderKeys() != null && !list.getPlaceholderKeys().isBlank()) {
      try {
        keys.addAll(objectMapper.readValue(list.getPlaceholderKeys(), STRING_LIST));
      } catch (JsonProcessingException ignored) {
        // fall back to email/name
      }
    }
    try {
      TemplateValidator.validate(subject, body, keys);
    } catch (TemplateValidationException e) {
      throw new ApiException("TEMPLATE_INVALID", e.getMessage(), 400);
    }
  }

  private void validate(UpsertTemplateRequest request, UUID orgId) {
    if (request.getContactListId() == null) {
      try {
        TemplateValidator.validate(request.getSubject(), request.getBody(), Set.of("email", "name"));
      } catch (TemplateValidationException e) {
        throw new ApiException("TEMPLATE_INVALID", e.getMessage(), 400);
      }
      return;
    }
    ContactList list =
        lists
            .findByIdAndOrganizationId(request.getContactListId(), orgId)
            .orElseThrow(
                () ->
                    new ApiException(
                        "CONTACT_LIST_NOT_FOUND", "The contact list was not found.", 404));
    validateAgainstList(request.getSubject(), request.getBody(), list);
  }

  private void apply(StoredEmailTemplate entity, UpsertTemplateRequest request) {
    entity.setName(request.getName().trim());
    entity.setSubject(request.getSubject());
    entity.setBody(request.getBody());
  }

  private TemplateResponse toResponse(StoredEmailTemplate entity) {
    return new TemplateResponse(
        entity.getId(),
        entity.getOrganizationId(),
        entity.getName(),
        entity.getSubject(),
        entity.getBody(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }

  private UUID org(AuthPrincipal principal) {
    tenants.requireMembership(principal.userId(), principal.organizationId());
    return principal.organizationId();
  }
}
