package com.leyou.search.client;

import com.leyou.common.vo.PageResult;
import com.leyou.item.pojo.Spu;
import com.leyou.search.pojo.Goods;
import com.leyou.search.repository.GoodsRepository;
import com.leyou.search.service.SearchService;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author: HuYi.Zhang
 * @create: 2018-08-23 16:42
 **/
@RunWith(SpringRunner.class)
@SpringBootTest
public class EsTest {

    @Autowired
    private ElasticsearchTemplate template;

    @Autowired
    private GoodsRepository repository;

    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private SearchService searchService;

    @Test
    public void testCreate(){
        template.createIndex(Goods.class);
        template.putMapping(Goods.class);
    }

    @Test
    public void testLoadData(){
        int page = 1, rows = 100, size = 0;
        do {
            PageResult<Spu> result = goodsClient.querySpuByPage(page, rows, true, null);
            List<Spu> spus = result.getItems();
            if (CollectionUtils.isEmpty(spus)) {
                break;
            }

            List<Goods> goodsList = spus.stream()
                    .map(spu -> searchService.buildGoods(spu)).collect(Collectors.toList());

            repository.saveAll(goodsList);
            size = spus.size();
            page++;
        }while (size == 100);
    }
}
