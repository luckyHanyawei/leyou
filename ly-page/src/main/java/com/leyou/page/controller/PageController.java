package com.leyou.page.controller;

import com.leyou.page.pojo.User;
import com.leyou.page.service.PageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.FileNotFoundException;
import java.util.Map;

@Controller
public class PageController {

    @Autowired
    private PageService pageService;

    @GetMapping("item/{id}.html")
    public String toItem(Model model, @PathVariable("id") Long spuId){
        // 加载模型数据
        Map<String,Object> data = pageService.loadData(spuId);
        // 添加模型数据
        model.addAllAttributes(data);

        // 创建html
        pageService.asyncCreateHtml(spuId, data);
        return "item";
    }
}
