package com.example.familymanage;

import com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceAutoConfigure;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = DruidDataSourceAutoConfigure.class)
@MapperScan("com.example.familymanage.mapper")
public class FamilyManageApplication {

    public static void main(String[] args) {
        SpringApplication.run(FamilyManageApplication.class, args);
    }
}
