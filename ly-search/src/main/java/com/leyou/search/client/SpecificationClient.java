package com.leyou.search.client;

import com.leyou.item.api.SpecificationApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @author: HuYi.Zhang
 * @create: 2018-08-23 16:18
 **/
@FeignClient("item-service")
public interface SpecificationClient extends SpecificationApi{
}
