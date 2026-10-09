package com.coldstorage.oms;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.coldstorage.oms.mapper")
public class ColdStorageOmsApplication {
    public static void main(String[] args) {
        SpringApplication.run(ColdStorageOmsApplication.class, args);
    }
}
