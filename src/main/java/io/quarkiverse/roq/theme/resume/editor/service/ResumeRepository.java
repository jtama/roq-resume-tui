package io.quarkiverse.roq.theme.resume.editor.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkiverse.roq.theme.resume.editor.model.Bio;
import io.quarkiverse.roq.theme.resume.editor.model.Profile;
import io.quarkiverse.roq.theme.resume.editor.model.Social;
import io.quarkus.logging.Log;

@ApplicationScoped
public class ResumeRepository {

    @Inject
    DataSource dataSource;

    // --- Profile ---

    public Profile getProfile() {
        String sql = "SELECT first_name, last_name, picture, job_title, bio, city, country, phone, email, site FROM profile LIMIT 1";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return new Profile(rs.getString("first_name"), rs.getString("last_name"), rs.getString("picture"),
                        rs.getString("job_title"), rs.getString("bio"), rs.getString("city"), rs.getString("country"),
                        rs.getString("phone"), rs.getString("email"), rs.getString("site"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new Profile("", "", "", "", "", "", "", "", "", "");
    }

    public void saveProfile(Profile profile) {
        String deleteSql = "DELETE FROM profile";
        String insertSql = "INSERT INTO profile (first_name, last_name, picture, job_title, bio, city, country, phone, email, site) VALUES (?,?,?,?,?,?,?,?,?,?)";

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psDelete = conn.prepareStatement(deleteSql);
                    PreparedStatement psInsert = conn.prepareStatement(insertSql)) {
                psDelete.execute();
                psInsert.setString(1, profile.firstName());
                psInsert.setString(2, profile.lastName());
                psInsert.setString(3, profile.picture());
                psInsert.setString(4, profile.jobTitle());
                psInsert.setString(5, profile.bio());
                psInsert.setString(6, profile.city());
                psInsert.setString(7, profile.country());
                psInsert.setString(8, profile.phone());
                psInsert.setString(9, profile.email());
                psInsert.setString(10, profile.site());
                psInsert.execute();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // --- Social ---

    public Social getSocial() {
        List<Social.Item> items = new ArrayList<>();
        String sql = "SELECT name, url FROM social_item ORDER BY sort_order";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                items.add(new Social.Item(rs.getString("name"), rs.getString("url")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new Social(items);
    }

    public void saveSocial(Social social) {
        String deleteSql = "DELETE FROM social_item";
        String insertSql = "INSERT INTO social_item (name, url, sort_order) VALUES (?,?,?)";

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psDelete = conn.prepareStatement(deleteSql);
                    PreparedStatement psInsert = conn.prepareStatement(insertSql)) {
                psDelete.execute();
                if (social.items() != null) {
                    int order = 0;
                    for (Social.Item item : social.items()) {
                        psInsert.setString(1, item.name());
                        psInsert.setString(2, item.url());
                        psInsert.setInt(3, order++);
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
            Log.error(e);
            throw new RuntimeException(e);
        }
    }

    // --- Bio ---
    // Note: The original implementation was complex with recursive items and
    // map-based reconstruction.
    // For JDBC, this will require multiple selects or a recursive join query, and
    // then mapping.

    public Bio getBio() {
        // Simplified for now - assuming limited depth or simple structure for the
        // prototype
        // To do this correctly with pure JDBC, it's safer to fetch sections and then
        // items.
        // I will implement a basic version that fits the structure of the model.
        return new Bio(new ArrayList<>()); // Placeholder
    }

    public void saveBio(Bio bio) {
        // Implementation pending for nested structure
    }
}
