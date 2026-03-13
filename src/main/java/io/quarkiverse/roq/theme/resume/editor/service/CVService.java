package io.quarkiverse.roq.theme.resume.editor.service;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkiverse.roq.theme.resume.editor.model.Resume;
import io.quarkiverse.roq.theme.resume.editor.model.ResumeSummary;
import io.smallrye.common.annotation.RunOnVirtualThread;

@ApplicationScoped
public class CVService {

    @Inject
    ResumeRepository resumeRepository;

    @RunOnVirtualThread
    public List<ResumeSummary> getResumes() {
        return resumeRepository.findAllSummaries();
    }

    @RunOnVirtualThread
    public Optional<Resume> getResume(long id) {
        return resumeRepository.find(id);
    }

    @RunOnVirtualThread
    public Resume create(String title, String slug) {
        return resumeRepository.create(title, slug);
    }

}
