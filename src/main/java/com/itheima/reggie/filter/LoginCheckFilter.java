package com.itheima.reggie.filter;

import com.alibaba.fastjson.JSON;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 检查用户是否完成登陆
 */
//拦截器 *：拦截所有
@WebFilter(filterName = "loginCheckFilter", urlPatterns = "/*")
//日志
@Slf4j
public class LoginCheckFilter implements Filter {
    //路径匹配器
    public static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest)servletRequest;
        HttpServletResponse response = (HttpServletResponse)servletResponse;
        //1.获取请求url
        String url = request.getRequestURI();
        log.info("拦截到请求:{}",url);
        //定义不需要请求的路径
        String [] urls = new String[]{
                "/employee/login","/employee/logout","/backend/**","/font/**","/user/sendMsg","/user/login",
                "/front/**"
        };
        //2.判断本次请求是否需要处理（检查登陆状态）
        Boolean match = check(urls,url);
        //3.若不需要处理 放行
        if (match){
            log.info("本次请求不需要处理:{}",url);
            filterChain.doFilter(request,response);
            return;
        }
        //4.1 判断员工角色登陆状态，如果已登陆，直接放行
        //创建本地线程储存id值
        BaseContext baseContext = new BaseContext();
        if (request.getSession().getAttribute("employee") != null){
            Long empId = (Long) request.getSession().getAttribute("employee");
            log.info("用户已登陆,用户id为:{}",empId);
            //创建本地线程储存id值
            BaseContext.setCurrentId(empId);
            filterChain.doFilter(request,response);
            return;
        }
        //4.2 判断移动端用户登陆状态，如果已登陆，直接放行
        //创建本地线程储存id值
        if (request.getSession().getAttribute("user") != null){
            Long userId = (Long) request.getSession().getAttribute("user");
            log.info("用户已登陆,用户id为:{}",userId);
            //创建本地线程储存id值
            BaseContext.setCurrentId(userId);
            filterChain.doFilter(request,response);
            return;
        }

        //5.如未登录，返回未登录结果 通过输出流方式返回response数据 返回登陆页面
        log.info("用户未登录");
        response.getWriter().write(JSON.toJSONString(R.error("NOTLOGIN")));
        return;
    }

    /**
     * 路径匹配 检查当前url是否需要放行
     * @param requestURL
     * @return
     */
    public boolean check(String[] urls,String requestURL){
        for (String url: urls) {
            Boolean match =  PATH_MATCHER.match(url,requestURL);
            if (match){
                return true;
            }
        }
        return false;
    }
}
