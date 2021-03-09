package com.leyou.item.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.leyou.common.exception.LyException;
import com.leyou.common.vo.PageResult;
import com.leyou.item.dto.CartDTO;
import com.leyou.item.mapper.SkuMapper;
import com.leyou.item.mapper.SpuDetailMapper;
import com.leyou.item.mapper.SpuMapper;
import com.leyou.item.mapper.StockMapper;
import com.leyou.item.pojo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author: HuYi.Zhang
 * @create: 2018-08-19 17:53
 **/
@Slf4j
@Service
public class GoodsService {

    @Autowired
    private SpuMapper spuMapper;

    @Autowired
    private SpuDetailMapper detailMapper;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private BrandService brandService;

    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private StockMapper stockMapper;

    @Autowired
    private AmqpTemplate amqpTemplate;

    public PageResult<Spu> querySpuByPage(Integer page, Integer rows, Boolean saleable, String key) {
        // 分页
        PageHelper.startPage(page, Math.min(rows, 200));

        // 过滤
        Example example = new Example(Spu.class);
        Example.Criteria criteria = example.createCriteria();
        // 过滤逻辑删除的商品
        criteria.andEqualTo("valid", true);
        // 条件过滤
        if (StringUtils.isNotBlank(key)) {
            criteria.andLike("title", "%" + key + "%");
        }
        // 是否上架
        if (saleable != null) {
            criteria.andEqualTo("saleable", saleable);
        }

        // 查询结果
        List<Spu> list = spuMapper.selectByExample(example);

        // 对分类名称和品牌名称处理
        handleCategoryAndBrandName(list);

        // 封装分页结果
        PageInfo<Spu> info = new PageInfo<>(list);
        return new PageResult<>(info.getTotal(), list);
    }

    private void handleCategoryAndBrandName(List<Spu> list) {
        for (Spu spu : list) {
            // 根据商品分类id查询分类
            List<Category> categories = categoryService.queryCategoryByIds(
                    Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()));
            if (categories == null) {
                throw new LyException(HttpStatus.INTERNAL_SERVER_ERROR, "商品所属分类不存在");
            }
            List<String> names = categories.stream().map(c -> c.getName()).collect(Collectors.toList());
            spu.setCname(StringUtils.join(names, "/"));
            // 根据品牌id查询品牌
            Brand brand = brandService.queryById(spu.getBrandId());
            if (brand == null) {
                throw new LyException(HttpStatus.INTERNAL_SERVER_ERROR, "商品所属品牌不存在");
            }
            spu.setBname(brand.getName());
        }
    }

    @Transactional
    public void saveGoods(Spu spu) {
        // 新增spu
        spu.setCreateTime(new Date());
        spu.setLastUpdateTime(spu.getCreateTime());
        spu.setValid(true);
        spu.setSaleable(true);
        spu.setId(null);
        spuMapper.insert(spu);

        Long spuId = spu.getId();
        // 新增detail
        spu.getSpuDetail().setSpuId(spuId);
        detailMapper.insert(spu.getSpuDetail());

        // 新增sku和库存
        saveSkuAndStock(spu);

        // 发送消息
        sendMessage("insert", spuId);
    }

    public SpuDetail queryDetailBySpuId(Long spuId) {
        SpuDetail detail = detailMapper.selectByPrimaryKey(spuId);
        if (detail == null) {
            throw new LyException(HttpStatus.NOT_FOUND, "商品详情查询失败");
        }
        return detail;
    }

    public List<Sku> querySkuBySpuId(Long spuId) {
        // 查询sku
        Sku sku = new Sku();
        sku.setSpuId(spuId);
        List<Sku> list = skuMapper.select(sku);

        if (CollectionUtils.isEmpty(list)) {
            throw new LyException(HttpStatus.NOT_FOUND, "商品查询失败");
        }
        // 查询库存
        List<Long> ids = list.stream().map(s -> s.getId()).collect(Collectors.toList());
        List<Stock> stocks = stockMapper.selectByIdList(ids);

        if (CollectionUtils.isEmpty(stocks)) {
            throw new LyException(HttpStatus.INTERNAL_SERVER_ERROR, "商品数据异常");
        }
        // 转换数据
        Map<Long, Integer> stockMap = new HashMap<>();
        for (Stock stock : stocks) {
            stockMap.put(stock.getSkuId(), stock.getStock());
        }
        for (Sku s : list) {
            s.setStock(stockMap.get(s.getId()));
        }
        return list;
    }

    @Transactional
    public void updateGoods(Spu spu) {
        Long spuId = spu.getId();
        if (spuId == null) {
            throw new LyException(HttpStatus.BAD_REQUEST, "商品id不能为空");
        }
        // 查询sku
        Sku sku = new Sku();
        sku.setSpuId(spuId);
        List<Sku> list = skuMapper.select(sku);
        if (CollectionUtils.isNotEmpty(list)) {
            // 删除sku
            skuMapper.delete(sku);
            // 删除stock
            List<Long> ids = list.stream().map(s -> s.getId()).collect(Collectors.toList());
            stockMapper.deleteByIdList(ids);
        }
        // 修改spu
        spu.setLastUpdateTime(new Date());
        spu.setValid(null);
        spu.setCreateTime(null);
        spu.setSaleable(null);
        spuMapper.updateByPrimaryKeySelective(spu);
        // 修改detail
        detailMapper.updateByPrimaryKey(spu.getSpuDetail());
        // 新增sku和stock
        saveSkuAndStock(spu);

        // 发送消息
        sendMessage("update", spuId);
    }

    public void sendMessage(String type, Long spuId) {
        // 发送消息
        try {
            amqpTemplate.convertAndSend("item." + type, spuId);
        } catch (Exception e) {
            log.error("消息发送失败，{}", e.getMessage(), e);
            throw new RuntimeException("消息发送失败", e);
        }
    }

    private void saveSkuAndStock(Spu spu) {
        Long spuId = spu.getId();
        List<Stock> stocks = new ArrayList<>();
        // 新增sku
        for (Sku sku : spu.getSkus()) {
            // 填充sku属性
            sku.setId(null);
            sku.setSpuId(spuId);
            sku.setCreateTime(new Date());
            sku.setLastUpdateTime(sku.getCreateTime());
            skuMapper.insert(sku);

            Stock stock = new Stock();
            stock.setSkuId(sku.getId());
            stock.setStock(sku.getStock());
            stocks.add(stock);
        }

        // 新增库存
        stockMapper.insertList(stocks);
    }

    public Spu querySpuById(Long id) {
        // 查询spu
        Spu spu = spuMapper.selectByPrimaryKey(id);
        if (spu == null) {
            throw new LyException(HttpStatus.NOT_FOUND, "spu查询失败！");
        }
        // 查询detail
        spu.setSpuDetail(queryDetailBySpuId(id));
        // 查询skus
        spu.setSkus(querySkuBySpuId(id));
        return spu;
    }

    public List<Sku> querySkuByIds(List<Long> ids) {
        // 查询sku
        List<Sku> skus = skuMapper.selectByIdList(ids);
        if (CollectionUtils.isEmpty(skus)) {
            throw new LyException(HttpStatus.NOT_FOUND, "商品查询失败！");
        }
        // 查询库存
        List<Stock> stocks = stockMapper.selectByIdList(ids);
        if (CollectionUtils.isEmpty(stocks)) {
            throw new LyException(HttpStatus.NOT_FOUND, "库存查询失败！");
        }
        // 把库存转为map，key是skuId，值是库存
        Map<Long, Integer> stockMap = stocks.stream()
                .collect(Collectors.toMap(s -> s.getSkuId(), s -> s.getStock()));

        // 保存库存到sku
        for (Sku sku : skus) {
            sku.setStock(stockMap.get(sku.getId()));
        }
        return skus;
    }

    @Transactional
    public void decreaseStock(List<CartDTO> carts) {
        for (CartDTO cart : carts) {
            // 减库存
            int count = stockMapper.decreaseStock(cart.getSkuId(), cart.getNum());
            if(count != 1){
                throw new LyException(HttpStatus.INTERNAL_SERVER_ERROR, "库存不足");
            }
        }
    }
}