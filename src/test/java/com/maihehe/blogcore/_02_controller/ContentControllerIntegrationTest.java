package com.maihehe.blogcore._02_controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.maihehe.blogcore._04_mapper.NoteMapper;
import com.maihehe.blogcore._04_mapper.TopicMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:blog;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:sql/schema.sql",
        "spring.cache.type=none",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
                "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration",
        "spring.profiles.active=test",
        "spring.config.import=optional:file:./config/ContentScan/contentScan.yml"
})
@AutoConfigureMockMvc(addFilters = false)
class ContentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NoteMapper noteMapper;

    @Autowired
    private TopicMapper topicMapper;

    @Test
    void scanToUpdate_executesAndPersists() throws Exception {
        mockMvc.perform(post("/api/admin/content/scan"))
                .andExpect(status().isOk())
                .andExpect(content().string("内容扫描完成！"));

        Long topicCount = topicMapper.selectCount(new QueryWrapper<>());
        Long noteCount = noteMapper.selectCount(new QueryWrapper<>());

        assertTrue(topicCount != null && topicCount > 0, "Topic 应该被写入");
        assertTrue(noteCount != null && noteCount > 0, "Note 应该被写入");
    }
}
