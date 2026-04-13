package com.jinjiu.drtm.map.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.jinjiu.drtm.map.domain.RoadNetwork;
import com.jinjiu.drtm.map.service.RoadNetworkService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/road")
public class RoadNetworkController {

    private final RoadNetworkService roadNetworkService;

    public RoadNetworkController(RoadNetworkService roadNetworkService) {
        this.roadNetworkService = roadNetworkService;
    }

    /**
     * 上传四级路网GeoJSON文件
     * POST /api/map/road/upload
     */
    @PostMapping("/upload")
    public Map<String, Object> uploadRoadNetwork(@RequestParam("file") MultipartFile file) {
        StpUtil.checkLogin();
        Map<String, Object> result = new HashMap<>();

        if (file == null || file.isEmpty()) {
            result.put("code", 400);
            result.put("message", "文件不能为空");
            return result;
        }

        String name = file.getOriginalFilename();
        if (name == null || (!name.endsWith(".json") && !name.endsWith(".geojson"))) {
            result.put("code", 400);
            result.put("message", "只支持.json或.geojson文件");
            return result;
        }

        try {
            RoadNetwork rn = roadNetworkService.uploadAndParse(file);
            result.put("code", 200);
            result.put("data", rn);
            result.put("message", rn.getMessage());
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "上传失败：" + e.getMessage());
        }
        return result;
    }

    /**
     * 获取已上传的路网列表
     * GET /api/map/road/list
     */
    @GetMapping("/list")
    public Map<String, Object> listRoadNetworks() {
        StpUtil.checkLogin();
        Map<String, Object> result = new HashMap<>();
        List<RoadNetwork> list = roadNetworkService.listRoadNetworks();
        result.put("code", 200);
        result.put("data", list);
        result.put("message", "查询成功");
        return result;
    }

    /**
     * 删除指定路网
     * DELETE /api/map/road/{id}
     */
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteRoadNetwork(@PathVariable String id) {
        StpUtil.checkLogin();
        Map<String, Object> result = new HashMap<>();
        try {
            roadNetworkService.deleteRoadNetwork(id);
            result.put("code", 200);
            result.put("message", "删除成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "删除失败：" + e.getMessage());
        }
        return result;
    }
}
