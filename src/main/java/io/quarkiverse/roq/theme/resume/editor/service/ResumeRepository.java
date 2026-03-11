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
        List<Bio.Section> sections = new ArrayList<>();
        String sqlSections = "SELECT id, title FROM bio_section ORDER BY sort_order";

        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sqlSections);
                ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int sectionId = rs.getInt("id");
                String title = rs.getString("title");
                List<Bio.Item> items = getItemsForSection(conn, sectionId, null);
                sections.add(new Bio.Section(title, items));
            }
        } catch (SQLException e) {
            Log.error(e);
            throw new RuntimeException(e);
        }
        return new Bio(sections);
    }

    private List<Bio.Item> getItemsForSection(Connection conn, int sectionId, Integer parentId) throws SQLException {
        List<Bio.Item> items = new ArrayList<>();
        String sql = "SELECT id, header, title, link, content, logo_label, logo_image_url, logo_link, collapsible, collapsed, ruler FROM bio_item WHERE section_id = ? AND "
                + (parentId == null ? "parent_id IS NULL" : "parent_id = ?") + " ORDER BY sort_order";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sectionId);
            if (parentId != null) {
                ps.setInt(2, parentId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int itemId = rs.getInt("id");
                    Bio.Logo logo = null;
                    if (rs.getString("logo_label") != null) {
                        logo = new Bio.Logo(rs.getString("logo_label"), rs.getString("logo_image_url"),
                                rs.getString("logo_link"));
                    }

                    List<Bio.Item> subItems = getItemsForSection(conn, sectionId, itemId);

                    items.add(new Bio.Item(
                            rs.getString("header"),
                            rs.getString("title"),
                            rs.getString("link"),
                            rs.getString("content"),
                            logo,
                            rs.getBoolean("collapsible"),
                            rs.getBoolean("collapsed"),
                            rs.getBoolean("ruler"),
                            subItems));
                }
            }
        }
        return items;
    }

    public void saveBio(Bio bio) {
        String deleteSections = "DELETE FROM bio_section"; // Cascades to bio_item
        String insertSection = "INSERT INTO bio_section (title, sort_order) VALUES (?, ?)";
        String insertItem = "INSERT INTO bio_item (section_id, header, title, link, content, logo_label, logo_image_url, logo_link, collapsible, collapsed, ruler, parent_id, sort_order) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psDelete = conn.prepareStatement(deleteSections);
                    PreparedStatement psInsertSection = conn.prepareStatement(insertSection,
                            PreparedStatement.RETURN_GENERATED_KEYS);
                    PreparedStatement psInsertItem = conn.prepareStatement(insertItem,
                            PreparedStatement.RETURN_GENERATED_KEYS)) {

                psDelete.execute();

                if (bio.list() != null) {
                    int sectionOrder = 0;
                    for (Bio.Section section : bio.list()) {
                        psInsertSection.setString(1, section.title());
                        psInsertSection.setInt(2, sectionOrder++);
                        psInsertSection.executeUpdate();

                        try (ResultSet generatedKeys = psInsertSection.getGeneratedKeys()) {
                            if (generatedKeys.next()) {
                                int sectionId = generatedKeys.getInt(1);
                                if (section.items() != null) {
                                    saveItems(psInsertItem, sectionId, null, section.items());
                                }
                            }
                        }
                    }
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

    private void saveItems(PreparedStatement psInsertItem, int sectionId, Integer parentId, List<Bio.Item> items)
            throws SQLException {
        int order = 0;
        for (Bio.Item item : items) {
            psInsertItem.setInt(1, sectionId);
            psInsertItem.setString(2, item.header());
            psInsertItem.setString(3, item.title());
            psInsertItem.setString(4, item.link());
            psInsertItem.setString(5, item.content());
            if (item.logo() != null) {
                psInsertItem.setString(6, item.logo().label());
                psInsertItem.setString(7, item.logo().imageUrl());
                psInsertItem.setString(8, item.logo().link());
            } else {
                psInsertItem.setNull(6, java.sql.Types.VARCHAR);
                psInsertItem.setNull(7, java.sql.Types.VARCHAR);
                psInsertItem.setNull(8, java.sql.Types.VARCHAR);
            }
            psInsertItem.setBoolean(9, item.collapsible() != null && item.collapsible());
            psInsertItem.setBoolean(10, item.collapsed() != null && item.collapsed());
            psInsertItem.setBoolean(11, item.ruler() != null && item.ruler());
            if (parentId != null) {
                psInsertItem.setInt(12, parentId);
            } else {
                psInsertItem.setNull(12, java.sql.Types.INTEGER);
            }
            psInsertItem.setInt(13, order++);
            psInsertItem.executeUpdate();

            if (item.subItems() != null && !item.subItems().isEmpty()) {
                try (ResultSet generatedKeys = psInsertItem.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int itemId = generatedKeys.getInt(1);
                        saveItems(psInsertItem, sectionId, itemId, item.subItems());
                    }
                }
            }
        }
    }
}
