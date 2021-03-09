package com.leyou.common.mapper;

import tk.mybatis.mapper.additional.idlist.DeleteByIdListMapper;
import tk.mybatis.mapper.additional.idlist.IdListMapper;
import tk.mybatis.mapper.additional.idlist.SelectByIdListMapper;
import tk.mybatis.mapper.additional.insert.InsertListMapper;
import tk.mybatis.mapper.common.Mapper;

/**
 * @author: HuYi.Zhang
 * @create: 2018-08-20 16:55
 **/
public interface BaseMapper<T,PK> extends Mapper<T>, IdListMapper<T, PK>, InsertListMapper<T> {
}
