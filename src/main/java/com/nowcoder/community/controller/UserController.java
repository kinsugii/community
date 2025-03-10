package com.nowcoder.community.controller;

import com.nowcoder.community.annotation.LoginRequired;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

@Controller
@RequestMapping("/user")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Value("${community.path.upload}")
    private String uploadPath;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;

    @LoginRequired
    @RequestMapping(path = "/setting", method = RequestMethod.GET)
    public String getSettingPage() {
        return "/site/setting";
    }//前端调用/user/setting跳转到账号设置页面

    @LoginRequired
    @RequestMapping(path = "/upload", method = RequestMethod.POST)
    public String uploadHeader(MultipartFile headerImage, Model model) {//页面提交一个文件 model模版用来得到数据，向前端返回数据
        if (headerImage == null) {
            model.addAttribute("error", "您还没有选择图片!");
            return "/site/setting";
        }

        String fileName = headerImage.getOriginalFilename();//headerImage是上传的图片文件
        String suffix = fileName.substring(fileName.lastIndexOf("."));//上传的图片文件后缀
        if (StringUtils.isBlank(suffix)) {//防止用户上传无后缀文件
            model.addAttribute("error", "文件的格式不正确!");
            return "/site/setting";
        }

        // 生成随机文件名
        fileName = CommunityUtil.generateUUID() + suffix;//生成新文件名（防止文件重名覆盖）
        // 确定文件存放的路径
        File dest = new File(uploadPath + "/" + fileName);
        try {
            // 把文件保存到服务器
            headerImage.transferTo(dest);
        } catch (IOException e) {
            logger.error("上传文件失败: " + e.getMessage());
            throw new RuntimeException("上传文件失败,服务器发生异常!", e);
        }

        // 更新当前用户的头像的路径(web访问路径)
        // http://localhost:8080/community/user/header/xxx.png
        User user = hostHolder.getUser();
        String headerUrl = domain + contextPath + "/user/header/" + fileName;
        userService.updateHeader(user.getId(), headerUrl);

        return "redirect:/index";
    }
    /**
     路径：/header/{fileName}
     方法：GET
     作用：读取服务器存储的头像，并返回给前端
     •	@PathVariable("fileName") String fileName：从 URL 里获取 fileName
     •	例如，用户访问 /uploadPath/xxxx.jpg，fileName="xxxx.jpg"
     •	HttpServletResponse response：用于向前端 返回图片文件

     **/
    @RequestMapping(path = "/header/{fileName}", method = RequestMethod.GET)
    public void getHeader(@PathVariable("fileName") String fileName, HttpServletResponse response) {
        // 服务器存放路径
        fileName = uploadPath + "/" + fileName;
        // 文件后缀
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        // 响应图片
        response.setContentType("image/" + suffix);
        try (
                FileInputStream fis = new FileInputStream(fileName);//读取文件
                OutputStream os = response.getOutputStream();
        ) {
            byte[] buffer = new byte[1024];
            int b = 0;
            while ((b = fis.read(buffer)) != -1) {
                os.write(buffer, 0, b);
            }
        } catch (IOException e) {
            logger.error("读取头像失败: " + e.getMessage());
        }
    }

    @LoginRequired
    @RequestMapping(path = "/uploadPassword", method = RequestMethod.POST)
    public String uploadPassword(String oldPassword, String newPassword, String confirmPassword,Model model) {
        User user = hostHolder.getUser();
        //检查原密码是否正确
        oldPassword = CommunityUtil.md5(oldPassword + user.getSalt());
        if(StringUtils.isBlank(oldPassword) || !oldPassword.equals(user.getPassword())){
            model.addAttribute("passwordMsg", "原密码错误");
            return "/site/setting";
        }
        //检查新密码是否合法
        if(StringUtils.isBlank(newPassword)){
            model.addAttribute("newPasswordMsg", "新密码不能为空");
            return "/site/setting";
        }
//        System.out.println(newPassword);
//        System.out.println(confirmPassword);
        if(StringUtils.isBlank(confirmPassword) || !confirmPassword.equals(newPassword)){
            model.addAttribute("confirmPasswordMsg", "两次输入的密码不一致");
            return "/site/setting";
        }
//        System.out.println(newPassword);
//        System.out.println(confirmPassword);
        //更新当前用户的密码
        newPassword = CommunityUtil.md5(newPassword+user.getSalt());
        userService.updatePassword(user.getId(), newPassword);
        return "redirect:/logout";

    }



}
