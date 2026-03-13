package io.quarkiverse.roq.theme.resume.editor.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkiverse.roq.theme.resume.editor.model.Bio;
import io.quarkiverse.roq.theme.resume.editor.model.Profile;
import io.quarkiverse.roq.theme.resume.editor.model.Resume;
import io.quarkiverse.roq.theme.resume.editor.model.ResumeSummary;
import io.quarkiverse.roq.theme.resume.editor.model.Social;
import io.quarkus.logging.Log;

@ApplicationScoped
public class ResumeRepository {

    @Inject
    DataSource dataSource;

    public List<ResumeSummary> findAllSummaries() {
        List<ResumeSummary> resumes = new ArrayList<>();
        String sql = "SELECT id, slug, title FROM resume ORDER BY title";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                resumes.add(new ResumeSummary(rs.getLong("id"), rs.getString("slug"), rs.getString("title")));
            }
        } catch (SQLException e) {
            Log.error("Failed to fetch resume summaries", e);
        }
        return resumes;
    }

    public Optional<Resume> find(long resumeId) {
        String sql = "SELECT id, slug, title FROM resume WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, resumeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Resume(
                            rs.getLong("id"),
                            rs.getString("slug"),
                            rs.getString("title"),
                            getProfile(resumeId),
                            getSocial(resumeId),
                            getBio(resumeId)));
                }
            }
        } catch (SQLException e) {
            Log.errorf(e, "Failed to find resume with id %d", resumeId);
        }
        return Optional.empty();
    }

    public Resume create(String title, String slug) {
        String insertResumeSql = "INSERT INTO resume (title, slug) VALUES (?, ?)";
        String insertProfileSql = "INSERT INTO profile (resume_id, first_name, last_name) VALUES (?, ?, ?)";
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            long resumeId = -1;
            try (PreparedStatement psResume = conn.prepareStatement(insertResumeSql, Statement.RETURN_GENERATED_KEYS)) {
                psResume.setString(1, title);
                psResume.setString(2, slug);
                psResume.executeUpdate();
                try (ResultSet generatedKeys = psResume.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        resumeId = generatedKeys.getLong(1);
                    } else {
                        throw new SQLException("Creating resume failed, no ID obtained.");
                    }
                }
            }

            try (PreparedStatement psProfile = conn.prepareStatement(insertProfileSql)) {
                psProfile.setLong(1, resumeId);
                psProfile.setString(2, "");
                psProfile.setString(3, "");
                psProfile.executeUpdate();
            }

            conn.commit();
            return find(resumeId).orElseThrow();

        } catch (SQLException e) {
            Log.error("Failed to create new resume", e);
            throw new RuntimeException(e);
        }

    }

    // --- Profile ---

    public Profile getProfile(long resumeId) {
        String sql = "SELECT first_name, last_name, picture, job_title, bio, city, country, phone, email, site FROM profile WHERE resume_id = ? LIMIT 1";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, resumeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Profile(rs.getString("first_name"), rs.getString("last_name"), rs.getString("picture"),
                            rs.getString("job_title"), rs.getString("bio"), rs.getString("city"), rs.getString("country"),
                            rs.getString("phone"), rs.getString("email"), rs.getString("site"));
                }
            }
        } catch (SQLException e) {
            Log.errorf(e, "Failed to get profile for resume_id %d", resumeId);
        }
        return new Profile("", "", "", "", "", "", "", "", "", "");
    }

    public void saveProfile(long resumeId, Profile profile) {
        String updateSql = "UPDATE profile SET first_name = ?, last_name = ?, picture = ?, job_title = ?, bio = ?, city = ?, country = ?, phone = ?, email = ?, site = ? WHERE resume_id = ?";

        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(updateSql)) {
            ps.setString(1, profile.firstName());
            ps.setString(2, profile.lastName());
            ps.setString(3, profile.picture());
            ps.setString(4, profile.jobTitle());
            ps.setString(5, profile.bio());
            ps.setString(6, profile.city());
            ps.setString(7, profile.country());
            ps.setString(8, profile.phone());
            ps.setString(9, profile.email());
            ps.setString(10, profile.site());
            ps.setLong(11, resumeId);
            ps.executeUpdate();
        } catch (SQLException e) {
            Log.errorf(e, "Failed to save profile for resume_id %d", resumeId);
            throw new RuntimeException(e);
        }
    }

    // --- Social ---

    public Social getSocial(long resumeId) {
        List<Social.Item> items = new ArrayList<>();
        String sql = "SELECT name, url FROM social_item WHERE resume_id = ? ORDER BY sort_order";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, resumeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(new Social.Item(rs.getString("name"), rs.getString("url")));
                }
            }
        } catch (SQLException e) {
            Log.errorf(e, "Failed to get social for resume_id %d", resumeId);
        }
        return new Social(items);
    }

    public void saveSocial(long resumeId, Social social) {
        String deleteSql = "DELETE FROM social_item WHERE resume_id = ?";
        String insertSql = "INSERT INTO social_item (resume_id, name, url, sort_order) VALUES (?,?,?,?)";

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psDelete = conn.prepareStatement(deleteSql)) {
                psDelete.setLong(1, resumeId);
                psDelete.execute();
            }
            try (PreparedStatement psInsert = conn.prepareStatement(insertSql)) {
                if (social.items() != null) {
                    int order = 0;
                    for (Social.Item item : social.items()) {
                        psInsert.setLong(1, resumeId);
                        psInsert.setString(2, item.name());
                        psInsert.setString(3, item.url());
                        psInsert.setInt(4, order++);
                        psInsert.addBatch();
                    }
                    psInsert.executeBatch();
                }
            }
            conn.commit();
        } catch (SQLException e) {
            Log.errorf(e, "Failed to save social for resume_id %d", resumeId);
            throw new RuntimeException(e);
        }
    }

    // --- Bio ---
    // Note: The original implementation was complex with recursive items and
    // map-based reconstruction.
    // For JDBC, this will require multiple selects or a recursive join query, and
    // then mapping.

    public Bio getBio(long resumeId) {
        // Simplified for now - assuming limited depth or simple structure for the
        // prototype
        // To do this correctly with pure JDBC, it's safer to fetch sections and then
        // items.
        // I will implement a basic version that fits the structure of the model.
        return new Bio(new ArrayList<>()); // Placeholder
    }

    public void saveBio(long resumeId, Bio bio) {
        // Implementation pending for nested structure
    }

}
