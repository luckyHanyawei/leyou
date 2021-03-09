package com.leyou.search.client;

import com.leyou.item.api.CategoryApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @author: HuYi.Zhang
 * @create: 2018-08-23 16:09
 **/
@FeignClient("item-service")
public interface CategoryClient extends CategoryApi{
}
