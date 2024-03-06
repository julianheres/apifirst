package com.zhourui;

import com.zhourui.download.DownloadFailureException;
import com.zhourui.download.HttpFileRequester;
import com.zhourui.download.SilentProgressReport;
import org.apache.http.HttpHeaders;
import org.apache.http.message.BasicHeader;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.openapitools.codegen.*;
import org.openapitools.codegen.config.CodegenConfigurator;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.plugin.logging.Log;
import org.openapitools.codegen.config.GlobalSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.maven.shared.utils.StringUtils.isNotEmpty;

public class OpenAPIGenerator {

    private static boolean isLocalFile = false;
    private File output;

    private String downloadSpecFolder;
    private boolean dryRun = false;

    private String[] apiPkg;

    private final Logger logger = LoggerFactory.getLogger(OpenAPIGenerator.class);
    private Log mavenPluginLog;

    private MavenProject project;
    public OpenAPIGenerator(MavenProject project, Log mavenPluginLog, String buildDir) {
        this.project = project;
        this.mavenPluginLog = mavenPluginLog;
        this.output = new File(buildDir,"generated-sources/openapi");
        this.downloadSpecFolder = buildDir + File.separator + "downloaded-openapi";
    }

    public void generate(OpenApiConfig openApiConfig) {
        for (ApiSpec apiSpec: openApiConfig.apiSpecs) {
            try {
                doGenerate(apiSpec);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private Map preConfig(ApiSpec apiSpec) {
        Map configOptions = apiSpec.configOptions == null? new HashMap<>():apiSpec.configOptions;
        configOptions.putAll(Map.of(
                "interfaceOnly", true,
                "dateLibrary", "java8",
                "delegatePattern", false,
                "useTags", true,
                "requestMappingMode", "api_interface"
        ));
        return configOptions;
    }

    private void doGenerate(ApiSpec apiSpec) throws MojoExecutionException, MojoFailureException {
        if (isEmpty(apiSpec.url)) {
            mavenPluginLog.warn("skip an empty api spec");
            return;
        }
        if (!isLocalFile && !download(apiSpec)) {
            return;
        }

        String inputSpec = getDownloadSpecName(apiSpec);
        //generate
        GlobalSettings.setProperty(CodegenConstants.API_TESTS, "false");
        Map configOptions = preConfig(apiSpec);
        CodegenConfigurator configurator = new CodegenConfigurator();
        configurator.setInputSpec(apiSpec.url);
        String isClientPkg = apiSpec.type.equals("java")?"consumer":"producer";
        String apiPackage = String.format("com.zhourui.%s.%s.%s.%s.api",
                this.apiPkg[0], this.apiPkg[1], this.apiPkg[2], isClientPkg);


        configurator.setApiPackage(apiPackage);
        String modelPackage = String.format("com.zhourui.%s.%s.%s.model",
                this.apiPkg[0], this.apiPkg[1], this.apiPkg[2]);
        configurator.setApiPackage(modelPackage);
        configurator.setGeneratorName(apiSpec.type);
        configurator.setOutputDir(output.getAbsolutePath());


        // generate config
        final ClientOptInput input = configurator.toClientOptInput();
        final CodegenConfig config = input.getConfig();

        if (configOptions != null) {
            for (CliOption langCliOption : config.cliOptions()) {
                if (configOptions.containsKey(langCliOption.getOpt())) {
                    input.getConfig().additionalProperties()
                            .put(langCliOption.getOpt(), configOptions.get(langCliOption.getOpt()));
                }
            }
        }
        new DefaultGenerator(dryRun).opts(input).generate();
        // add to source
        String sourcePath = getCompileSourceRoot(configOptions);
        logger.info("");
        project.addCompileSourceRoot(sourcePath);
        project.addTestCompileSourceRoot(getCompileSourceRoot(configOptions));
        // TODO dependency
    }

    private String getCompileSourceRoot(Map configOptions) {
        final Object sourceFolderObject =
                configOptions == null ? null : configOptions.get(CodegenConstants.SOURCE_FOLDER);
        final String sourceFolder = sourceFolderObject != null? sourceFolderObject.toString() : "src/main/java";
        return output.getPath() + File.separator + sourceFolder;
    }
    private String getDownloadSpecName(ApiSpec apiSpec) {
        this.apiPkg = apiSpec.url.split(":");
        String outputFileName = String.format("%s-%s-%s.yml", this.apiPkg[0], this.apiPkg[1], this.apiPkg[2]);
        return this.downloadSpecFolder + File.separator + outputFileName;
    }

    public boolean download(ApiSpec apiSpec) throws MojoExecutionException {
        String outputFileName = getDownloadSpecName(apiSpec);
        String urlPattern = "https://bitbucket.xxx.com/apifirst-contracts/raw/%s/%s/%s.yml"; // TODO
        String urlStr = String.format(urlPattern, this.apiPkg[0], this.apiPkg[1], this.apiPkg[2]);
        URI uri = URI.create(urlStr);
        final File outputFile = new File(outputFileName);
        if (outputFile.getParent() != null && !new File(outputFile.getParent()).exists()) {
            File parent = Paths.get(outputFile.getParent()).toFile();
            parent.mkdirs();
        }

        boolean done = false;
        for (int retriesLeft = 3; !done && retriesLeft > 0; --retriesLeft) {
            try {
                final HttpFileRequester.Builder fileRequesterBuilder = new HttpFileRequester.Builder();
                String username = System.getenv("BITBUCKET_USERNAME");
                String password = System.getenv("BITBUCKET_PASSWORD");

                final String auth = username + ":" + password;
                final byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.ISO_8859_1));
                final String authHeader = "Basic " + new String(encodedAuth);
                Map<String, String> headers = new HashMap<>();
                headers.put(HttpHeaders.AUTHORIZATION, authHeader);
                List list = headers.entrySet().stream().map(pair -> new BasicHeader(pair.getKey(), pair.getValue()))
                        .collect(Collectors.toList());

                final HttpFileRequester fileRequester = fileRequesterBuilder
                        .withProgressReport(new SilentProgressReport(this.mavenPluginLog))
                        .withConnectTimeout(3000)
                        .withSocketTimeout(3000)
                        .withUri(uri)
                        .withRedirectsEnabled(true)
                        .withUsername(username)
                        .withPassword(password)
                        .withPreemptiveAuth(false)
                        .withLog(this.mavenPluginLog)
                        .withInsecure(false)
                        .build();
                fileRequester.download(outputFile, list);
                done = true;
            } catch (DownloadFailureException ex) {
                mavenPluginLog.warn(ex.getMessage());
            } catch (IOException ex) {
                mavenPluginLog.warn(ex.getMessage());
            }
            if (!done) {
                mavenPluginLog.warn("Retrying (" + (retriesLeft -1) + " more");
            }
        }
        return done;
    }
}

