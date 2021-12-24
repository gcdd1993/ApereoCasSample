package io.github.gcdd1993.casdemo.app1;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author gcdd1993
 * @since 2021/12/24
 */
@Slf4j
@SpringBootApplication
public class CasDemoApplication {

    public static void main(String[] args) {
        try {
            SpringApplication.run(CasDemoApplication.class, args);
        } catch (Exception e) {
            log.error("app run error.", e);
            System.exit(-1);
        }
    }
}
