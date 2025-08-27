package com.maihehe.blogcore._05_entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 专题表
 * </p>
 *
 * @author maihehe
 * @since 2025-05-21
 */
@Getter
@Setter
@TableName("maihehe_topic")
public class Topic implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id",  type = IdType.ASSIGN_UUID)
    private String id;

    private String path;

    private String name;

    private String category;

    private Boolean visible;

//    在执行 更新操作（update） 时，自动为这个字段 update_time 添加 update_time = now() 这样的 SQL 片段
//    这里的 now 是 mysql 的时间
    @TableField(value = "updated_time", update = "now()")
    private LocalDateTime updatedTime;

    @Override
    public String toString() {
        return "Topic{path=" + path + "}";
    }
}
