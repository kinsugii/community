package com.nowcoder.community.dao;

import com.nowcoder.community.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MessageMapper {

    // 查询当前用户的会话列表,针对每个会话只返回一条最新的私信.
    List<Message> selectConversations(@Param("userId") int userId, @Param("offset") int offset, @Param("limit") int limit);

    // 查询当前用户的会话数量.（查conversationId中包含userid的有多少种）
    int selectConversationCount(@Param("userId") int userId);

    // 查询某个会话所包含的私信列表.（一个conversationId的全部message）
    List<Message> selectLetters(@Param("conversationId") String conversationId, @Param("offset") int offset, @Param("limit") int limit);

    // 查询某个会话所包含的私信数量.（一个conversationId的message数量）
    int selectLetterCount(@Param("conversationId") String conversationId);

    // 查询未读私信的数量（用户的每个会话中接收的status=0的消息数）
    int selectLetterUnreadCount(@Param("userId") int userId, @Param("conversationId") String conversationId);

}
