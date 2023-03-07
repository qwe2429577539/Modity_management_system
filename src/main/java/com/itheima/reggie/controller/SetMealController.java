package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.SetmealDto;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.entity.SetmealDish;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.SetMealDishService;
import com.itheima.reggie.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RestController

@RequestMapping("/setmeal")
/**
 * 套餐管理
 */
public class SetMealController {

    @Autowired
    private SetmealService setmealService;
    @Autowired
    private SetMealDishService setMealDishService;
    @Autowired
    private CategoryService categoryService;
    /**
     * 新增套餐
     * @param setmealDto
     * @return
     */
    @PostMapping
    @Transactional
    //当新增一个套餐时，把所有setmealCache的缓存删了
    @CacheEvict(value = "setmealCache",allEntries = true)
    public R<String> save(@RequestBody SetmealDto setmealDto) {
        setmealService.saveSetWithDish(setmealDto);
        return R.success("新增成功");
    }

    /**
     * 套餐分页查询
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String name) {
        //分页构造器
        Page<Setmeal> setmealPage = new Page<>(page,pageSize);
        Page<SetmealDto> setmealDtoPage = new Page<>(page,pageSize);

        //查询构造器
        LambdaQueryWrapper<Setmeal> setmealLambdaQueryWrapper = new LambdaQueryWrapper<>();
        //添加查询条件
        setmealLambdaQueryWrapper.like(name!=null,Setmeal::getName,name);
        setmealLambdaQueryWrapper.orderByDesc(Setmeal::getUpdateTime);
        //查询
        setmealService.page(setmealPage,setmealLambdaQueryWrapper);

        //对象拷贝
        //不要records的原因是因为泛型不一样
        BeanUtils.copyProperties(setmealPage,setmealDtoPage,"records");
        //所以要重做一个records
        List<Setmeal> setMealList = setmealPage.getRecords();
        List<SetmealDto> setmealDtos = setMealList.stream().map((item) -> {
            //创建新对象
            SetmealDto setmealDto = new SetmealDto();
            //对象拷贝
            BeanUtils.copyProperties(item,setmealDto);
            //获得分类对象
            Category category = categoryService.getById(item.getCategoryId());
            //如果分类对象不为空，赋值
            if (category!=null){
                String categoryName = category.getName();
                setmealDto.setCategoryName(categoryName);
            }
            return setmealDto;
        }).collect(Collectors.toList());
        //设置新records
        setmealDtoPage.setRecords(setmealDtos);
        //返回
        return R.success(setmealDtoPage);
    }

    /**
     * 删除套餐
     * @param ids
     * @return
     */
    @DeleteMapping
    //当删除一个套餐时，把所有setmealCache的缓存删了
    @CacheEvict(value = "setmealCache",allEntries = true)
    public R<String> remove(@RequestParam List<Long> ids) {
        setmealService.deleteWithDish(ids);
        return R.success("删除套餐成功");
    }

    /**
     * 停售
     * @param ids
     * @return
     */
    @PostMapping("/status/0")
    public R<String> stop(@RequestParam List<Long> ids) {
            LambdaQueryWrapper<Setmeal> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.in(Setmeal::getId,ids);
            List<Setmeal> setmeals = setmealService.list(lambdaQueryWrapper);
            List<Setmeal> setmealList = setmeals.stream().map((item)->{
                item.setStatus(0);
                return item;
            }).collect(Collectors.toList());
            setmealService.updateBatchById(setmealList);
            return R.success("停售成功");
    }

    /**
     * 启售
     * @param ids
     * @return
     */
    @PostMapping("/status/1")
    public R<String> open(@RequestParam List<Long> ids) {
        LambdaQueryWrapper<Setmeal> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.in(Setmeal::getId,ids);
        List<Setmeal> setmeals = setmealService.list(lambdaQueryWrapper);
        List<Setmeal> setmealList = setmeals.stream().map((item)->{
            item.setStatus(1);
            return item;
        }).collect(Collectors.toList());
        setmealService.updateBatchById(setmealList);
        return R.success("启售成功");
    }

    /**
     * 根据id 获取对应套餐信息 加对应菜品
     * @param ids
     * @return
     */
    @GetMapping("/{ids}")
    public R<Setmeal> get(@PathVariable Long ids) {
        //页面需要的对象是一个dto，所以我们这里需要重写方法
        SetmealDto setmealDto = setmealService.getByIdWithDish(ids);
        return R.success(setmealDto);
    }

    /**
     * 更新套餐以及菜品数据
     * @param setmealDto
     * @return
     */
    @CacheEvict
    @PutMapping
    //传json格式的数据回来 用request body
    public R<String> update(@RequestBody SetmealDto setmealDto) {
        setmealService.updateWithDishes(setmealDto);
        return R.success("更新套餐成功");
    }

    /**
     * 根据条件查询套餐数据
     * @param setmeal
     * @return
     */
    //开启缓存
    @Cacheable(value = "setmealCache",key="#setmeal.categoryId + '_' +#setmeal.status")
    @GetMapping("/list")
    public R<List<Setmeal>> list(Setmeal setmeal){
        //得到普通套餐数据
        LambdaQueryWrapper<Setmeal> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(setmeal.getCategoryId()!=null,Setmeal::getCategoryId,setmeal.getCategoryId());
        lambdaQueryWrapper.orderByDesc(Setmeal::getUpdateTime);

        List<Setmeal> setmeals = setmealService.list(lambdaQueryWrapper);

        return R.success(setmeals);

//        //构建套餐dto
//        setmeals.stream().map((item)->{
//            SetmealDto setmealDto = new SetmealDto();
//            BeanUtils.copyProperties(item,setmealDto);
//            //设置category name
//            Long categoryId = item.getCategoryId();
//            Category category = categoryService.getById(categoryId);
//            setmealDto.setCategoryName(category.getName());
//            //设置套餐内菜品
//            Long id= item.getId();
//            LambdaQueryWrapper<SetmealDish> lambdaQueryWrapper1 = new LambdaQueryWrapper<>();
//            lambdaQueryWrapper1.eq(SetmealDish::getId,id);
//            List<SetmealDish> dishes = setMealDishService.list(lambdaQueryWrapper1);
//        }).collect(Collectors.toList());
//        return R.success()
    }
}
