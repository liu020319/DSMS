package com.medicine.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MapperRegistrationTest {

    @Test
    void everyMapperInterfaceMustBeRegisteredWithMapperAnnotation() throws Exception {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        CachingMetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory(resolver);
        Resource[] resources = resolver.getResources("classpath*:com/medicine/mapper/*Mapper.class");
        List<String> missingAnnotations = new ArrayList<>();

        for (Resource resource : resources) {
            MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(resource);
            String className = metadataReader.getClassMetadata().getClassName();
            Class<?> mapperType = Class.forName(className);
            if (mapperType.isInterface() && !mapperType.isAnnotationPresent(Mapper.class)) {
                missingAnnotations.add(className);
            }
        }

        assertTrue(resources.length > 0, "没有扫描到任何 Mapper 接口");
        assertTrue(missingAnnotations.isEmpty(),
                "下列 Mapper 未添加 @Mapper，Spring 启动时无法注入: " + missingAnnotations);
    }
}
