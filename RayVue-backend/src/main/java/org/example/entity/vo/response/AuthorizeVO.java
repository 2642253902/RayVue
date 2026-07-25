package org.example.entity.vo.response;

import lombok.Data;

import java.util.Date;

@Data
// 登录成功后返回给前端的数据：前端保存 token，并用 expireTime 判断何时需要重新登录。
public class AuthorizeVO {

    // 当前登录用户展示名。
    String username;

    // 当前用户角色；现在还没接真实角色体系，所以 SecurityConfiguration 里暂时传空字符串。
    String role;

    // 后续访问受保护接口时，前端需要把它放到 Authorization 请求头里。
    String token;

    // token 过期时间。
    Date expireTime;

}
