package io.quarkiverse.roq.theme.resume.editor.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkiverse.roq.theme.resume.editor.model.Bio;
import io.quarkiverse.roq.theme.resume.editor.model.Profile;
import io.quarkiverse.roq.theme.resume.editor.model.Resume;
import io.quarkiverse.roq.theme.resume.editor.model.Social;
import io.quarkus.logging.Log;

@ApplicationScoped
public class ResumeRepository {

    @Inject
    DataSource dataSource;

    // --- Resume Management ---

    public List<Resume> listResumes() {
        List<Resume> resumes = new ArrayList<>();
        String sql = "SELECT id, name FROM resume ORDER BY name";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                resumes.add(new Resume(rs.getLong("id"), rs.getString("name")));
            }
        } catch (SQLException e) {
            Log.error("Error listing resumes", e);
        }
        return resumes;
    }

    public Resume createResume(String name) {
        String sql = "INSERT INTO resume (name) VALUES (?)";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return new Resume(rs.getLong(1), name);
                }
            }
        } catch (SQLException e) {
            Log.error("Error creating resume", e);
        }
        return null;
    }

    public void updateResume(Long id, String name) {
        String sql = "UPDATE resume SET name = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            Log.error("Error updating resume", e);
        }
    }

    public void deleteResume(Long id) {
        String sql = "DELETE FROM resume WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            Log.error("Error deleting resume", e);
        }
    }

    // --- Profile ---

    public Profile getProfile(Long resumeId) {
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
            Log.error("Error getting profile", e);
        }
        return new Profile("", "", "", "", "", "", "", "", "", "");
    }

    public void saveProfile(Long resumeId, Profile profile) {
        String deleteSql = "DELETE FROM profile WHERE resume_id = ?";
        String insertSql = "INSERT INTO profile (resume_id, first_name, last_name, picture, job_title, bio, city, country, phone, email, site) VALUES (?,?,?,?,?,?,?,?,?,?,?)";

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psDelete = conn.prepareStatement(deleteSql);
                    PreparedStatement psInsert = conn.prepareStatement(insertSql)) {
                psDelete.setLong(1, resumeId);
                psDelete.execute();
                psInsert.setLong(1, resumeId);
                psInsert.setString(2, profile.firstName());
                psInsert.setString(3, profile.lastName());
                psInsert.setString(4, profile.picture());
                psInsert.setString(5, profile.jobTitle());
                psInsert.setString(6, profile.bio());
                psInsert.setString(7, profile.city());
                psInsert.setString(8, profile.country());
                psInsert.setString(9, profile.phone());
                psInsert.setString(10, profile.email());
                psInsert.setString(11, profile.site());
                psInsert.execute();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            Log.error("Error saving profile", e);
        }
    }

    // --- Social ---

    public Social getSocial(Long resumeId) {
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
            Log.error("Error getting social items", e);
        }
        return new Social(items);
    }

    public void saveSocial(Long resumeId, Social social) {
        String deleteSql = "DELETE FROM social_item WHERE resume_id = ?";
        String insertSql = "INSERT INTO social_item (resume_id, name, url, sort_order) VALUES (?,?,?,?)";

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psDelete = conn.prepareStatement(deleteSql);
                    PreparedStatement psInsert = conn.prepareStatement(insertSql)) {
                psDelete.setLong(1, resumeId);
                psDelete.execute();
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
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            Log.error("Error saving social items", e);
            throw new RuntimeException(e);
        }
    }

    // --- Bio ---

    public Bio getBio(Long resumeId) {
        // Implementation pending for nested structure, but needs to filter by resume_id
        return new Bio(new ArrayList<>());
    }

    public void saveBio(Long resumeId, Bio bio) {
        // Implementation pending for nested structure
    }
}
