package com.brluiz.poc.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/hello")
public class HelloController {

    @GetMapping
    public String hello() {
        return "hello world";
    }

   @GetMapping("/pod")
    public String getPodName() {
        String podName = System.getenv("HOSTNAME");
        return podName != null ? podName : "brluiz-poc-deployment-6c79d6f84c";
    }
}