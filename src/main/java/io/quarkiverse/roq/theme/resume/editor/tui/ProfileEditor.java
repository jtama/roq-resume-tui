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

    public void load() {
        this.currentProfile = repository.getProfile();
    }

    public void save() {
        repository.saveProfile(currentProfile);
    }

    public Profile getCurrentProfile() {
        return currentProfile;
    }

    public void updateField(String field, String value) {
        // Implementation to update the record in a mutable way or recreate it
    }
}
