package com.leyou.item.mapper;

import com.leyou.common.mapper.BaseMapper;
import com.leyou.item.pojo.Brand;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @author: HuYi.Zhang
 * @create: 2018-08-17 15:00
 **/
public interface BrandMapper extends BaseMapper<Brand, Long> {

    @Insert("INSERT INTO `tb_category_brand` (`category_id`, `brand_id`) VALUES (#{cid}, #{bid})")
    int saveCategoryBrand(@Param("cid") Long cid, @Param("bid")Long bid);

    @Select("SELECT b.* FROM tb_category_brand cb LEFT JOIN tb_brand b ON cb.brand_id = b.id WHERE cb.category_id = #{cid}")
    List<Brand> queryBrandByCid(@Param("cid") Long cid);
}
