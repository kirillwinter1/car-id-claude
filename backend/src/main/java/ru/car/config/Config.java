package ru.car.config;

import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.config.MeterFilterReply;
import jakarta.annotation.PostConstruct;
import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import ru.car.filter.RequestURIOverriderServletFilter;

import javax.sql.DataSource;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@EnableEncryptableProperties
//@PropertySource(name="EncryptedProperties", value = "classpath:application.yml")
@EnableScheduling
@Configuration
public class Config {

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate(DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }

    @Bean
    public RequestURIOverriderServletFilter requestURIOverriderServletFilter() {
        return new RequestURIOverriderServletFilter();
    }

    @Bean(name = "pushExecutorService")
    public ExecutorService executorService() {
        return Executors.newFixedThreadPool(8, new ThreadFactory() {
            final AtomicInteger i = new AtomicInteger(1);
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setDaemon(false);
                thread.setName("dima_thread_" + i.getAndIncrement());
                return thread;
            }
        });
    }

//    @Bean
//    public Counter counter() {
//        return Counter.builder("register.user").
//    }

    @Autowired
    MeterRegistry registry;

    @PostConstruct
    public void init() {
//        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
//        registry.config().commonTags("application", "app");
        MeterFilter jdbcConnections = new MeterFilter() {
            @Override
            public MeterFilterReply accept(Meter.Id id) {
                if (id.getName().startsWith("car.")) {
                    return MeterFilterReply.NEUTRAL;
                }
                return MeterFilterReply.DENY;
            }
        };

        registry.config().meterFilter(jdbcConnections);
//        return registry;
    }

//    @Bean
//    public MeterRegistryCustomizer<MeterRegistry> configurer() {
//        return (registry) -> registry.config().namingConvention(new NamingConvention() {
//            @Override
//            public String name(String name, Meter.Type type, String baseUnit) {
//                if (name.length() >= 32) {
//                    return name.substring(0, 31);
//                }
//                return name;
//            }
//        });
//    }

    @Bean("jasyptStringEncryptor")
    public StringEncryptor stringEncryptor(@Value("${jasypt.encryptor.myPassword}") String password) {
        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();
        config.setPassword(password);
        config.setAlgorithm("PBEWITHHMACSHA512ANDAES_256");
        config.setKeyObtentionIterations("1000");
        config.setPoolSize(1);
        config.setProviderName("SunJCE");
        config.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator");
        config.setIvGeneratorClassName("org.jasypt.iv.RandomIvGenerator");
        config.setStringOutputType("base64");
        encryptor.setConfig(config);
        return encryptor;
    }
}
