package xyz.staffjoy.common.config;

import com.github.structlog4j.StructLog4J;
import com.github.structlog4j.json.JsonFormatter;
import feign.RequestInterceptor;
import io.sentry.Sentry;
import io.sentry.SentryClient;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import xyz.staffjoy.common.auth.AuthorizeInterceptor;
import xyz.staffjoy.common.auth.FeignRequestHeaderInterceptor;
import xyz.staffjoy.common.env.EnvConfig;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * Staffjoy 项目配置类
 * 用于配置一些项目所需要的Bean
 */
@Configuration
@EnableConfigurationProperties(StaffjoyProps.class)
public class StaffjoyConfig implements WebMvcConfigurer {

    @Value("${spring.profiles.active:NA}")
    private String activeProfile;

    @Value("${spring.application.name:NA}")
    private String appName;

    @Autowired
    StaffjoyProps staffjoyProps;

    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }

    @Bean
    public EnvConfig envConfig() {
        return EnvConfig.getEnvConfg(activeProfile);
    }

    /**
     * 异常监控 Sentry 客户端初始化配置
     * @return
     */
    @Bean
    public SentryClient sentryClient() {

        SentryClient sentryClient = Sentry.init(staffjoyProps.getSentryDsn());
        // 配置公共字段
        sentryClient.setEnvironment(activeProfile);
        sentryClient.setRelease(staffjoyProps.getDeployEnv());
        sentryClient.addTag("service", appName);

        return sentryClient;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuthorizeInterceptor());
    }

    @Bean
    public RequestInterceptor feignRequestInterceptor() {
        return new FeignRequestHeaderInterceptor();
    }

    @PostConstruct
    public void init() {
        // init structured logging
        // 初始化日志格式 设置日志格式为JSON
        StructLog4J.setFormatter(JsonFormatter.getInstance());

        // global log fields setting
        // 全局日志字段设置
        StructLog4J.setMandatoryContextSupplier(() -> new Object[]{
                "env", activeProfile,
                "service", appName});
    }

    @PreDestroy
    public void destroy() {
        sentryClient().closeConnection();
    }
}
