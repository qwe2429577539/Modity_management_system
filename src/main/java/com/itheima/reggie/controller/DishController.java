package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.DishFlavor;
import com.itheima.reggie.mapper.DishFlavorMapper;
import com.itheima.reggie.mapper.DishMapper;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.DishFlavorService;
import com.itheima.reggie.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Slf4j
@RestController
@RequestMapping("/dish")
public class DishController {
    @Autowired
    private DishFlavorService dishFlavorService;
    @Autowired
    private DishService dishService;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 新增菜品
     * @param dishDto
     * @return
     */
    @PostMapping
    public R<String> save(@RequestBody DishDto dishDto) {
        log.info(dishDto.toString());
        dishService.saveWithFlavor(dishDto);

        //从redis里获取所有以dish_开头的key
        //Set keys =redisTemplate.keys("dish_*");
        //清理所有redis缓存
        //redisTemplate.delete(keys);

        //清理某个分类下面的菜品缓存数据
        String key = "dish_" + dishDto.getCategoryId() + "_1";
        //清除缓存
        redisTemplate.delete(key);
        return R.success("新增菜品成功");
    }

    /**
     * 菜品分页查询
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String name) {
        //构造分页构造器
        Page<Dish> pageInfo = new Page<>(page,pageSize);
        Page<DishDto> dishDtoPage = new Page<>();
        //构造条件构造器
        LambdaQueryWrapper<Dish> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        //添加过滤条件(name不为空时，根据name进行模糊查询)
        lambdaQueryWrapper.like(name!=null,Dish::getName,name);
        lambdaQueryWrapper.orderByDesc(Dish::getUpdateTime);
        //通过 分页助手 +两个条件 进行查询
        dishService.page(pageInfo,lambdaQueryWrapper);

        //问题：页面上的数据对象不是Dish
        //解决方案：创造一个对应的数据对象：DishDto（继承Dish）

        //对象拷贝
        //records是所有的dish 详细菜品数据
        BeanUtils.copyProperties(pageInfo,dishDtoPage,"records");
        List<Dish> records = pageInfo.getRecords();

        List<DishDto> list = records.stream().map((item) ->{
            //对于records里的每一种数据，都创建一个dishDto去承接
            DishDto dishDto = new DishDto();
            //对象拷贝，但是categoryName还未填充
            BeanUtils.copyProperties(item,dishDto);
            //根据id查分类对象
            Long categoryId = item.getCategoryId();//分类id
            Category category = categoryService.getById(categoryId);//根据id查分类对象
            if (category != null) {
                String categoryName = category.getName();
                dishDto.setCategoryName(categoryName);
            }
            //返回dishDto
            return dishDto;
            //转换成list
        }).collect(Collectors.toList());
        //dishDto 设置records
        dishDtoPage.setRecords(list);
        //返回成功信息
        return R.success(dishDtoPage);

    }

    /**
     * 根据id查询菜品、口味信息
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public R<DishDto> get(@PathVariable Long id) {
        //问题：因为这里的要体现的数据不是单纯的Dish数据对象 而是dish + dish flavor，也就是dishDto
        //解决方案：所以这里重写一个新方法
        DishDto dishDto = dishService.getByIdWithFlavor(id);
        return R.success(dishDto);
    }

    /**
     * 更新菜品数据
     * @param dishDto
     * @return
     */
    @PutMapping
    public R<String> update(@RequestBody DishDto dishDto) {
        log.info(dishDto.toString());
        dishService.updateWithFlavor(dishDto);

        //从redis里获取所有以dish_开头的key
        //Set keys =redisTemplate.keys("dish_*");
        //redisTemplate.delete(keys);

        //清理某个分类下面的菜品缓存数据
        String key = "dish_" + dishDto.getCategoryId() + "_1";
        //清除缓存
        redisTemplate.delete(key);
        return R.success("更新菜品成功");
    }

//    /**
//     * 根据条件查询对应菜品数据
//     * @param dish
//     * @return
//     */
//    @GetMapping("/list")
//    public R<List<Dish>> list(Dish dish) {
//        LambdaQueryWrapper<Dish> lambdaQueryWrapper = new LambdaQueryWrapper<>();
//        lambdaQueryWrapper.eq(Dish::getStatus,1);
//        lambdaQueryWrapper.eq(dish.getCategoryId()!=null,Dish::getCategoryId,dish.getCategoryId());
//        lambdaQueryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);
//        List<Dish> list = dishService.list(lambdaQueryWrapper);
//        return R.success(list);
//    }

    /**
     * 根据条件查询对应菜品数据 + 口味信息
     * @param dish
     * @return
     */
    @GetMapping("/list")
    public R<List<DishDto>> list(Dish dish) {
        List<DishDto> dishDtoList = null;
        String key = "dish_" + dish.getCategoryId() + "_" + dish.getStatus();

        //先从redis缓存里找数据
        dishDtoList = (List<DishDto>) redisTemplate.opsForValue().get(key);
        //redis缓存里如果存在 返回数据
        if (dishDtoList!=null){
            return R.success(dishDtoList);
        }

        LambdaQueryWrapper<Dish> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        //查询状态为1的菜品
        lambdaQueryWrapper.eq(Dish::getStatus,1);
        //查询categoryid
        lambdaQueryWrapper.eq(dish.getCategoryId()!=null,Dish::getCategoryId,dish.getCategoryId());
        //添加排序条件
        lambdaQueryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);
        List<Dish> list = dishService.list(lambdaQueryWrapper);

        dishDtoList =list.stream().map((item)->{
                DishDto dishDto = new DishDto();
                BeanUtils.copyProperties(item,dishDto);
                //得到category id
                Long categoryId = item.getCategoryId();
                //得到对应的category
                Category category = categoryService.getById(categoryId);
                //设置category name
                if (category!=null){
                    String catogoryName = category.getName();
                    dishDto.setCategoryName(catogoryName);
                }
                //获取dish id
                Long dishId = item.getId();
                LambdaQueryWrapper<DishFlavor> lambdaQueryWrapper1 = new LambdaQueryWrapper<>();
                //查询dish id
                lambdaQueryWrapper1.eq(DishFlavor::getDishId,dishId);
                //获取flavors list
                List<DishFlavor> dishFlavors = dishFlavorService.list(lambdaQueryWrapper1);
                //设置flavors
                dishDto.setFlavors(dishFlavors);
                return dishDto;
                }).collect(Collectors.toList());
        //如果数据在redis不存在 存入redis
        redisTemplate.opsForValue().set(key,dishDtoList,60, TimeUnit.MINUTES);
        //返回结果
        return R.success(dishDtoList);
    }
}
