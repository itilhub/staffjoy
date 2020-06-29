package xyz.staffjoy.mail.service;

import xyz.staffjoy.mail.dto.EmailRequest;

public interface MailSendService {

    void sendMailAsync(EmailRequest req);
}
