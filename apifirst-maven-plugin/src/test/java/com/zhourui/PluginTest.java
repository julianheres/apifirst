package com.zhourui;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.project.MavenProject;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PluginTest {
    @Test
    public void openApiTest() {
//        String fileName = "petstore.yaml";
//        ClassLoader classLoader = getClass().getClassLoader();
//        InputStream inputStream = classLoader.getResourceAsStream(fileName);
//
//        // the stream holding the file content
//        if (inputStream == null) {
//            throw new IllegalArgumentException("file not found! " + fileName);
//        }
//        String fileName = "petstore.yaml";
//        ClassLoader classLoader = getClass().getClassLoader();
//        File file = new File(classLoader.getResource(fileName).getFile());
//        System.out.println(file.getAbsolutePath());


        String path = "";
        File file = new File(path);
        String myBasedir = file.getAbsolutePath();
        OpenApiConfig openApiConfig = new OpenApiConfig();
        List<ApiSpec> list = new ArrayList<>();
        ApiSpec apiSpec = new ApiSpec();
        apiSpec.url = "src/test/resources/petstore.yaml"; // TODO
        apiSpec.type = "java";
        list.add(apiSpec);
        openApiConfig.apiSpecs = list;
        Map configOptions = new HashMap(
                Map.of(
                        "library", "resttemplate"
                )
        );
        apiSpec.configOptions = configOptions;
        MavenProject project = new MavenProject();
        Log log = new SystemStreamLog();
        OpenAPIGenerator generator = new OpenAPIGenerator(
                project,log, myBasedir + "/target");
        generator.generate(openApiConfig);
    }
}
