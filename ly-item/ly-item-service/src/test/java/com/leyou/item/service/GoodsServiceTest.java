package com.leyou.item.service;

import com.leyou.item.dto.CartDTO;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GoodsServiceTest {

    @Autowired
    private GoodsService goodsService;

    @org.junit.Test
    public void decreaseStock() {
        goodsService.decreaseStock(Arrays.asList(new CartDTO(2600242L, 2)));
    }
}