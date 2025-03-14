package com.nowcoder.community.controller;

import com.google.code.kaptcha.Producer;
import com.mysql.cj.util.StringUtils;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.imageio.ImageIO;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;


@Controller
public class LoginController implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private Producer kaptchaProducer;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @RequestMapping(path = "/register", method = RequestMethod.GET)
    public String getRegisterPage() {
        return "/site/register";
    }//注册页面

    @RequestMapping(path = "/login", method = RequestMethod.GET)
    public String getLoginPage() {
        return "/site/login";
    }

    @RequestMapping(path = "/register", method = RequestMethod.POST)
    public String register(Model model, User user) {
        Map<String, Object> map = userService.register(user);
        if (map == null || map.isEmpty()) {
            model.addAttribute("msg", "注册成功,我们已经向您的邮箱发送了一封激活邮件,请尽快激活!");
            model.addAttribute("target", "/index");
            return "/site/operate-result";
        } else {
            model.addAttribute("usernameMsg", map.get("usernameMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            model.addAttribute("emailMsg", map.get("emailMsg"));
            return "/site/register";//返回注册界面
        }
    }

    // http://localhost:8080/community/activation/101/code
    @RequestMapping(path = "/activation/{userId}/{code}", method = RequestMethod.GET)
    public String activation(Model model, @PathVariable("userId") int userId, @PathVariable("code") String code) {
        int result = userService.activation(userId, code);
        if (result == ACTIVATION_SUCCESS) {
            model.addAttribute("msg", "激活成功,您的账号已经可以正常使用了!");
            model.addAttribute("target", "/login");
        } else if (result == ACTIVATION_REPEAT) {
            model.addAttribute("msg", "无效操作,该账号已经激活过了!");
            model.addAttribute("target", "/index");
        } else {
            model.addAttribute("msg", "激活失败,您提供的激活码不正确!");
            model.addAttribute("target", "/index");
        }
        return "/site/operate-result";
    }

    @RequestMapping(path = "/kaptcha", method = RequestMethod.GET)
    public void getKaptcha(HttpServletResponse response, HttpSession session) {//生成的验证码是敏感数据，使用session
        //生成验证码
        String text = kaptchaProducer.createText();
        BufferedImage image = kaptchaProducer.createImage(text);
        //验证码存入session
        session.setAttribute("kaptcha", text);
        //图片输出给浏览器
        response.setContentType("image/png");
        try {
            OutputStream out = response.getOutputStream();
            ImageIO.write(image,"png",out);
        } catch (IOException e) {
            logger.error("相应验证码失败："+e.getMessage());
        }

    }

    @RequestMapping(path = "/login",method = RequestMethod.POST)
    public String login(String username, String password, String code, boolean rememberme
    , Model model, HttpSession session, HttpServletResponse response) {//获得页面上用户输入的数据
        String kaptcha =  (String) session.getAttribute("kaptcha");//从session中获得验证码
        //equalsIgnoreCase()方法忽略大小写
        if(StringUtils.isNullOrEmpty(kaptcha) || StringUtils.isNullOrEmpty(code) || !kaptcha.equalsIgnoreCase(code)) {
            model.addAttribute("codeMsg","验证码不正确，请重试");
            return "/site/login";
        }

        int expiredSeconds = rememberme ? REMEMBER_EXPIRED_SECONDS : DEFAULT_EXPIRED_SECONDS;
        //检查账号密码
        Map<String, Object> map = userService.login(username, password, expiredSeconds);
        if(map.containsKey("ticket")){//登陆成功，返回首页
            //给客户端发送cookie让客户端存ticket即保存用户登陆凭证信息
            Cookie cookie = new Cookie("ticket", map.get("ticket").toString());//cookie获得登陆凭证
            cookie.setPath(contextPath);//设置cookie的作用路径为/community 即整个项目都有效
            cookie.setMaxAge(expiredSeconds);
            response.addCookie(cookie);//将cookie发送给页面，再相应时就会发送给浏览器
            return "redirect:/index";
        }else {
            //model用于给前端返回错误信息
            model.addAttribute("usernameMsg", map.get("usernameMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            return "/site/login";
        }


    }

    //处理页面的退出登陆请求
    @RequestMapping(path = "logout", method = RequestMethod.GET)
    public String logout(@CookieValue("ticket") String ticket) {
        userService.logout(ticket);
        return "redirect:/login";
    }

}
