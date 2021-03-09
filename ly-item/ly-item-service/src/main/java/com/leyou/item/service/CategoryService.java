package com.leyou.item.service;

import com.leyou.common.exception.LyException;
import com.leyou.item.mapper.CategoryMapper;
import com.leyou.item.pojo.Category;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author: HuYi.Zhang
 * @create: 2018-08-16 16:42
 **/
@Service
public class CategoryService {
    @Autowired
    private CategoryMapper categoryMapper;

    public List<Category> queryCategoryByPid(Long pid) {
        // 查询条件
        Category c = new Category();
        c.setParentId(pid);
        // 查询结果
        List<Category> list = categoryMapper.select(c);
        // 判断
        if(CollectionUtils.isEmpty(list)){
            throw new LyException(HttpStatus.NOT_FOUND, "没有查询到该分类下的子分类！");
        }
        return list;
    }

    public List<Category> queryCategoryByIds(List<Long> ids){
        return categoryMapper.selectByIdList(ids);
    }
}
