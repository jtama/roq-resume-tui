package io.quarkiverse.roq.theme.resume.editor.tui;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkiverse.roq.theme.resume.editor.model.Bio;
import io.quarkiverse.roq.theme.resume.editor.service.ResumeRepository;

@ApplicationScoped
public class BioEditor {

    @Inject
    ResumeRepository repository;

    private Bio currentBio;

    public void load() {
        this.currentBio = repository.getBio();
    }

    public void save() {
        repository.saveBio(currentBio);
    }

    public Bio getCurrentBio() {
        return currentBio;
    }

    public void updateBio(Bio bio) {
        this.currentBio = bio;
    }
}
