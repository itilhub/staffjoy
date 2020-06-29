package xyz.staffjoy.mail.config;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import xyz.staffjoy.common.config.StaffjoyRestConfig;
import xyz.staffjoy.mail.MailConstant;
import xyz.staffjoy.mail.props.AppProps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@Import(value = StaffjoyRestConfig.class)
@SuppressWarnings(value = "Duplicates")
public class AppConfig {

    public static final String ASYNC_EXECUTOR_NAME = "asyncExecutor";

    @Autowired
    AppProps appProps;

    @ConditionalOnProperty(value = {"staffjoy.aliyun-enable"}, havingValue = "true")
    @Bean
    public IAcsClient acsClient() {
        IClientProfile profile = DefaultProfile.getProfile(MailConstant.ALIYUN_REGION_ID,
                appProps.getAliyunAccessKey(), appProps.getAliyunAccessSecret());
        IAcsClient client = new DefaultAcsClient(profile);
        return client;
    }

    @ConditionalOnProperty(value = {"staffjoy.mail-enable"}, havingValue = "true")
    @Bean
    public JavaMailSender javaMailSender(){
        JavaMailSenderImpl javaMailSenderImpl = new JavaMailSenderImpl();
        javaMailSenderImpl.setHost(appProps.getMailHost());
        if (null != appProps.getMailPort()){
            javaMailSenderImpl.setPort(appProps.getMailPort());
        }
        javaMailSenderImpl.setProtocol(appProps.getMailProtocol());
        javaMailSenderImpl.setUsername(appProps.getMailUsername());
        javaMailSenderImpl.setPassword(appProps.getMailPassword());
        return javaMailSenderImpl;
    }

    @Bean(name=ASYNC_EXECUTOR_NAME)
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setThreadNamePrefix("AsyncThread-");
        executor.initialize();
        return executor;
    }
}
