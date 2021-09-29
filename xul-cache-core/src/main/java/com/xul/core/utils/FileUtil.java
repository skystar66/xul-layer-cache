package com.xul.core.utils;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.javatuples.Pair;
import org.springframework.core.io.ClassPathResource;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings("unchecked")
public class FileUtil {

    /**
     * The "active profiles" property name.
     */
    private static final String ACTIVE_PROFILES_PROPERTY = "spring.profiles.active";

    /**
     * The "config location" property name.
     */
    private static final String CONFIG_LOCATION_PROPERTY = "spring.config.location";

    /**
     * The "config file name" property name.
     */
    private static final String CONFIG_FILE_NAME = "application";

    /**
     * The "config file suffix" property name.
     */
    private static final String CONFIG_FILE_SUFFIX = "yml";

    /**
     * The "config file dir" property name.
     */
    @Getter
    private static String configFileDir = getJarPath();

    private static final char EXTENSION_SEPARATOR = '.';

    private static final String FOLDER_SEPARATOR = File.separator;

    /**
     * 约定大于配置，配置文件名称 application.yml
     * <p>
     * 外部配置文件的配置覆盖jar包内的配置
     */
    private static Map<String, Object> loadFlattenedConfig() {
        String fileName = getFileName();
        boolean exists = existsFile(fileName);
        if (exists) {
            Map<String, Object> resultMap = getFlattenedMap(getResourceAsStream(fileName, false));
            Map<String, Object> flattenedMap = getFlattenedMap(getResourceAsStream(fileName, true));
            resultMap.putAll(flattenedMap);
            return resultMap;
        } else {
            return getFlattenedMap(getResourceAsStream(fileName, false));
        }
    }

    private static Map<String, Object> cacheYamlConfigMap = new HashMap<>();

    public static Map<String, Object> loadFlattenedYaml() {
        if (cacheYamlConfigMap.isEmpty()) {
            cacheYamlConfigMap = loadFlattenedConfig();
        }
        cacheYamlConfigMap.forEach((key, value) -> System.setProperty(key, value.toString()));
        return cacheYamlConfigMap;
    }

    private static boolean hasConfigLocationProperty() {
        return Objects.nonNull(System.getProperty(CONFIG_LOCATION_PROPERTY));
    }

    public static String loadClassPathResource(String fileName) {
        try (InputStream inputStream = loadClassPathStream(fileName)) {
            return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("读取相关配置文件 {} 失败", fileName);
        }
        return "";
    }

    public static InputStream loadClassPathStream(String fileName) throws FileNotFoundException {
        boolean exists = existsFile(fileName);
        return getResourceStream(fileName, exists);
    }

    private static boolean existsFile(String fileName) {
        return new File(fileName).exists() || new File(configFileDir + fileName).exists();
    }

    /**
     * 获取配置文件或者配置目录
     * <p>
     * 支持 active profiles 和  config location
     */
    public static String getFileName() {
        String profile = System.getProperty(ACTIVE_PROFILES_PROPERTY);
        if (hasConfigLocationProperty()) {
            String location = System.getProperty(CONFIG_LOCATION_PROPERTY);
            // 不以yml后缀结尾，说明配置的是目录
            if (!CONFIG_FILE_SUFFIX.equals(getFilenameExtension(location))) {
                buildConfigFileDir(location);
                return location + (Objects.nonNull(profile) ? getConfigFileName(profile) : getDefaultFileName());
            }
            // 以yml后缀结尾，直接返回
            return location;
        }
        return Objects.nonNull(profile) ? getConfigFileName(profile) : getDefaultFileName();
    }

    /**
     * 更新配置文件目录
     */
    private static void buildConfigFileDir(String location) {
        if (isAbsolute(location)) {
            configFileDir = location;
        } else {
            configFileDir = configFileDir + location;
        }
    }

    private static String getConfigFileName(String profile) {
        return CONFIG_FILE_NAME + "-" + profile + "." + CONFIG_FILE_SUFFIX;
    }

    private static String getDefaultFileName() {
        return CONFIG_FILE_NAME + "." + CONFIG_FILE_SUFFIX;
    }

    private static Map<String, Object> getResourceAsStream(String fileName, boolean hasOuterFile) {
        try (InputStream inputStream = getResourceStream(fileName, hasOuterFile)) {
            return inputStream == null ? Maps.newHashMap() : new Yaml().loadAs(inputStream, Map.class);
        } catch (IOException e) {
            log.warn("读取yaml文件 {} 失败", fileName);
        }
        return Maps.newHashMap();
    }

    private static InputStream getResourceStream(String fileName, boolean hasOuterFile) throws FileNotFoundException {
        if (hasOuterFile) {
            return new BufferedInputStream(new FileInputStream(isAbsolute(fileName) ? fileName : configFileDir + fileName));
        } else {
            return FileUtil.class.getClassLoader().getResourceAsStream(fileName);
        }
    }

    /**
     * 是否是绝对路径
     */
    private static boolean isAbsolute(String fileName) {
        return new File(fileName).exists();
    }

    public static List<Pair<Long, Double>> loadDataFromCsv(String fileName) {
        List<Pair<Long, Double>> inputList = new ArrayList<>();
        List<Pair<Long, Double>> resultList = new ArrayList<>();
        File inputF = new File(fileName);
        try (
            InputStream inputStream = new FileInputStream(inputF);
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
        ) {
            // 增加头信息判断，否则会丢失第一行的数据
            try {
                String readLine = br.readLine();
                if (readLine != null) {
                    String[] csvHead = readLine.split(",");
                    Long headTimeStamp = Long.valueOf(csvHead[0]);
                    Double headValue = Double.valueOf(csvHead[1]);
                    inputList.add(new Pair<>(headTimeStamp, headValue));
                    inputList.addAll(br.lines().map(v -> new Pair<>(Long.valueOf(v.split(",")[0]), Double.valueOf(v.split(",")[1]))).collect(Collectors.toList()));
                }
            } catch (NumberFormatException e) {
                inputList = br.lines().map(v -> new Pair<>(Long.valueOf(v.split(",")[0]), Double.valueOf(v.split(",")[1]))).collect(Collectors.toList());
            }
            if (!inputList.isEmpty()) {
                // 按时间戳排序
                inputList.sort(Comparator.comparing(Pair::getValue0));
                resultList.add(inputList.get(0));
                // 时间戳相同，取第一次出现的数据点值
                inputList.forEach(v -> {
                    if (!v.getValue0().equals(resultList.get(resultList.size() - 1).getValue0())) {
                        resultList.add(v);
                    }
                });
            }
        } catch (IOException e) {
            log.error("Load data file failed!", e);
        }
        return resultList;
    }

    /**
     * 获取当前服务路径
     */
    public static String getJarPath() {
        return getJarPath(getJarFullPath(FileUtil.class));
    }

    /**
     * 获取指定Class所在的包路径
     */
    public static String getJarPath(Class clazz) {
        return getJarPath(getJarFullPath(clazz));
    }

    public static String getJarPath(String path) {
        if (Objects.isNull(path)) {
            return "";
        }
        int bootInf = path.lastIndexOf("/BOOT-INF");
        if (bootInf > 0) {
            path = path.substring(0, bootInf);
        }
        int jarIndex = path.lastIndexOf(".jar");
        //  jar运行和单元测试区分
        if (jarIndex > 0) {
            path = path.substring(0, jarIndex);
            int lastSlashIndex = path.lastIndexOf("/");
            path = path.substring(0, lastSlashIndex + 1);
        } else if (path.endsWith("test-classes/")) {
            int pos = path.lastIndexOf("target");
            path = path.substring(0, pos);
            path = path + "src/test/resources/";
        } else {
            int pos = path.lastIndexOf("target");
            path = path.substring(0, pos);
            path = path + "src/main/resources/";
        }
        return path;
    }

    private static String getJarFullPath(Class clazz) {
        String path = clazz.getProtectionDomain().getCodeSource().getLocation().getPath();
        try {
            path = URLDecoder.decode(path, "UTF-8");
            path = path.replace("\\", "/");
            if (path.contains(":")) {
                // 删去 file:
                path = path.substring(5);
            }
            return path;
        } catch (UnsupportedEncodingException e) {
            log.error("[FileUtil] get JarFullPath, error message: {}", e.getMessage(), e);
        }
        return "";
    }

    private static Map<String, Object> getFlattenedMap(Map<String, Object> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        buildFlattenedMap(result, source, null);
        return result;
    }

    private static void buildFlattenedMap(Map<String, Object> result, Map<String, Object> source, String path) {
        source.forEach((key, value) -> {
            if (!Strings.isNullOrEmpty(path)) {
                if (key.startsWith("[")) {
                    key = path + key;
                } else {
                    key = path + '.' + key;
                }
            }
            if (value instanceof String) {
                result.put(key, value);
            } else if (value instanceof Map) {
                // Need a compound key
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) value;
                buildFlattenedMap(result, map, key);
            } else if (value instanceof Collection) {
                buildStepFlattenedMap(result, key, value);
            } else {
                result.put(key, (value != null ? value : ""));
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static void buildStepFlattenedMap(Map<String, Object> result, String key, Object value) {
        // Need a compound key
        Collection<Object> collection = (Collection<Object>) value;
        if (collection.isEmpty()) {
            result.put(key, "");
        } else {
            int count = 0;
            for (Object object : collection) {
                buildFlattenedMap(result, Collections.singletonMap(
                    "[" + (count++) + "]", object), key);
            }
        }
    }

    public static <T> T loadConfig(String fileName, Class<T> targetType) throws IOException {
        File configF = new File(fileName);
        try (
            InputStream inputStream = new FileInputStream(configF)
        ) {
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            return GSONUtil.fromJson(br, targetType);
        } catch (IOException e) {
            log.error("Load configuration file failed!", e);
            throw e;
        }
    }

    private static String getFilenameExtension(String path) {
        if (path == null) {
            return null;
        }

        int extIndex = path.lastIndexOf(EXTENSION_SEPARATOR);
        if (extIndex == -1) {
            return null;
        }

        int folderIndex = path.lastIndexOf(FOLDER_SEPARATOR);
        if (folderIndex > extIndex) {
            return null;
        }

        return path.substring(extIndex + 1);
    }

    public static InputStream loadOtherClassPathResource(String filePath) {
        ClassPathResource classPathResource = new ClassPathResource(filePath);
        try {
            return classPathResource.getInputStream();
        } catch (IOException e) {
            log.error("[Util] Fail to load file:{}", filePath, e);
        }
        return null;
    }

    public static Map<String, String> loadMapsFromCsv(InputStream inputStream, boolean reverse) {
        Map<String, String> res = new HashMap<>(16);
        try (
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
        ) {
            if (!reverse) {
                br.lines().forEach(v -> {
                    String[] split = v.split(",");
                    res.put(split[0], split[1]);
                });
            } else {
                br.lines().forEach(v -> {
                    String[] split = v.split(",");
                    res.put(split[1], split[0]);
                });
            }

        } catch (IOException e) {
            log.error("Load local csv data file failed!", e);
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                log.error("loadPointDataFromCsv failed!", e);
            }
        }
        return res;
    }

}
