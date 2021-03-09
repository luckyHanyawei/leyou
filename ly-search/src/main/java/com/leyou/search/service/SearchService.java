package com.leyou.search.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.leyou.common.exception.LyException;
import com.leyou.common.utils.JsonUtils;
import com.leyou.common.utils.NumberUtils;
import com.leyou.common.vo.PageResult;
import com.leyou.item.pojo.*;
import com.leyou.search.client.BrandClient;
import com.leyou.search.client.CategoryClient;
import com.leyou.search.client.GoodsClient;
import com.leyou.search.client.SpecificationClient;
import com.leyou.search.pojo.Goods;
import com.leyou.search.pojo.SearchRequest;
import com.leyou.search.pojo.SearchResult;
import com.leyou.search.repository.GoodsRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.LongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author: HuYi.Zhang
 * @create: 2018-08-23 16:52
 **/
@Service
@Slf4j
public class SearchService {

    @Autowired
    private BrandClient brandClient;

    @Autowired
    private CategoryClient categoryClient;

    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private SpecificationClient specClient;

    @Autowired
    private GoodsRepository repository;

    @Autowired
    private ElasticsearchTemplate template;

    public Goods buildGoods(Spu spu) {
        // 查询分类
        List<String> names = categoryClient.queryByIds(
                Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3())).stream()
                .map(c -> c.getName()).collect(Collectors.toList());
        // 查询品牌
        Brand brand = brandClient.queryById(spu.getBrandId());
        // 搜索过滤字段, 标题、分类名称、品牌名称
        String all = spu.getTitle() + " " + StringUtils.join(names, " ") + " " + brand.getName();

        // 查询sku
        List<Sku> skus = goodsClient.querySkuBySpuId(spu.getId());
        // 价格
        Set<Long> price = new HashSet<>();
        // sku字段
        List<Map<String, Object>> skuList = new ArrayList<>();
        for (Sku sku : skus) {
            price.add(sku.getPrice());
            // 创建map
            Map<String, Object> map = new HashMap<>();
            map.put("id", sku.getId());
            map.put("title", sku.getTitle());
            map.put("image", StringUtils.substringBefore(sku.getImages(), ","));
            map.put("price", sku.getPrice());
            skuList.add(map);
        }

        // 查询规格参数，当前分类下的可以用来搜索的规格参数
        List<SpecParam> params = specClient.queryParams(null, spu.getCid3(), null, true);
        // 查询商品详情
        SpuDetail detail = goodsClient.queryDetailBySpuId(spu.getId());
        // 取出通用参数
        String json = detail.getGenericSpec();
        Map<String, Object> genericSpec = JsonUtils.nativeRead(detail.getGenericSpec(),
                new TypeReference<Map<String, Object>>() {
                });
        // 取出特有规格参数
        Map<String, List<String>> specialSpec = JsonUtils.nativeRead(detail.getSpecialSpec(),
                new TypeReference<Map<String, List<String>>>() {
                });
        // 规格参数map：其key来自于tb_spec_param的规格参数名,其值来自于spuDetail中的specialSpecs和genericSpec
        Map<String, Object> specs = new HashMap<>();
        // 封装规格参数
        for (SpecParam param : params) {
            // 规格参数的名称
            String key = param.getName();
            // 规格参数值
            Object value = null;
            // 判断是否是通用属性
            if (param.getGeneric()) {
                // 通用属性
                value = genericSpec.get(param.getId().toString());
                // 判断是否是数值类型
                if (param.getNumeric()) {
                    // 对value分段
                    value = chooseSegment(value.toString(), param);
                }
            } else {
                // 特有属性
                value = specialSpec.get(param.getId().toString());
            }
            if (value == null) {
                value = "其它";
            }
            specs.put(key, value);
        }

        Goods goods = new Goods();
        goods.setId(spu.getId());
        goods.setCreateTime(spu.getCreateTime());
        goods.setBrandId(spu.getBrandId());
        goods.setCid1(spu.getCid1());
        goods.setCid2(spu.getCid2());
        goods.setCid3(spu.getCid3());
        goods.setSubTitle(spu.getSubTitle());
        goods.setSkus(JsonUtils.toString(skuList));//sku集合的json字符串
        goods.setPrice(price);// 所有sku的价格的集合
        goods.setSpecs(specs);// 当前分类下的可以用来搜索的规格参数
        goods.setAll(all);// 搜索过滤的字段
        return goods;
    }

    private String chooseSegment(String value, SpecParam p) {
        double val = NumberUtils.toDouble(value);
        String result = "其它";
        // 保存数值段
        for (String segment : p.getSegments().split(",")) {
            String[] segs = segment.split("-");
            // 获取数值范围
            double begin = NumberUtils.toDouble(segs[0]);
            double end = Double.MAX_VALUE;
            if (segs.length == 2) {
                end = NumberUtils.toDouble(segs[1]);
            }
            // 判断是否在范围内
            if (val >= begin && val < end) {
                if (segs.length == 1) {
                    result = segs[0] + p.getUnit() + "以上";
                } else if (begin == 0) {
                    result = segs[1] + p.getUnit() + "以下";
                } else {
                    result = segment + p.getUnit();
                }
                break;
            }
        }
        return result;
    }

    public PageResult<Goods> search(SearchRequest request) {
        String key = request.getKey();
        if (StringUtils.isBlank(key)) {
            throw new LyException(HttpStatus.BAD_REQUEST, "查询条件不能为空！");
        }
        // 原生查询构建器
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        // 0、搜索结果字段的过滤
        queryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{"id", "subTitle", "skus"}, null));
        // 1、分页
        int page = request.getPage() - 1;
        int size = request.getSize();
        queryBuilder.withPageable(PageRequest.of(page, size));

        // 2、搜索条件
        QueryBuilder basicQuery = buildBasicQuery(request);
        queryBuilder.withQuery(basicQuery);

        // 3、聚合分类和品牌
        String categoryAggName = "categoryAgg";
        queryBuilder.addAggregation(AggregationBuilders.terms(categoryAggName).field("cid3"));
        String brandAggName = "brandAgg";
        queryBuilder.addAggregation(AggregationBuilders.terms(brandAggName).field("brandId"));

        // 4、查询结果
        AggregatedPage<Goods> pageResult = template.queryForPage(queryBuilder.build(), Goods.class);

        // 5、解析结果
        // 5.1、解析聚合结果
        // 分类聚合结果
        Aggregations aggs = pageResult.getAggregations();
        List<Category> categories = handleCategoryAgg(aggs.get(categoryAggName));
        // 品牌聚合结果
        List<Brand> brands = handleBrandAgg(aggs.get(brandAggName));

        // 5.2、判断是否需要聚合规格参数
        List<Map<String, Object>> specs = null;
        if (categories != null && categories.size() == 1) {
            specs = handleSpecs(categories.get(0).getId(), basicQuery);
        }

        // 5.3、解析分页结果
        long total = pageResult.getTotalElements();
        int totalPages = pageResult.getTotalPages();
        List<Goods> list = pageResult.getContent();

        // 封装并返回
        return new SearchResult(total, Long.valueOf(totalPages), list, categories, brands, specs);
    }

    private QueryBuilder buildBasicQuery(SearchRequest request) {
        // 1、创建布尔查询
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
        // 2、基本搜索
        queryBuilder.must(QueryBuilders.matchQuery("all", request.getKey()));
        // 3、过滤
        Map<String, String> map = request.getFilter();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String key = entry.getKey();
            if (!"cid3".equals(key) && !"brandId".equals(key)) {
                key = "specs." + key + ".keyword";
            }
            String value = entry.getValue();
            // 添加过滤条件
            queryBuilder.filter(QueryBuilders.termQuery(key, value));
        }
        return queryBuilder;
    }

    private List<Map<String, Object>> handleSpecs(Long cid, QueryBuilder basicQuery) {
        List<Map<String, Object>> specs = new ArrayList<>();
        // 1、根据分类id查询可以用来搜索的规格参数
        List<SpecParam> params = specClient.queryParams(null, cid, null, true);
        // 2、对规格参数聚合
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        // 2.1、基本搜索条件
        queryBuilder.withQuery(basicQuery);
        // 减小查询结果量
        queryBuilder.withPageable(PageRequest.of(0, 1));
        // 2.2、聚合
        for (SpecParam param : params) {
            String name = param.getName();
            queryBuilder.addAggregation(AggregationBuilders.terms(name).field("specs." + name + ".keyword"));
        }

        // 3、搜索聚合结果
        AggregatedPage<Goods> aggregatedPage = template.queryForPage(queryBuilder.build(), Goods.class);

        // 4、解析聚合结果
        Aggregations aggs = aggregatedPage.getAggregations();
        for (SpecParam param : params) {
            StringTerms terms = aggs.get(param.getName());

            // 创建结果map
            Map<String, Object> map = new HashMap<>();
            map.put("k", param.getName());
            map.put("options", terms.getBuckets().stream().map(b -> b.getKeyAsString()).collect(Collectors.toList()));
            specs.add(map);
        }
        return specs;
    }

    private List<Brand> handleBrandAgg(LongTerms terms) {
        try {
            // 从桶中获取id
            List<Long> ids = terms.getBuckets().stream()
                    .map(b -> b.getKeyAsNumber().longValue()).collect(Collectors.toList());
            // 查询
            List<Brand> brands = brandClient.queryByIds(ids);
            return brands;
        } catch (Exception e) {
            log.error("解析品牌数据出错", e);
            return null;
        }
    }

    private List<Category> handleCategoryAgg(LongTerms terms) {
        try {
            // 从桶中获取id
            List<Long> ids = terms.getBuckets().stream()
                    .map(b -> b.getKeyAsNumber().longValue()).collect(Collectors.toList());
            // 查询
            List<Category> categories = categoryClient.queryByIds(ids);
            return categories;
        } catch (Exception e) {
            log.error("解析品牌数据出错", e);
            return null;
        }
    }

    public void insertOrUpdate(Long spuId) {
        // 查询spu
        Spu spu = goodsClient.querySpuById(spuId);
        if(spu == null){
            log.error("插入索引库失败，商品不存在， id：{}", spuId);
            throw new RuntimeException("插入索引库失败，商品不存在！");
        }
        // 转为goods
        Goods goods = buildGoods(spu);
        repository.save(goods);
    }

    public void deleteIndex(Long spuId) {
        repository.deleteById(spuId);
    }
}
