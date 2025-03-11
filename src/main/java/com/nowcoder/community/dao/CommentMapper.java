package com.nowcoder.community.dao;

import com.nowcoder.community.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CommentMapper {

    //查询评列表论 分页查看
    List<Comment> selectCommentsByEntity(@Param("entityType") int entityType,@Param("entityId") int entityId,
                                         @Param("offset") int offset,@Param("limit") int limit);

    //查询某类型的某id类型实体拥有的帖子的数量
    int selectCountByEntity(@Param("entityType") int entityType,@Param("entityId") int entityId);

    int insertComment(Comment comment);

}
