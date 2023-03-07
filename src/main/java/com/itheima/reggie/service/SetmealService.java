package com.itheima.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.reggie.dto.SetmealDto;
import com.itheima.reggie.entity.Setmeal;

import java.util.List;

public interface SetmealService extends IService<Setmeal> {
    /**
     * 新增套餐，保存套餐+菜品关联数据
     * @param setmealDto
     */
    public void saveSetWithDish(SetmealDto setmealDto);

    /**
     * 删除套餐和对应菜品信息
     * @param ids
     */
    public void deleteWithDish(List<Long>ids);

    /**
     * 获取套餐和对应菜品信息
     * @param id
     * @return
     */
    public SetmealDto getByIdWithDish(Long id);

    /**
     * 更新套餐以及菜品信息
     * @param setmealDto
     */
    public void updateWithDishes(SetmealDto setmealDto);
}
