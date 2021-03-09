package com.leyou.upload.service;

import com.github.tobato.fastdfs.domain.StorePath;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import com.leyou.common.exception.LyException;
import com.leyou.upload.config.UploadProperties;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

/**
 * @author: HuYi.Zhang
 * @create: 2018-08-17 16:52
 **/
@Service
@EnableConfigurationProperties(UploadProperties.class)
public class UploadService {

    @Autowired
    private UploadProperties prop;

    @Autowired
    private FastFileStorageClient storageClient;

    public String uploadImage(MultipartFile file) {
        try {
            // 校验文件类型
            if (!prop.getAllowFileTypes().contains(file.getContentType())) {
                throw new LyException(HttpStatus.BAD_REQUEST, "不允许的文件类型");
            }
            // 校验文件内容
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                throw new LyException(HttpStatus.BAD_REQUEST, "文件内容受损");
            }
           /* // 准备文件夹
            File destDir = new File(prop.getLocalPath());
            if (!destDir.exists()) {
                destDir.mkdirs();
            }
            // 保存文件
            file.transferTo(new File(destDir, file.getOriginalFilename()));*/

            // FastDFS上传
            String fileExtName = StringUtils.substringAfterLast(file.getOriginalFilename(), ".");
            StorePath storePath = storageClient.uploadFile(file.getInputStream(), file.getSize(), fileExtName, null);
            // 组织url地址
            String url = prop.getBaseUrl() + storePath.getFullPath();

            return url;
        } catch (Exception e) {
            throw new LyException(HttpStatus.INTERNAL_SERVER_ERROR, "文件上传失败");
        }
    }
}
