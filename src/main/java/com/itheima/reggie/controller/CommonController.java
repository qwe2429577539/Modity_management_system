package com.itheima.reggie.controller;

import com.itheima.reggie.common.R;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.CoyoteOutputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/common")
public class CommonController {

    @Value("${reggie.path}")
    private String basePath;

    /**
     * 文件上传
     * @param file
     * @return
     */
    @RequestMapping("/upload")
    public R<String> upload(MultipartFile file) {
        //只是临时文件，请求完成后会删除，需要保存操作
        log.info(file.toString());
        //原始文件名
        String fileName = file.getOriginalFilename();
        //后缀
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        //随机名称
        String fileNewName = UUID.randomUUID().toString() + suffix;
        //创建一个目录对象
        File dir = new File("/Users/travis/Desktop/photos");
        if (!dir.exists()) {
            //目录不存在 需要创建
            dir.mkdirs();
        }

        try{
            file.transferTo(new File(basePath + fileNewName));
        }catch (IOException e){
            log.error(e.getMessage());
        }
        return R.success(fileName);
    }

    /**
     * 下载文件
     * @param name
     * @param response
     * @return
     */
    @GetMapping("/download")
    public R<String> download(String name, HttpServletResponse response) {
        try{
            //输入流 读取文件内容
            FileInputStream fileInputStream = new FileInputStream(new File(basePath + name));
            //输出流 将文件写回浏览器
            ServletOutputStream outputStream = response.getOutputStream();
            //设置文件格式
            response.setContentType("image/jpeg");

            //开始读
            int len = 0;
            byte[] bytes = new byte[1024];
            //读到 bytes里
            while ((len = fileInputStream.read(bytes))!= -1){
                //从0 开始写回浏览器 写到len
                outputStream.write(bytes,0,len);
                outputStream.flush();
            }
            //关闭资源
            outputStream.close();
            fileInputStream.close();
         } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return R.success("下载成功");
    }
}
