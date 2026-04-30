package com.xirr.calculator.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class MultiUserAuthenticationTest {

    @Autowired
    private UserDetailsService userDetailsService;

    @Test
    void loadsAdditionalConfiguredUser() {
        var user = userDetailsService.loadUserByUsername("advisor");

        assertThat(user.getUsername()).isEqualTo("advisor");
    }
}
