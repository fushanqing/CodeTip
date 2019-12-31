package top.binggo.codetipcontrolcenter.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author binggo
 */
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {


    /**
     * roles admin allow to access /admin/**
     * roles user allow to access /user/**
     * custom 403 access denied handler
     */
    @Override
    protected void configure(HttpSecurity http) throws Exception {

        http.csrf().disable()
                .authorizeRequests()
                // 满足该条件下的路由需要ROLE_ADMIN的角色
                .antMatchers("/crawler/**").hasAnyRole("ADMIN")
                .anyRequest().authenticated()
                .and()
                .formLogin()
                .successHandler(new CustomAuthenticationSuccessHandler())
                .permitAll()
                .and()
                .logout()
                .permitAll()
                .and()
                //自定义异常处理
                .exceptionHandling().accessDeniedHandler(new CustomAccessDeniedHandler());
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {

        System.out.println("hello world 1");
        auth.inMemoryAuthentication()
                .withUser("binggo").password(passwordEncoder().encode("123!@#qazWSX")).roles("ADMIN");
    }

    @Slf4j
    public static class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

        @Override
        public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
            System.out.println("hello world 2");

            String location = "http://localhost:8080/spiderManagement";
            if (authentication != null) {
                log.info("User '" + authentication.getName()
                        + "login success and will be redirect to " + location);
            }
            response.sendRedirect(location);
        }
    }

    @Slf4j
    public static class CustomAccessDeniedHandler implements AccessDeniedHandler {


        @Override
        public void handle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, AccessDeniedException e) throws IOException, ServletException {
            Authentication auth
                    = SecurityContextHolder.getContext().getAuthentication();
            System.out.println("hello world 3");

            if (auth != null) {
                log.info("User '" + auth.getName()
                        + "' attempted to access the protected URL: "
                        + httpServletRequest.getRequestURI());
            }
            httpServletResponse.sendRedirect("http://www.binggo.online:8083/login");
        }
    }
}
