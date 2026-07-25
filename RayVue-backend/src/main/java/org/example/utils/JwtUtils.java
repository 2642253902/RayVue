package org.example.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class JwtUtils {

    // JWT 签名的密钥，来自 application.yml 里的 spring.security.jwt.key。
    @Value("${spring.security.jwt.key}")
    private String key;

    // JWT 过期时间，单位是天，来自 application.yml 里的 spring.security.jwt.expireTime。
    @Value("${spring.security.jwt.expireTime}")
    private int expireTime;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // 把 token 放进黑名单里，后续就算 token 没过期，也会被当作无效 token。
    public boolean invalidateJwt(String headerToken) {
        String token = convertToken(headerToken);
        if (token == null) {
            return false;
        } else {
            // HMAC256 是对称签名算法：同一个 key 用来签发，也用来验签。
            Algorithm algorithm = Algorithm.HMAC256(key);
            JWTVerifier jwtVerifier = JWT.require(algorithm).build();
            try {
                DecodedJWT verifiedJwt = jwtVerifier.verify(token);
                String id = verifiedJwt.getId();
                return deleteToken(id, verifiedJwt.getExpiresAt());
            } catch (JWTVerificationException e) {
                return false;
            }
        }
    }

    //  把 token 放进黑名单里，后续就算 token 没过期，也会被当作无效 token。
    private boolean deleteToken(String uuid, Date expiresAt) {
        if (this.isInvalidToken(uuid)) {
            return false;
        } else {
            Date now = new Date();
            long expire = Math.max(expiresAt.getTime() - now.getTime(), 0);
            stringRedisTemplate.opsForValue().set(Const.JWT_BLACK_LIST + uuid, "1", expire, TimeUnit.MILLISECONDS);
            return true;
        }
    }

    // 判断 token 是否在黑名单里，如果在黑名单里，就当作无效 token。
    private boolean isInvalidToken(String uuid) {
        return stringRedisTemplate.hasKey(Const.JWT_BLACK_LIST + uuid);
    }

    /*
     * 校验 token 是否可信。
     *
     * 1. 校验签名是否正确，防止 token 被篡改；
     * 2. 校验 token 是否过期，防止过期 token 被使用；
     * 3. 校验 token 是否在黑名单里，防止已注销的
     */
    public DecodedJWT resolveJwt(String headerToken) {
        String token = convertToken(headerToken);
        if (token == null) {
            return null;
        } else {
            // HMAC256 是对称签名算法：同一个 key 用来签发，也用来验签。
            Algorithm algorithm = Algorithm.HMAC256(key);
            JWTVerifier jwtVerifier = JWT.require(algorithm).build();
            try {
                // verify 会检查 token 是否被篡改；签名不对会抛 JWTVerificationException。
                DecodedJWT verifiedJwt = jwtVerifier.verify(token);
                if (this.isInvalidToken(verifiedJwt.getId())) {
                    return null;
                } else {
                    Date expiresAt = verifiedJwt.getExpiresAt();
                    // 这里再手动判断一次过期时间，过期就当作未登录。
                    return new Date().after(expiresAt) ? null : verifiedJwt;
                }
            } catch (JWTVerificationException e) {
                return null;
            }
        }
    }

    /*
     * 登录成功后签发 JWT。
     *
     * JWT 里存了用户 id、用户名、权限列表，后续请求可以从 token 里还原成 Spring Security 的 UserDetails
     */
    public String creatJwt(UserDetails userDetails, int id, String username) {
        Algorithm algorithm = Algorithm.HMAC256(key);
        Date expireTime = this.expireTime();
        return JWT
                .create()
                .withJWTId(UUID.randomUUID().toString())
                .withClaim("id", id)
                .withClaim("username", username)
                .withClaim("authorities", userDetails.getAuthorities().stream().map(a -> a.getAuthority()).toList())
                .withExpiresAt(expireTime)
                .withIssuedAt(new Date())
                .sign(algorithm);

    }


    // 计算 token 的过期时间，单位是天。
    public Date expireTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, expireTime * 24);
        return calendar.getTime();
    }

    // 把 JWT 里的用户信息还原成 Spring Security 的 UserDetails 对象。
    public UserDetails toUser(DecodedJWT jwt) {
        Map<String, Claim> claims = jwt.getClaims();
        return User.withUsername(claims.get("username").asString())
                // 这里的 password 是无用的，因为 JWT 里没有密码，所以这里随便填一个。
                .password("*****")
                .authorities(claims.get("authorities").asArray(String.class))
                .build();

    }

    // 把 JWT 里的用户 id 还原成 Integer 对象。
    public Integer toId(DecodedJWT jwt) {
        Map<String, Claim> claims = jwt.getClaims();
        return claims.get("id").asInt();
    }


    // 把 Authorization 请求头里的 token 提取出来。
    private String convertToken(String headerToken) {
        // 前端一般需要传：Authorization: Bearer <token>
        if (headerToken != null && headerToken.startsWith("Bearer ")) {
            return headerToken.substring(7);
        } else {
            return null;
        }
    }
}
