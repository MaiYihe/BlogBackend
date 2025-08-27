package com.maihehe.blogcore._05_entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 笔记内容表
 * </p>
 *
 * @author maihehe
 * @since 2025-05-21
 */
@Getter
@Setter
@TableName("maihehe_note")
public class Note implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    private String topicPath;

    private String currentPath;

    private String name;

    private Boolean isFolder;

    private Integer viewCount;

    private String parentPath;

    private Boolean visible;

    private LocalDateTime updatedTime;
}
