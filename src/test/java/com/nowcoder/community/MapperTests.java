package com.nowcoder.community;

import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.util.Date;
import java.util.List;

@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class MapperTests {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Test
    public void testSelectUser() {
        User user = userMapper.selectById(103);
        System.out.println(user);

        user = userMapper.selectByUsername("zhangfei");
        System.out.println(user);

        user = userMapper.selectByEmail("nowcoder103@sina.com");
        System.out.println(user);
    }
    @Test
    public void testInsertUser() {
        User user = new User();
        user.setUsername("test");
        user.setPassword("123456");
        user.setSalt("abc");
        user.setEmail("abc@sina.com");
        user.setHeaderUrl("http://www.nowcoder.com/101.png");
        user.setCreateTime(new Date());

        int rows = userMapper.insertUser(user);
        System.out.println(rows);
        System.out.println(user.getId());

    }
    @Test
    public void testUpdateUser() {
        int rows = userMapper.updateStatus(150,1);
        System.out.println(rows);

        rows = userMapper.updatePassword(150,"hello");
        System.out.println(rows);

    }
    @Test
    public void testSelectDiscussPost() {
        List<DiscussPost> list= discussPostMapper.selectDiscussPosts(149,0,10);
        for(DiscussPost discussPost:list){
            System.out.println(discussPost);
        }
        int rows = discussPostMapper.selectDiscussPostRows(149);
        System.out.println(rows);
    }
}


