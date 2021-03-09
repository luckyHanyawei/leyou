package com.leyou.item.api;

import com.leyou.item.pojo.Category;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * @author: HuYi.Zhang
 * @create: 2018-08-23 16:27
 **/
public interface CategoryApi {
    @GetMapping("category/list/ids")
    List<Category> queryByIds(@RequestParam("ids") List<Long> ids);
}
