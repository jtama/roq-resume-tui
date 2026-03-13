package io.quarkiverse.roq.theme.resume.editor.tui;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import dev.tamboui.toolkit.element.Element;
import dev.tamboui.widgets.form.FormState;
import dev.tamboui.widgets.form.Validators;

import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.formField;
import static dev.tamboui.toolkit.Toolkit.panel;
import static dev.tamboui.toolkit.Toolkit.row;

import io.quarkiverse.roq.theme.resume.editor.model.Profile;
import io.quarkiverse.roq.theme.resume.editor.service.ResumeRepository;

@Singleton
public class ProfileEditorWidget {

    @Inject
    ResumeRepository repository;

    private boolean loaded = false;
    private long resumeId;

    public void setResumeId(long resumeId) {
        this.resumeId = resumeId;
        this.loaded = false;
    }

    // FormState holding all profile data
    private final FormState form = FormState.builder().textField("firstName", "").textField("lastName", "")
            .textField("jobTitle", "").textField("email", "").textField("city", "").textField("country", "")
            .textField("phone", "").textField("site", "").textField("bio", "").textField("picture", "").build();

    public void load() {
        Profile profile = repository.getProfile(resumeId);

        form.setTextValue("firstName", profile.firstName() != null ? profile.firstName() : "");
        form.setTextValue("lastName", profile.lastName() != null ? profile.lastName() : "");
        form.setTextValue("jobTitle", profile.jobTitle() != null ? profile.jobTitle() : "");
        form.setTextValue("email", profile.email() != null ? profile.email() : "");
        form.setTextValue("city", profile.city() != null ? profile.city() : "");
        form.setTextValue("country", profile.country() != null ? profile.country() : "");
        form.setTextValue("phone", profile.phone() != null ? profile.phone() : "");
        form.setTextValue("site", profile.site() != null ? profile.site() : "");
        form.setTextValue("bio", profile.bio() != null ? profile.bio() : "");
        form.setTextValue("picture", profile.picture() != null ? profile.picture() : "");

        loaded = true;
    }

    public void save() {
        Profile updated = new Profile(form.textValue("firstName"), form.textValue("lastName"),
                form.textValue("picture"), form.textValue("jobTitle"), form.textValue("bio"), form.textValue("city"),
                form.textValue("country"), form.textValue("phone"), form.textValue("email"), form.textValue("site"));
        repository.saveProfile(resumeId, updated);
    }

    public Element render() {
        if (!loaded) {
            load();
        }

        // @formatter:off
        return panel("Profile Editor",
                column(
                        // Personal Info
                        row(
                                formField("First Name", form.textField("firstName"))
                                        .addClass("formfield")
                                .formState(form, "firstName")
                                .id("firstName")
                                .labelWidth(12).fill()
                                .validate(Validators.required())
                                .showInlineErrors(true)
                                .focusable(),
                        formField("Last Name", form.textField("lastName"))
                                .addClass("formfield")
                                .formState(form, "lastName")
                                .id("lastName")
                                .labelWidth(12).fill()
                                .validate(Validators.required())
                                .showInlineErrors(true)
                                .focusable()
                ).spacing(1),

                // Professional Info
                row(
                        formField("Job Title", form.textField("jobTitle"))
                                .addClass("formfield")
                                .formState(form, "jobTitle")
                                .id("jobTitle")
                                .labelWidth(12).fill()
                                .focusable(),
                        formField("Email", form.textField("email"))
                                .addClass("formfield")
                                .formState(form, "email")
                                .id("email")
                                .labelWidth(12).fill()
                                // Simple email regex, can be improved or removed if too strict
                                .validate(Validators.pattern("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$", "Invalid email"))
                                .showInlineErrors(true)
                                .focusable()
                ).spacing(1),

                // Location
                row(
                        formField("City", form.textField("city"))
                                .addClass("formfield")
                                .formState(form, "city")
                                .id("city")
                                .labelWidth(12).fill()
                                .focusable(),
                        formField("Country", form.textField("country"))
                                .addClass("formfield")
                                .formState(form, "country")
                                .id("country")
                                .labelWidth(12).fill()
                                .focusable()
                ).spacing(1),

                // Contact
                row(
                        formField("Phone", form.textField("phone"))
                                .addClass("formfield")
                                .formState(form, "phone")
                                .id("phone")
                                .labelWidth(12).fill()
                                .focusable(),
                        formField("Site", form.textField("site"))
                                .addClass("formfield")
                                .formState(form, "site")
                                .id("site")
                                .labelWidth(12).fill()
                                .focusable()
                ).spacing(1),

                // Other
                formField("Picture URL", form.textField("picture"))
                        .addClass("formfield")
                        .formState(form, "picture")
                        .id("picture")
                        .labelWidth(15).fill()
                        .focusable(),

                formField("Bio", form.textField("bio"))
                        .addClass("formfield")
                        .formState(form, "bio")
                        .id("bio")
                        .labelWidth(15).fill() // TODO: Replace with textArea when supported
                        .focusable()
                ).spacing(1)
        )
        .fill();
        // @formatter:on
    }
}
