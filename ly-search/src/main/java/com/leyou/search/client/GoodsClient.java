package com.leyou.search.client;

import com.leyou.item.api.GoodsApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @author: HuYi.Zhang
 * @create: 2018-08-23 16:17
 **/
@FeignClient("item-service")
public interface GoodsClient extends GoodsApi {
}
