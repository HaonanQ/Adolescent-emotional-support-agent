package com.air.aiagent.mapper;

import com.air.aiagent.domain.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
* @author 30280
* @description 针对表【user(用户表)】的数据库操作Mapper
* @createDate 2025-09-12 12:44:39
* @Entity com.air.aiagent.domain.entity.User
*/
public interface UserMapper extends BaseMapper<User> {
    /**
     * 查询用户列表（忽略逻辑删除）
     */
    @Select("<script>"
            + "SELECT id,username,nickname,password,sex,create_time,update_time,is_deleted,is_admin,avatar "
            + "FROM user "
            + "<where>"
            + "   <if test='isDeleted != null'> AND is_deleted = #{isDeleted} </if>"
            + "   <if test='isDeleted == null'> AND is_deleted = 0 </if>"
            + "   <if test='sex != null'> AND sex = #{sex} </if>"
            + "   <if test='keyword != null and keyword != &quot;&quot;'> "
            + "       AND (username LIKE CONCAT('%',#{keyword},'%') OR nickname LIKE CONCAT('%',#{keyword},'%')) "
            + "   </if>"
            + "</where>"
            + " ORDER BY create_time DESC"
            + "</script>")
    List<User> selectUserList(
            @Param("keyword") String keyword,
            @Param("isDeleted") Integer isDeleted,
            @Param("sex") Integer sex
    );

    /**
     * 根据ID查询用户（忽略逻辑删除）
     */
    @Select("SELECT id,username,nickname,password,sex,create_time,update_time,is_deleted,is_admin,avatar "
            + "FROM user WHERE id = #{userId}")
    User selectUserByIdIgnoreLogicDelete(@Param("userId") Long userId);

    /**
     * 更新用户状态
     */
    @Update("UPDATE user SET is_deleted = #{status}, update_time = NOW() WHERE id = #{userId}")
    int updateUserStatus(@Param("userId") Long userId, @Param("status") Integer status);
}




