package com.atguigu.gulimall.product.service.impl;

import com.atguigu.gulimall.product.service.CategoryBrandRelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.product.dao.CategoryDao;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;
import org.springframework.transaction.annotation.Transactional;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    @Autowired
    private CategoryBrandRelationService categoryBrandRelationService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }
    @Override
    public List<CategoryEntity> listWithTree() {
        //1.查询出所有的CateGoryEnity信息。可以Autowired一个CategoryDao,也可以直接用baseMapper,都一样的。
        List<CategoryEntity> allCategory = baseMapper.selectList(null);
        //2.找出所有的最上层的一级分类。
        List<CategoryEntity> oneLevelCategories = allCategory.stream().filter(categoryEntity ->                                                                  categoryEntity.getParentCid() == 0                                                                             ).map(categoryEntity -> {
            //3.设置分类的子分类.
            categoryEntity.setChildren(findChildren(categoryEntity,allCategory));
            return categoryEntity;
            //4.设置排序字段
        }).sorted((c1,c2) -> (c1.getSort() == null ? 0 : c1.getSort()) - (c2.getSort() == null ? 0 :c2.getSort()))
                .collect(Collectors.toList());
        return oneLevelCategories;
    }

    //递归查找所有菜单的子菜单。
    private List<CategoryEntity> findChildren(CategoryEntity categoryEntity, List<CategoryEntity> allCategory) {
        List<CategoryEntity> children = allCategory.stream().filter(c -> c.getParentCid().equals(categoryEntity.getCatId()))
                .map(c -> {
                    //1.递归找到子菜单
                    c.setChildren(findChildren(c, allCategory));
                    return c;
                    //2.排序
                }).sorted((c1, c2) ->
                        (c1.getSort() == null ? 0 : c1.getSort()) - (c2.getSort() == null ? 0 : c2.getSort())
                ).collect(Collectors.toList());
        return children;
    }


    @Override
    public void removeMenuByIds(List<Long> asList) {
        //TODO 判斷是否有依赖关系，有的话就不能删除。
        baseMapper.deleteBatchIds(asList);
    }

    @Override
    public Long[] findCatelogPath(Long catelogId) {
        List<Long> list = new ArrayList<>();
        findParentPath(catelogId,list);
        //返回的数据格式应该是 祖父，父亲，自己 这样的
        Collections.reverse(list);
        return list.toArray(new Long[0]);
    }

    /**
     * 级联更新所有关联的数据
     * @param category
     */
    @Transactional
    @Override
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(),category.getName());
    }

    private void findParentPath(Long catelogId, List<Long> list) {
        //1.收集当前节点id
        list.add(catelogId);
        //2.看是否有父节点。
        CategoryEntity categoryEntity = getById(catelogId);
        if (categoryEntity!= null && categoryEntity.getParentCid() != 0) {
            findParentPath(categoryEntity.getParentCid(),list);
        }
    }


}