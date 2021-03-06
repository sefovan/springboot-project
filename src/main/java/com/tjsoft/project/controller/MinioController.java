package com.tjsoft.project.controller;

import com.alibaba.fastjson.JSON;
import com.tjsoft.project.response.ResponseValue;
import io.minio.MinioClient;
import io.minio.ObjectStat;
import io.minio.PutObjectOptions;
import io.minio.Result;
import io.minio.messages.Item;
import io.swagger.annotations.Api;
import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * minio相关接口
 * @author sefo
 * @version V1.0
 * @Package com.tjsoft.project.controller
 * @date 2021/10/28 10:10
 * @Copyright ©
 */
@Log4j2
@Api(tags = {"minio操作文件控制器"})
@RestController
@RequestMapping("/minio")
public class MinioController {

    @Autowired
    private MinioClient minioClient;

    private static final String MINIO_BUCKET = "test";

    @GetMapping("/list")
    public List<Object> list(ModelMap map) throws Exception {
        Iterable<Result<Item>> myObjects = minioClient.listObjects(MINIO_BUCKET);
        Iterator<Result<Item>> iterator = myObjects.iterator();
        List<Object> items = new ArrayList<>();
        String format = "{'fileName':'%s','fileSize':'%s'}";
        while (iterator.hasNext()) {
            Item item = iterator.next().get();
            items.add(JSON.parse(String.format(format, item.objectName(), formatFileSize(item.size()))));
        }
        return items;
    }

    @PostMapping("/upload/{bucket}")
    public ResponseValue upload(@RequestParam(name = "file", required = false) MultipartFile[] file, @PathVariable("bucket") String bucket) {

        if (file == null || file.length == 0) {
            return ResponseValue.fail("上传文件不能为空");
        }

        List<String> orgfileNameList = new ArrayList<>(file.length);

        for (MultipartFile multipartFile : file) {
            String orgfileName = multipartFile.getOriginalFilename();
            orgfileNameList.add(orgfileName);

            try {
                //判断minio中的bucket是否存在
                boolean isExist = minioClient.bucketExists(bucket);
                if(isExist) {
                    log.info("Bucket {} already exists.", bucket);
                } else {
                    minioClient.makeBucket(bucket);
                }

                InputStream in = multipartFile.getInputStream();
                minioClient.putObject(bucket, orgfileName, in, new PutObjectOptions(in.available(), -1));
                in.close();
            } catch (Exception e) {
                log.error(e.getMessage());
                return ResponseValue.fail("上传失败");
            }
        }

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("bucketName", MINIO_BUCKET);
        data.put("fileName", orgfileNameList);
        return ResponseValue.success(data);
    }

    @RequestMapping("/download/{fileName}")
    public void download(HttpServletResponse response, @PathVariable("fileName") String fileName) {
        InputStream in = null;
        try {
            ObjectStat stat = minioClient.statObject(MINIO_BUCKET, fileName);
            response.setContentType(stat.contentType());
            response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileName, "UTF-8"));

            in = minioClient.getObject(MINIO_BUCKET, fileName);
            IOUtils.copy(in, response.getOutputStream());
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    log.error(e.getMessage());
                }
            }
        }
    }

    @DeleteMapping("/delete/{fileName}")
    public ResponseValue delete(@PathVariable("fileName") String fileName) {
        try {
            minioClient.removeObject(MINIO_BUCKET, fileName);
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseValue.fail("删除失败");
        }
        return ResponseValue.success("删除成功");
    }

    private static String formatFileSize(long fileS) {
        DecimalFormat df = new DecimalFormat("#.00");
        String fileSizeString = "";
        String wrongSize = "0B";
        if (fileS == 0) {
            return wrongSize;
        }
        if (fileS < 1024) {
            fileSizeString = df.format((double) fileS) + " B";
        } else if (fileS < 1048576) {
            fileSizeString = df.format((double) fileS / 1024) + " KB";
        } else if (fileS < 1073741824) {
            fileSizeString = df.format((double) fileS / 1048576) + " MB";
        } else {
            fileSizeString = df.format((double) fileS / 1073741824) + " GB";
        }
        return fileSizeString;
    }

}
