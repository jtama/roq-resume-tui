package io.quarkiverse.roq.theme.resume.editor.tui;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkiverse.roq.theme.resume.editor.model.Profile;
import io.quarkiverse.roq.theme.resume.editor.service.ResumeRepository;

@ApplicationScoped
public class ProfileEditor {

    @Inject
    ResumeRepository repository;

    private Profile currentProfile;
    private Long currentResumeId;

    public void load(Long resumeId) {
        this.currentResumeId = resumeId;
        this.currentProfile = repository.getProfile(resumeId);
    }

    public void save() {
        if (currentResumeId != null) {
            repository.saveProfile(currentResumeId, currentProfile);
        }
    }

    public Profile getCurrentProfile() {
        return currentProfile;
    }

    public void updateField(String field, String value) {
        // Implementation to update the record in a mutable way or recreate it
    }
}
