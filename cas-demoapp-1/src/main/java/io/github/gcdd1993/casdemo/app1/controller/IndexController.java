package io.github.gcdd1993.casdemo.app1.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author gcdd1993
 * @since 2021/12/24
 */
@RestController
@RequestMapping("/")
public class IndexController {

    @GetMapping
    public String index(@AuthenticationPrincipal UserDetails userDetails) {
        return "欢迎您 " + userDetails.getUsername();
    }
}
