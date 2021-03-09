package com.leyou.cart.service;

import com.leyou.auth.pojo.UserInfo;
import com.leyou.cart.interceptor.LoginInterceptor;
import com.leyou.cart.pojo.Cart;
import com.leyou.common.exception.LyException;
import com.leyou.common.utils.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CartService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "ly:cart:uid:";

    public void addCart(Cart cart) {
        Integer num = cart.getNum();
        // 获取用户
        UserInfo user = LoginInterceptor.getUser();
        String key = KEY_PREFIX + user.getId();
        // hash操作对象
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(key);
        // 判断当前商品在购物车中是否存在
        String hashKey = cart.getSkuId().toString();

        if(hashOps.hasKey(hashKey)){
            // 存在，查询修改数量
            String json = hashOps.get(hashKey).toString();
            // 反序列化
            cart = JsonUtils.toBean(json, Cart.class);
            // 修改数量
            cart.setNum(cart.getNum() + num);
        }
        // 不存在，直接写入
        hashOps.put(hashKey, JsonUtils.toString(cart));
    }

    public List<Cart> queryCartList() {
        // 获取用户
        UserInfo user = LoginInterceptor.getUser();
        String key = KEY_PREFIX + user.getId();
        // 判断是否存在
        if (!redisTemplate.hasKey(key)) {
            throw new LyException(HttpStatus.NOT_FOUND, "购物车数据为空");
        }
        // 查询购物车
        return redisTemplate.boundHashOps(key).values().stream()
                .map(o -> JsonUtils.toBean(o.toString(), Cart.class))
                .collect(Collectors.toList());
    }

    public void updateNum(Long id, Integer num) {
        // 获取用户
        UserInfo user = LoginInterceptor.getUser();
        String key = KEY_PREFIX + user.getId();
        // hash操作对象
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(key);

        // 判断是否存在
        if (!hashOps.hasKey(id.toString())) {
            // 不存在
            throw new LyException(HttpStatus.BAD_REQUEST, "购物车商品不存在");
        }

        // 存在，查询出来，修改
        String json = hashOps.get(id.toString()).toString();
        Cart cart = JsonUtils.toBean(json, Cart.class);
        cart.setNum(num);
        // 写到redis
        hashOps.put(id.toString(), JsonUtils.toString(cart));
    }
}
