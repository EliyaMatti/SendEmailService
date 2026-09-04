package com.mailSender.campaign;

import com.mailSender.MailAppProperties;
import com.mailSender.excel.Contact;
import com.mailSender.template.TemplateRenderer;
import java.util.List;

/**
 * Builds an {@link EmailMessage} from a {@link Contact} and template. Rendering only; it does not
 * send mail or use SMTP.
 */
public final class EmailComposer {

  private final MailAppProperties mailAppProperties;

  public EmailComposer(MailAppProperties mailAppProperties) {
    this.mailAppProperties = mailAppProperties;
  }

  public EmailMessage compose(Contact contact, String template) {
    String attachmentPath = mailAppProperties.getAttachmentPath();
    List<String> attachments =
        attachmentPath == null || attachmentPath.isBlank() ? List.of() : List.of(attachmentPath);
    return new EmailMessage(
        contact.getEmail(),
        mailAppProperties.getSubject(),
        TemplateRenderer.render(template, contact),
        mailAppProperties.getFrom(),
        "",
        attachments);
  }
}
