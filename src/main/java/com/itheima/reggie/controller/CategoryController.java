package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分类管理
 */
@Slf4j
@RestController
@RequestMapping("/category")
public class CategoryController {
    @Autowired
    private CategoryService categoryService;

    /**
     * 新增分类
     * @param category
     * @return
     */
    @PostMapping
    public R<String> save(@RequestBody Category category) {
        categoryService.save(category);
        log.info("category:{}",category);
        return R.success("新增分类成功");
    }

    /**
     * 分页查询
     * @param page
     * @param pageSize
     * @return
     */
   @GetMapping("/page")
   public R<Page> page(int page, int pageSize) {
       //分页构造器
       Page<Category> pageInfo = new Page<>(page,pageSize);
       //条件构造器
       LambdaQueryWrapper<Category> lambdaQueryWrapper = new LambdaQueryWrapper();
       //添加排序条件(根据sort进行排序)
       lambdaQueryWrapper.orderByAsc(Category::getSort);
       //进行分页查询
       categoryService.page(pageInfo,lambdaQueryWrapper);
       return R.success(pageInfo);
   }

    /**
     * 根据id删除分类
     * @param id
     * @return
     */
   @DeleteMapping()
   public R<String> delete(Long id) {
       log.info("删除id为:{}的分类",id);
       //categoryService.removeById(ids);
       //检查分类是否有关联菜品/套餐
       //重写了remove方法
       categoryService.remove(id);
       return R.success("删除分类成功!");
   }

    /**
     * 根据id更新分类
     * @param category
     * @return
     */
   @PutMapping()
   public R<String> update(@RequestBody Category category) {
       categoryService.updateById(category);
       log.info("更新分类的id:{}",category.getId());
       return R.success("更新分类成功");
   }

    /**
     * 根据条件查询分类数据
     * @param category
     * @return
     */
   @GetMapping("/list")
   public R<List<Category>> list(Category category) {
       //条件构造器
       LambdaQueryWrapper<Category> lambdaQueryWrapper = new LambdaQueryWrapper<>();
       //添加条件
       lambdaQueryWrapper.eq(category.getType()!=null,Category::getType,category.getType());
       lambdaQueryWrapper.orderByAsc(Category::getSort).orderByDesc(Category::getUpdateTime);
       List<Category> categories = categoryService.list(lambdaQueryWrapper);
       return R.success(categories);
   }
}
