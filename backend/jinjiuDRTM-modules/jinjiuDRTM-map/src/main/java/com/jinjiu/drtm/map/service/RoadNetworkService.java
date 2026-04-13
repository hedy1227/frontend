package com.jinjiu.drtm.map.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jinjiu.drtm.map.domain.RoadNetwork;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@Service
public class RoadNetworkService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Value("${app.road-network.storage-path:src/main/resources/static/road-network}")
    private String storagePath;

    public RoadNetwork uploadAndParse(MultipartFile file) throws IOException {
        // 1. 生成唯一ID，保存文件
        String id = UUID.randomUUID().toString().replace("-", "");
        String originalName = file.getOriginalFilename();
        String ext = (originalName != null && originalName.endsWith(".geojson")) ? ".geojson" : ".json";
        String savedFileName = id + ext;

        Path dir = Paths.get(storagePath);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
        Path filePath = dir.resolve(savedFileName);
        file.transferTo(filePath.toFile());

        // 2. 解析GeoJSON，验证格式
        JsonNode root = MAPPER.readTree(filePath.toFile());
        JsonNode features = root.get("features");
        if (features == null || !features.isArray()) {
            Files.deleteIfExists(filePath);
            throw new IllegalArgumentException("无效的GeoJSON格式：缺少features数组");
        }

        // 3. 统计片区数量
        int polygonCount = 0;
        int lineCount = 0;
        Map<String, Integer> classCount = new HashMap<>();
        for (JsonNode feature : features) {
            JsonNode geometry = feature.path("geometry");
            String geomType = geometry.path("type").asText("");
            if ("Polygon".equals(geomType)) {
                polygonCount++;
            } else if ("LineString".equals(geomType)) {
                lineCount++;
            }
            String roadClass = feature.path("properties").path("road_class").asText("zone");
            classCount.merge(roadClass, 1, Integer::sum);
        }

        // 4. 构建返回值
        RoadNetwork result = new RoadNetwork();
        result.setId(id);
        result.setFileName(originalName);
        result.setGeoJsonUrl("/static/road-network/" + savedFileName);
        result.setFeatureCount(features.size());
        result.setRoadClass(String.join(",", classCount.keySet()));
        if (polygonCount > 0 && lineCount > 0) {
            result.setMessage("上传成功，共 " + polygonCount + " 个片区，" + lineCount + " 条道路");
        } else if (polygonCount > 0) {
            result.setMessage("上传成功，共 " + polygonCount + " 个片区");
        } else {
            result.setMessage("上传成功，共 " + lineCount + " 条道路");
        }
        return result;
    }

    public List<RoadNetwork> listRoadNetworks() {
        // 扫描storagePath目录，返回文件列表
        List<RoadNetwork> list = new ArrayList<>();
        Path dir = Paths.get(storagePath);
        if (!Files.exists(dir)) {
            return list;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.geojson")) {
            for (Path path : stream) {
                RoadNetwork rn = new RoadNetwork();
                rn.setId(path.getFileName().toString().replace(".geojson", ""));
                rn.setFileName(path.getFileName().toString());
                rn.setGeoJsonUrl("/static/road-network/" + path.getFileName().toString());
                list.add(rn);
            }
        } catch (IOException e) {
            // ignore
        }
        return list;
    }

    public void deleteRoadNetwork(String id) {
        Path filePath = Paths.get(storagePath, id + ".geojson");
        if (Files.exists(filePath)) {
            try {
                Files.delete(filePath);
            } catch (IOException e) {
                throw new RuntimeException("删除文件失败", e);
            }
        }
        Path jsonPath = Paths.get(storagePath, id + ".json");
        if (Files.exists(jsonPath)) {
            try {
                Files.delete(jsonPath);
            } catch (IOException e) {
                throw new RuntimeException("删除文件失败", e);
            }
        }
    }
}
