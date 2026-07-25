package org.example.entity;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;

// 统一接口响应结构：所有接口都尽量返回 code/data/message，前端处理起来更稳定。
public record RestBean<T>(int code, T data, String message) {

    // 登录成功时会走这里，把 AuthorizeVO 包成 JSON 响应。
    public static <T> RestBean<T> success(T data) {
        return new RestBean<>(200, data, "登录成功");
    }

    // 登录成功但没有数据时也会走这里，data 为 null。
    public static <T> RestBean<T> success() {
        return success(null);
    }

    // 401 Unauthorized：用户未登录，或者 token 无效/过期。
    public static <T> RestBean<T> unauthorized(String message) {
        return failure(401, message);
    }

    // 403 Forbidden：用户已登录，但没有权限访问某个接口。
    public static <T> RestBean<T> forbidden(String message) {
        return failure(403, message);
    }

    // 登录失败或其他错误时，用 code/message 告诉前端失败原因。
    public static <T> RestBean<T> failure(int code, String message) {
        return new RestBean<>(code, null, message);
    }

    // fastjson2 序列化成 JSON 字符串；WriteNulls 会保留 null 字段。
    public String asJsonString() {
        return JSONObject.toJSONString(this, JSONWriter.Feature.WriteNulls);
    }

}
