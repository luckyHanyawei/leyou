package com.leyou.item.service;

import com.leyou.common.exception.LyException;
import com.leyou.item.mapper.SpecGroupMapper;
import com.leyou.item.mapper.SpecParamMapper;
import com.leyou.item.pojo.SpecGroup;
import com.leyou.item.pojo.SpecParam;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: HuYi.Zhang
 * @create: 2018-08-19 15:53
 **/
@Service
public class SpecificationService {

    @Autowired
    private SpecGroupMapper groupMapper;

    @Autowired
    private SpecParamMapper paramMapper;

    public List<SpecGroup> queryGroupByCid(Long cid) {
        SpecGroup s = new SpecGroup();
        s.setCid(cid);
        List<SpecGroup> list = groupMapper.select(s);
        if (CollectionUtils.isEmpty(list)) {
            throw new LyException(HttpStatus.NO_CONTENT, "该分类下没有规格组");
        }
        return list;
    }

    public List<SpecParam> queryParams(Long gid, Long cid, Boolean generic, Boolean searching) {
        SpecParam s = new SpecParam();
        s.setGroupId(gid);
        s.setCid(cid);
        s.setGeneric(generic);
        s.setSearching(searching);
        List<SpecParam> list = paramMapper.select(s);
        if (CollectionUtils.isEmpty(list)) {
            throw new LyException(HttpStatus.NO_CONTENT, "该组下没有规格参数");
        }
        return list;
    }

    public List<SpecGroup> querySpecsByCid(Long cid) {
        // 查询组
        SpecGroup t = new SpecGroup();
        t.setCid(cid);
        List<SpecGroup> groups = groupMapper.select(t);

        // 查询当前分类下的所有参数
        List<SpecParam> params = queryParams(null, cid, null, null);

        // 组装一个map， key是group的Id， 值是当前组下的所有param集合
        Map<Long, List<SpecParam>> map = new HashMap<>();
        for (SpecParam param : params) {
            // 判断参数的组是否存在
            if(!map.containsKey(param.getGroupId())){
                // 不存在，则新建一个集合
                map.put(param.getGroupId(), new ArrayList<>());
            }
            map.get(param.getGroupId()).add(param);
        }

        // 填充param
        for (SpecGroup group : groups) {
            group.setParams(map.get(group.getId()));
        }
        return groups;
    }
}
