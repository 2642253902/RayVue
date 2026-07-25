package org.example.filter;

import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.utils.JwtUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthorizeFilter extends OncePerRequestFilter {


    @Resource
    JwtUtils jwtUtils;


    /*
     * 每个请求都会经过这个过滤器一次。
     *
     * 它不负责“账号密码登录”，只负责“用户已经拿到 JWT 后，后续请求如何恢复登录态”：
     * 1. 从 Authorization 请求头里取 token；
     * 2. 校验 token 签名和过期时间；
     * 3. 校验通过后，把用户信息放进 Spring SecurityContext；
     * 4. 后面的接口就会被认为是已登录用户在访问。
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 前端一般需要传：Authorization: Bearer <token>
        String authorizationHeader = request.getHeader("Authorization");
        DecodedJWT jwt = jwtUtils.resolveJwt(authorizationHeader);
        if (jwt != null) {
            // 把 JWT 里的 username/authorities 还原成 Spring Security 能识别的用户对象。
            UserDetails user = jwtUtils.toUser(jwt);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
            // 补充本次请求的 IP、SessionId 等 Web 细节，方便 Spring Security 后续使用。
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            // 这一步最关键：把认证结果放进上下文，本次请求后续链路就能拿到“当前登录用户”。
            SecurityContextHolder.getContext().setAuthentication(authentication);
            // 业务接口如果需要用户 id，可以从 request.getAttribute("id") 取。
            request.setAttribute("id", jwtUtils.toId(jwt));
        }
        // 无论有没有 token，都继续往后走；如果目标接口要求认证，Spring Security 会在后面拦截未登录请求。
        filterChain.doFilter(request, response);
    }
}
