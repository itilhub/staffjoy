package xyz.staffjoy.mail.service;

import com.aliyuncs.IAcsClient;
import com.aliyuncs.dm.model.v20151123.SingleSendMailResponse;
import com.aliyuncs.exceptions.ClientException;
import com.github.structlog4j.ILogger;
import com.github.structlog4j.IToLog;
import com.github.structlog4j.SLoggerFactory;
import io.sentry.SentryClient;
import io.sentry.context.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import xyz.staffjoy.common.env.EnvConfig;
import xyz.staffjoy.common.env.EnvConstant;
import xyz.staffjoy.mail.MailConstant;
import xyz.staffjoy.mail.config.AppConfig;
import xyz.staffjoy.mail.dto.EmailRequest;
import xyz.staffjoy.mail.props.AppProps;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

@ConditionalOnMissingBean(IAcsClient.class)
@Service
public class PersonalMailSendService implements MailSendService {

    private static ILogger logger = SLoggerFactory.getLogger(PersonalMailSendService.class);

    @Autowired
    EnvConfig envConfig;

    @Autowired
    JavaMailSender javaMailSender;

    @Autowired
    SentryClient sentryClient;

    @Autowired
    AppProps appProps;

    @Override
    @Async(AppConfig.ASYNC_EXECUTOR_NAME)
    public void sendMailAsync(EmailRequest req) {
        IToLog logContext = () -> {
            return new Object[] {
                    "subject", req.getSubject(),
                    "to", req.getTo(),
                    "html_body", req.getHtmlBody()
            };
        };

        // In dev and uat - only send emails to @jskillcloud.com
        if (!EnvConstant.ENV_PROD.equals(envConfig.getName())) {
            // prepend env for sanity
            String subject = String.format("[%s] %s", envConfig.getName(), req.getSubject());
            req.setSubject(subject);

            // 开发环境不校验邮箱域名
//            if (!req.getTo().endsWith(MailConstant.STAFFJOY_EMAIL_SUFFIX)) {
//                logger.warn("Intercepted sending due to non-production environment.");
//                return;
//            }
        }


        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);
            helper.setFrom(appProps.getMailUsername());
            helper.setTo(req.getTo());
            helper.setSubject(req.getSubject());
            helper.setText(req.getHtmlBody(), true);
            javaMailSender.send(mimeMessage);
            logger.info("Successfully sent email", logContext);
        } catch (MessagingException ex) {
            Context sentryContext = sentryClient.getContext();
            sentryContext.addTag("subject", req.getSubject());
            sentryContext.addTag("to", req.getTo());
            sentryClient.sendException(ex);
            logger.error("Unable to send email ", ex, logContext);
        }
    }
}
