package com.mailSender.common.response;

import java.util.List;
import org.springframework.data.domain.Page;

public class PageResponse<T> {

  private List<T> items;
  private int page;
  private int size;
  private long totalItems;
  private int totalPages;

  public static <T> PageResponse<T> from(Page<T> page) {
    PageResponse<T> response = new PageResponse<>();
    response.items = page.getContent();
    response.page = page.getNumber();
    response.size = page.getSize();
    response.totalItems = page.getTotalElements();
    response.totalPages = page.getTotalPages();
    return response;
  }

  public List<T> getItems() {
    return items;
  }

  public int getPage() {
    return page;
  }

  public int getSize() {
    return size;
  }

  public long getTotalItems() {
    return totalItems;
  }

  public int getTotalPages() {
    return totalPages;
  }
}
