package org.example.config;

import jakarta.annotation.Resource;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.entity.RestBean;
import org.example.entity.vo.response.AuthorizeVO;
import org.example.filter.JwtAuthorizeFilter;
import org.example.utils.JwtUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.io.PrintWriter;

@Configuration
public class SecurityConfiguration {

    @Resource
    JwtUtils jwtUtils;

    @Resource
    JwtAuthorizeFilter jwtAuthorizeFilter;

    /*
     * Spring Security 的核心配置。
     *
     * 登录流程里最容易误会的一点：
     * 项目里虽然没有写 /api/auth/login 的 Controller，但 formLogin 会接管这个地址。
     * 前端 POST 用户名和密码到 /api/auth/login 后，Spring Security 会先完成账号密码校验；
     * 校验成功才会走 onAuthenticationSuccess，失败则走 onAuthenticationFailure。
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        return http
                .authorizeHttpRequests(auth -> auth
                        // 认证相关接口放行，否则用户还没登录时连登录接口本身都会被拦住。
                        .requestMatchers("/api/auth/**")
                        .permitAll()
                        // 除 /api/auth/** 之外的接口都要求已经认证。
                        .anyRequest()
                        .authenticated())
                .formLogin(form -> form
                        // 这里声明的是“登录请求处理地址”，不是页面地址，也不是 Controller 地址。
                        .loginProcessingUrl("/api/auth/login")
                        // 登录成功后不跳页面，直接返回 JSON，里面带 JWT 给前端保存。
                        .successHandler((request, response, authentication) ->
                                this.onAuthenticationSuccess(request, response, authentication))
                        // 登录失败时同样返回 JSON，前端可根据 code/message 做提示。
                        .failureHandler((request, response, exception) ->
                                this.onAuthenticationFailure(request, response, exception))

                )
                .logout(logout -> logout
                        // 登出接口也交给 Spring Security 处理。
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler((request, response, authentication) ->
                                this.onLogoutSuccess(request, response, authentication))
                )
                .exceptionHandling(excption -> excption
                        .authenticationEntryPoint((request, response, authException) ->
                                this.commence(request, response, authException))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                this.handle(request, response, accessDeniedException)
                        ))
                // 前后端分离项目一般不用表单 CSRF token，这里关闭后才能直接调登录接口。
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        // 不用服务器 Session 保存登录态；每次请求都靠 JWT 证明身份。
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(jwtAuthorizeFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    // 登录成功时，Spring Security 会把当前用户信息放进 authentication。
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        response.setContentType("application/json;charset=UTF-8");
        User user = (User) authentication.getPrincipal();
        // TODO 这里的 id 和 username 目前写死了；等接数据库用户表后，应改成真实用户信息。
        String token = jwtUtils.creatJwt(user, 1, "小明");
        AuthorizeVO authorizeVO = new AuthorizeVO();
        authorizeVO.setExpireTime(jwtUtils.expireTime());
        authorizeVO.setRole("");
        authorizeVO.setToken(token);
        authorizeVO.setUsername("小明");
        response.getWriter().write(RestBean.success(authorizeVO).asJsonString());
    }

    // JWT 是无状态的，后端没有 Session 可清；真正的登出通常由前端删除本地 token 完成。
    public void onLogoutSuccess(HttpServletRequest request,
                                HttpServletResponse response,
                                @Nullable Authentication authentication) throws IOException, ServletException {
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter writer = response.getWriter();
        String authorization = request.getHeader("Authorization");
        if (jwtUtils.invalidateJwt(authorization)) {
            writer.write(RestBean.success("登出成功").asJsonString());
        } else {
            writer.write(RestBean.failure(400, "登出失败").asJsonString());
        }


    }

    // 访问受保护接口时，如果当前用户没有登录，Spring Security 会走这里。
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(RestBean.unauthorized(authException.getMessage()).asJsonString());
    }

    // 访问受保护接口时，如果当前用户没有权限，Spring Security 会走这里。
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(RestBean.forbidden(accessDeniedException.getMessage()).asJsonString());
    }


    // 登录失败时，Spring Security 会把失败原因放进 exception。
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(RestBean.unauthorized(exception.getMessage()).asJsonString());
    }
}
