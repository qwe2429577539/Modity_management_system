package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.User;
import com.itheima.reggie.service.UserService;
import com.itheima.reggie.utils.ValidateCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Transactional
@RestController
@RequestMapping("/user")
public class userController {

    @Autowired
    private UserService userService;
    @Autowired
    private RedisTemplate redisTemplate;
    /**
     * 发送验证码
     *
     * @param user
     * @param session
     * @return
     */
    @PostMapping("/sendMsg")
    public R<String> sendMsg(@RequestBody User user, HttpSession session) {
        //获取手机号
        String phoneNum = user.getPhone();
        //生成随机验证码
        if (phoneNum!=null) {
            String code = ValidateCodeUtils.generateValidateCode(4).toString();
            log.info("code:{}", code);
            //使用阿里云api发送短信
            //SMSUtils.sendMessage("Alpha","",phone,code);
            //将生存的验证码保存到session，以供验证
            //session.setAttribute(phoneNum, code);
            //将验证码缓存到redis中 设置5分钟超时时间
            redisTemplate.opsForValue().set(phoneNum,code,5, TimeUnit.MINUTES);
            return R.success("验证码发送成功");
        }
        return R.error("验证码发送失败");
    }

    /**
     * 移动端用户登陆
     *
     * @param map
     * @param session
     * @return
     */
    @PostMapping("/login")
    public R<User> login(@RequestBody Map map, HttpSession session) {
        log.info(map.toString());
        //获取手机号
        String phoneNum = map.get("phone").toString();
        //获取验证码
        String code = map.get("code").toString();
        //获取session中已经保存的验证码
        //String trueCode = session.getAttribute(phoneNum).toString();

        //从redis中获取缓存验证码
        String trueCode = (String) redisTemplate.opsForValue().get(phoneNum);
        //做验证码判断
        if (trueCode != null && code.equals(trueCode)) {
            //如果判断成功，登陆成功
            //判断用户是否为新用户，如果是新用户就自动完成注册
            LambdaQueryWrapper<User> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(User::getPhone, phoneNum);
            //用get one 的原因是
            //因为phone是唯一的
            User user = userService.getOne(lambdaQueryWrapper);
            //如果是新用户
            if (user == null) {
            user= new User();
            user.setPhone(phoneNum);
            user.setStatus(1);
            userService.save(user);
            }
            //过滤器用户登录状态 需要校验 所以session需要赋值
            session.setAttribute("user",user.getId());

            //如果用户登陆成功 删除验证码缓存
            redisTemplate.delete(phoneNum);

            return R.success(user);
        }
        //判断失败
        return R.error("登陆失败");
    }
}
