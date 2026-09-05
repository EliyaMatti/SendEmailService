package com.mailSender.contact;

import com.mailSender.common.response.ApiResponse;
import com.mailSender.common.response.PageResponse;
import com.mailSender.common.security.AuthPrincipal;
import com.mailSender.config.ApplicationProfiles;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile(ApplicationProfiles.API)
@RequestMapping("/api/v1/contact-lists")
public class ContactListController {

  private final ContactListService contactLists;

  public ContactListController(ContactListService contactLists) {
    this.contactLists = contactLists;
  }

  @PostMapping
  public ApiResponse<ContactListResponse> create(
      @AuthenticationPrincipal AuthPrincipal principal,
      @Valid @RequestBody CreateContactListRequest request) {
    return ApiResponse.ok(contactLists.create(principal, request));
  }

  @GetMapping
  public ApiResponse<PageResponse<ContactListResponse>> list(
      @AuthenticationPrincipal AuthPrincipal principal,
      @PageableDefault(size = 20) Pageable pageable) {
    return ApiResponse.ok(contactLists.list(principal, pageable));
  }

  @GetMapping("/{id}")
  public ApiResponse<ContactListResponse> get(
      @AuthenticationPrincipal AuthPrincipal principal, @PathVariable UUID id) {
    return ApiResponse.ok(contactLists.get(principal, id));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(
      @AuthenticationPrincipal AuthPrincipal principal, @PathVariable UUID id) {
    contactLists.delete(principal, id);
    return ApiResponse.ok(null);
  }

  @GetMapping("/{id}/contacts")
  public ApiResponse<PageResponse<ContactResponse>> contacts(
      @AuthenticationPrincipal AuthPrincipal principal,
      @PathVariable UUID id,
      @PageableDefault(size = 20) Pageable pageable) {
    return ApiResponse.ok(contactLists.listContacts(principal, id, pageable));
  }
}
