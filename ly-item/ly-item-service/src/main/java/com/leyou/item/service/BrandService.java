package com.leyou.item.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.leyou.common.exception.LyException;
import com.leyou.common.vo.PageResult;
import com.leyou.item.mapper.BrandMapper;
import com.leyou.item.pojo.Brand;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

/**
 * @author: HuYi.Zhang
 * @create: 2018-08-17 15:01
 **/
@Service
public class BrandService {

    @Autowired
    private BrandMapper brandMapper;

    public PageResult<Brand> queryBrandByPage(Integer page, Integer rows, String sortBy, Boolean desc, String key) {
        // 分页
        PageHelper.startPage(page, rows);
        // 过滤
        Example example = new Example(Brand.class);
        if (StringUtils.isNotBlank(key)) {
            example.createCriteria().orLike("name", "%" + key + "%")
                    .orEqualTo("letter", key.toUpperCase());
        }
        // 排序
        if (StringUtils.isNotBlank(sortBy)) {
            example.setOrderByClause(desc ? sortBy + " DESC" : sortBy + " ASC");
        }
        // 查询结果
        List<Brand> brands = brandMapper.selectByExample(example);
        if(CollectionUtils.isEmpty(brands)){
            throw new LyException(HttpStatus.NOT_FOUND, "没有查询到品牌数据");
        }
        // 封装分页结果
        PageInfo<Brand> info = new PageInfo<>(brands);

        return new PageResult<>(info.getTotal(), brands);
    }

    public void saveBrand(Brand brand, List<Long> cids) {
        // 新增品牌
        brand.setId(null);
        brandMapper.insert(brand);
        // 插入中间表
        for (Long cid : cids) {
            brandMapper.saveCategoryBrand(cid, brand.getId());
        }
    }

    public Brand queryById(Long id){
        return brandMapper.selectByPrimaryKey(id);
    }

    public List<Brand> queryBrandByCid(Long cid) {
        return brandMapper.queryBrandByCid(cid);
    }

    public List<Brand> queryBrandByIds(List<Long> ids) {
        List<Brand> list = brandMapper.selectByIdList(ids);
        if(CollectionUtils.isEmpty(list)){
            throw new LyException(HttpStatus.NOT_FOUND, "品牌查询失败！");
        }
        return list;
    }
}
