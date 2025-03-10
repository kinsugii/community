package com.nowcoder.community.dao;

import com.nowcoder.community.entity.DiscussPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DiscussPostMapper {

    //实现动态sql userid不为0则查询对应用户的帖子 为0则查询全部帖子
    //offset每页起始行的行号 limit表示每页最多多少条数据
    List<DiscussPost> selectDiscussPosts(@Param("userId") int userId, @Param("offset") int offset, @Param("limit") int limit);

    //查询表里一共有多少条数据 同样userid=0时查询所有帖子 不为0时查看该用户一共有多少条帖子
    // @Param注解用于给参数取别名
    // 查询时如果只有一个参数,并且在<if>里使用,则必须加别名.
    int selectDiscussPostRows(@Param("userId") int userId);

    //返回增加的行数int
    int insertDiscussPost(DiscussPost discussPost);

    DiscussPost selectDiscussPostById(@Param("id") int id);

}
