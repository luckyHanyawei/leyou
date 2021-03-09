package com.leyou.page.service;

import com.leyou.item.pojo.Brand;
import com.leyou.item.pojo.Category;
import com.leyou.item.pojo.SpecGroup;
import com.leyou.item.pojo.Spu;
import com.leyou.page.client.BrandClient;
import com.leyou.page.client.CategoryClient;
import com.leyou.page.client.GoodsClient;
import com.leyou.page.client.SpecificationClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
public class PageService {

    private static final ExecutorService es = Executors.newFixedThreadPool(20);

    @Autowired
    private BrandClient brandClient;

    @Autowired
    private CategoryClient categoryClient;

    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private SpecificationClient specClient;

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${ly.page.basePath}")
    private String basePath;

    public Map<String, Object> loadData(Long spuId) {
        // 查询spu
        Spu spu = goodsClient.querySpuById(spuId);
        // 查询categories
        List<Category> categories = categoryClient.queryByIds(
                Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()));
        // 查询brand
        Brand brand = brandClient.queryById(spu.getBrandId());
        // 查询specs
        List<SpecGroup> specs = specClient.querySpecs(spu.getCid3());

        // 封装数据
        Map<String, Object> data = new HashMap<>();
        data.put("specs", specs);
        data.put("brand", brand);
        data.put("categories", categories);
        data.put("skus", spu.getSkus());
        data.put("detail", spu.getSpuDetail());

        // 简化spu的数据
        Map<String, String> map = new HashMap<>();
        map.put("title", spu.getTitle());
        map.put("subTitle", spu.getSubTitle());
        data.put("spu", map);
        return data;
    }

    public void createHtml(Long spuId) {
        createHtml(spuId, loadData(spuId));
    }

    public void createHtml(Long spuId, Map<String, Object> data) {
        // 上下文
        Context context = new Context();
        context.setVariables(data);

        // 获取目标文件路径
        File path = getFilePath(spuId);

        // 判断是否存在
        if (path.exists()) {
            path.delete();
        }
        // 准备流
        try (PrintWriter writer = new PrintWriter(path)) {
            // 生成页面
            templateEngine.process("item", context, writer);
        } catch (Exception e) {
            log.error("商品静态页生成失败，spuId: {}", spuId);
            throw new RuntimeException("商品静态页生成失败", e);
        }
    }

    public void asyncCreateHtml(Long spuId, Map<String, Object> data) {
        es.submit(() -> {
            // 生成html
            createHtml(spuId, data);
        });
    }

    private File getFilePath(Long spuId) {
        // 准备目录
        File dir = new File(basePath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        // 准备目标文件
        return new File(dir, spuId + ".html");
    }

    public void deleteHtml(Long spuId) {
        File file = getFilePath(spuId);
        if(file.exists()){
            file.delete();
        }
    }
}
