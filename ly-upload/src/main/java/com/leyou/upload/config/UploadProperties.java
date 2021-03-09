package com.leyou.upload.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * @author: HuYi.Zhang
 * @create: 2018-08-17 17:05
 **/
@Data
@ConfigurationProperties(prefix = "ly.upload")
public class UploadProperties {
    private String baseUrl;
    private String localPath;
    private List<String> allowFileTypes;
}
