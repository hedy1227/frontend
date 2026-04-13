package com.jinjiu.drtm.map.domain;

import lombok.Data;

/**
 * 四级路网数据响应对象
 */
@Data
public class RoadNetwork {
    /**
     * 文件唯一标识(UUID)
     */
    private String id;
    /**
     * 原文件名
     */
    private String fileName;
    /**
     * 访问URL，如 /static/road-network/xxx.geojson
     */
    private String geoJsonUrl;
    /**
     * 道路等级汇总，如 "highway,primary,secondary,tertiary"
     */
    private String roadClass;
    /**
     * 要素(线段)数量
     */
    private Integer featureCount;
    /**
     * 状态消息
     */
    private String message;
}
