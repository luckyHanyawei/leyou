package com.leyou.search.repository;

import com.leyou.search.pojo.Goods;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * @author: HuYi.Zhang
 * @create: 2018-08-23 16:48
 **/
public interface GoodsRepository extends ElasticsearchRepository<Goods,Long> {
}
