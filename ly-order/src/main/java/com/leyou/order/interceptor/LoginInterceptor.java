package com.leyou.order.interceptor;

import com.leyou.auth.pojo.UserInfo;
import com.leyou.auth.utils.JwtUtils;
import com.leyou.common.utils.CookieUtils;
import com.leyou.order.config.JwtProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
public class LoginInterceptor implements HandlerInterceptor {

    private JwtProperties prop;

    public LoginInterceptor(JwtProperties prop) {
        this.prop = prop;
    }

    private static final ThreadLocal<UserInfo> tl = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 获取token
        String token = CookieUtils.getCookieValue(request, prop.getCookieName());
        // 解析token
        try {
            // 获取用户
            UserInfo user = JwtUtils.getInfoFromToken(token, prop.getPublicKey());
            // 保存用户
            tl.set(user);
            return true;
        } catch (Exception e) {
            // 验证token失败
            log.error("校验登录状态失败！");
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        tl.remove();
    }

    public static UserInfo getUser() {
        return tl.get();
    }
}
