package com.papergen;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.papergen.mapper")
public class PapergenServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(PapergenServerApplication.class, args);
    }

}
