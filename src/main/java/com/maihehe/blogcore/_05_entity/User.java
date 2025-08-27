package com.maihehe.blogcore._05_entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;

import lombok.Data;
/**
 * <p>
 * 
 * </p>
 *
 * @author maihehe
 * @since 2025-05-19
 */


@Data
@TableName("maihehe_user")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private String username;

    private String password;

    /**
     * 昵称，展示用
     */
    private String nickname;

    private String roles;
}
