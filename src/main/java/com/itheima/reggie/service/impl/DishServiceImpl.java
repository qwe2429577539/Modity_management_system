package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.DishFlavor;
import com.itheima.reggie.mapper.DishMapper;
import com.itheima.reggie.service.DishFlavorService;
import com.itheima.reggie.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {
    @Autowired
    private DishFlavorService dishFlavorService;

    /**
     * 新增菜品入dish，同时保存口味数据入dish_flavor
     */
    @Override
    //事务控制 保持数据一致性
    @Transactional
    public void saveWithFlavor(DishDto dishDto) {
        //保存菜品基本信息入dish
        this.save(dishDto);

        Long dishId = dishDto.getId(); //菜品id
        List<DishFlavor> dishFlavors = dishDto.getFlavors(); //菜品口味
        //
        dishFlavors.stream().map((item) -> {
            item.setDishId(dishId);
            return item;
        }).collect(Collectors.toList());
        //保存口味数据入dish_flavor
        dishFlavorService.saveBatch(dishFlavors);
    }

    /**
     * 根据id查询菜品信息与口味信息
     * @param id
     * @return
     */
    @Override
    public DishDto getByIdWithFlavor(Long id) {
        //查询菜品基本信息 from dish
        Dish dish = this.getById(id);
        //对象拷贝，将dish拷贝进dishDto
        DishDto dishDto = new DishDto();
        BeanUtils.copyProperties(dish,dishDto);
        //查询菜品对应口味信息 from dish flavor
        LambdaQueryWrapper<DishFlavor> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(DishFlavor::getDishId,dish.getId());
        List<DishFlavor> dishFlavors = dishFlavorService.list(lambdaQueryWrapper);
        //设置dishDto的flavors
        dishDto.setFlavors(dishFlavors);
        return dishDto;
    }

    /**
     * 更新菜品信息
     * @param dishDto
     */
    @Override
    //开启事务注解，保证数据一致性，因为这里操作了两张表
    @Transactional
    public void updateWithFlavor(DishDto dishDto) {
        Long id = dishDto.getId();
        //更新dish表
        this.updateById(dishDto);

        //删除当前菜品对应口味数据
        LambdaQueryWrapper<DishFlavor> lambdaQueryWrapper = new LambdaQueryWrapper();
        lambdaQueryWrapper.eq(DishFlavor::getDishId,dishDto.getId());
        dishFlavorService.remove(lambdaQueryWrapper);

        //获取新口味数据
        List<DishFlavor> flavors = dishDto.getFlavors();

        //设置dish id
        flavors = flavors.stream().map((item) ->{
            item.setDishId(dishDto.getId());
            return item;
        }).collect(Collectors.toList());
        //问题：这里为什么不用update而是insert
        //回答：这里设计两个口味变成三个口味，update不能完成此需求，只有insert可以，所以需要先全部删了再重新插入

        //插入新菜品口味
        dishFlavorService.saveBatch(flavors);



    }

}
