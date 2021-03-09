package com.leyou.order.utils;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class PayHelperTest {

    @Autowired
    private PayHelper payHelper;

    @org.junit.Test
    public void createPayUrl() {
        String payUrl = payHelper.createPayUrl(31451361L, 1L, "乐优商城测试！");
        System.out.println("payUrl = " + payUrl);
    }
}