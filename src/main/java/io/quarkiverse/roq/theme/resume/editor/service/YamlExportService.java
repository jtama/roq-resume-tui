package io.quarkiverse.roq.theme.resume.editor.service;

import java.io.File;
import java.io.IOException;

import jakarta.enterprise.context.ApplicationScoped;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.quarkiverse.roq.theme.resume.editor.model.Bio;
import io.quarkiverse.roq.theme.resume.editor.model.Profile;
import io.quarkiverse.roq.theme.resume.editor.model.Social;

@ApplicationScoped
public class YamlExportService {

    private final ObjectMapper yamlMapper;

    public YamlExportService() {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
    }

    public void exportProfile(Profile profile, String outputDir) throws IOException {
        export(profile, outputDir, "profile.yaml");
    }

    public void exportBio(Bio bio, String outputDir) throws IOException {
        export(bio, outputDir, "bio.yaml");
    }

    public void exportSocial(Social social, String outputDir) throws IOException {
        export(social, outputDir, "social.yaml");
    }

    private void export(Object data, String outputDir, String filename) throws IOException {
        File directory = new File(outputDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        yamlMapper.writeValue(new File(directory, filename), data);
    }
}
