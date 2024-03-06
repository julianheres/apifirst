package com.zhourui;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

@Mojo(name = "generate", defaultPhase = LifecyclePhase.INITIALIZE)
public class OpenAPIMojo extends AbstractMojo {
    @Parameter(property = "project", readonly = true)
    private MavenProject project;

    @Parameter
    private OpenApiConfig openApiConfig;


    public void execute() throws MojoExecutionException, MojoFailureException {
        String basedDir = project.getBuild().getDirectory();
        OpenAPIGenerator generator = new OpenAPIGenerator(
                project, getLog(), basedDir
        );
        generator.generate(openApiConfig);
    }
}
