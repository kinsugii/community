package com.nowcoder.community.service;

import com.mysql.cj.util.StringUtils;
import com.nowcoder.community.dao.LoginTicketMapper;
import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.entity.LoginTicket;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.MailClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class UserService implements CommunityConstant {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MailClient mailClient;//邮件客户端

    @Autowired
    private TemplateEngine templateEngine;//注入模版引擎

    @Autowired
    private LoginTicketMapper loginTicketMapper;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    //根据id查询用户
    public User findUserById(int id) {
        return userMapper.selectById(id);
    }

    public Map<String, Object> register(User user) {
        Map<String, Object> map = new HashMap<>();

        //空值处理
        if(user == null){
            throw new IllegalArgumentException("参数不能为空");
        }
        if(StringUtils.isNullOrEmpty(user.getUsername())){
            map.put("usernameMsg","账号不能为空");
            return map;
        }
        if(StringUtils.isNullOrEmpty(user.getPassword())){
            map.put("passwordMsg","密码不能为空");
            return map;
        }
        if(StringUtils.isNullOrEmpty(user.getEmail())){
            map.put("emailMsg","邮箱不能为空");
            return map;
        }

        //验证账号
        User u = userMapper.selectByUsername(user.getUsername());
        if(u != null){
            map.put("usernameMsg","账号已存在");
            return map;
        }
        //验证邮箱
        u = userMapper.selectByEmail(user.getEmail());
        if(u != null){
            map.put("emailMsg","该邮箱已被注册");
            return map;
        }
        //注册用户
        user.setSalt(CommunityUtil.generateUUID().substring(0,5));
        user.setPassword(CommunityUtil.md5(user.getPassword()+user.getSalt()));//密码加密
        user.setType(0);
        user.setStatus(0);
        user.setActivationCode(CommunityUtil.generateUUID());
        user.setHeaderUrl(String.format("http://images.nowcoder.com/head/%dt.png", new Random().nextInt(1000)));
        user.setCreateTime(new Date());
        userMapper.insertUser(user);//将当前用户添加到数据库中

        // 激活邮件
        Context context = new Context();
        context.setVariable("email", user.getEmail());
        // http://localhost:8080/community/activation/101/code
        String url = domain + contextPath + "/activation/" + user.getId() + "/" + user.getActivationCode();
        context.setVariable("url", url);
        String content = templateEngine.process("/mail/activation", context);
        mailClient.sendMail(user.getEmail(), "激活账号", content);
        return map;
    }

    public int activation(int userId, String code) {
        User user = userMapper.selectById(userId);
        if (user.getStatus() == 1) {
            return ACTIVATION_REPEAT;
        } else if (user.getActivationCode().equals(code)) {
            userMapper.updateStatus(userId, 1);
            return ACTIVATION_SUCCESS;
        } else {
            return ACTIVATION_FAILURE;
        }
    }
    //登陆功能，登陆后用户获得凭证，凭证若干秒后会过期
    public Map<String, Object> login(String username, String password, int expiredSeconds) {
        //得到的密码是明文，需要按照和注册时相同的加密规则加密后再和数据库中的数据进行对比
        Map<String, Object> map = new HashMap<>();
        //空值处理
        if(StringUtils.isNullOrEmpty(username) || StringUtils.isNullOrEmpty(password)){
            map.put("usernameMsg","账号不能为空");
            return map;
        }
        if(StringUtils.isNullOrEmpty(password)){
            map.put("passwordMsg","密码不能为空");
            return map;
        }
        User user = userMapper.selectByUsername(username);
        if(user == null){//账号不存在
            map.put("usernameMsg","账号不存在");
            return map;
        }
        if(user.getStatus() == 0){//虽然账号存在，但status=0表示未激活，未激活的账号是不能使用的
            map.put("usernameMsg","账号还未激活");
            return map;
        }
        password = CommunityUtil.md5(password + user.getSalt());//密码加密
        if(!user.getPassword().equals(password)){
            map.put("passwordMsg","密码错误");
            return map;
        }

        //登陆成功，生成登陆凭证
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(user.getId());
        loginTicket.setTicket(CommunityUtil.generateUUID());
        loginTicket.setStatus(0);
        loginTicket.setExpired(new Date(System.currentTimeMillis() + expiredSeconds * 1000));
        loginTicketMapper.insertLoginTicket(loginTicket);

        map.put("ticket", loginTicket.getTicket());//获得登陆凭证
        return map;
    }
    //退出登陆，根据凭证，将对应用户的登录凭证修改为失效状态
    public void logout(String ticket) {
        loginTicketMapper.updateStatus(ticket,1);
    }
}
