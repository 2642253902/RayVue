package org.example.utils;

import org.springframework.cglib.core.Block;

public class Const {
    // Redis 里存放 JWT 黑名单的 key 前缀，后面会拼接 token 的 jti。
    public static final String JWT_BLACK_LIST = "jwt:blacklist:";

    public static final int ORDER_CORS =-102;

}
