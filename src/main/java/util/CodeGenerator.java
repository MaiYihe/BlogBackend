package util;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.rules.DbColumnType;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Types;
import java.util.Collections;

@Transactional
class CodeGenerator{
    public static void main(String[] args) {
        FastAutoGenerator.create("jdbc:mysql://localhost:3306/blog?useSSL=false&useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC", "root", "root")
                .globalConfig(builder -> {
                    builder.author("maihehe") // 设置作者
                            .enableSwagger() // 开启 swagger 模式
                            .outputDir("src/main/java/"); // 指定输出目录
                })
                .dataSourceConfig(builder ->
                        builder.typeConvertHandler((globalConfig, typeRegistry, metaInfo) -> {
                            int typeCode = metaInfo.getJdbcType().TYPE_CODE;
                            if (typeCode == Types.SMALLINT) {
                                // 自定义类型转换
                                return DbColumnType.INTEGER;
                            }
                            return typeRegistry.getColumnType(metaInfo);
                        })
                )
                .packageConfig(builder ->
                        builder.parent("com.maihehe.blogcore") // 设置父包名
                                .moduleName("") // 设置父包模块名
                                .pathInfo(Collections.singletonMap(OutputFile.xml, "src/main/resources/mapper")) // 设置mapperXml生成路径
                                .entity("entity")
                                .mapper("mapper")
                                .service("service")
                                .serviceImpl("service.impl")
                                .xml("mapper.xml")
                )
                .strategyConfig(builder ->
                                builder.addInclude("maihehe_topic") // 设置需要生成的表名 "ms_article" "ms_tag"
                                        .addTablePrefix("maihehe_") // 设置过滤表前缀
//                                .entityBuilder().enableLombok().enableFileOverride() // 开启文件覆盖 ，开启 Lombok
//                                .mapperBuilder().enableFileOverride() // 开启文件覆盖
//                                .serviceBuilder().enableFileOverride() // 开启文件覆盖
//                                .controllerBuilder().enableFileOverride() // 开启文件覆盖
                )
                .templateEngine(new FreemarkerTemplateEngine()) // 使用Freemarker引擎模板，默认的是Velocity引擎模板
                .execute();
    }
}

