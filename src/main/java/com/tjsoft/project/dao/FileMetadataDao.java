package com.tjsoft.project.dao;

import com.tjsoft.project.entity.FileMetadata;
import org.apache.ibatis.annotations.Param;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 保存在mongo中的文件信息(FileMetadata)表数据库访问层
 *
 * @author sefo
 * @since 2021-08-19 10:53:57
 */
@Mapper
public interface FileMetadataDao extends BaseMapper<FileMetadata>  {

    /**
     * 通过ID查询单条数据
     *
     * @param id 主键
     * @return 实例对象
     */
    FileMetadata queryById(String id);

    /**
     * 查询指定行数据
     *
     * @param offset 查询起始位置
     * @param limit 查询条数
     * @return 对象列表
     */
    List<FileMetadata> queryAllByLimit(@Param("offset") int offset, @Param("limit") int limit);


    /**
     * 通过实体作为筛选条件查询
     *
     * @param fileMetadata 实例对象
     * @return 对象列表
     */
    List<FileMetadata> queryAll(FileMetadata fileMetadata);


    /**
     * 修改数据
     *
     * @param fileMetadata 实例对象
     * @return 影响行数
     */
    int update(FileMetadata fileMetadata);

}