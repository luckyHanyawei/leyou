package com.leyou.item.api;

import com.leyou.item.pojo.Brand;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * @author: HuYi.Zhang
 * @create: 2018-08-23 16:27
 **/
@RequestMapping("brand")
public interface BrandApi {
    @GetMapping("{id}")
    Brand queryById(@PathVariable("id")Long id);

    @GetMapping("list")
    List<Brand> queryByIds(@RequestParam("ids") List<Long> ids);
}
