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

    private static final String FIND_ALL_RESUMES_SQL = """
            SELECT id, name
            FROM resume
            ORDER BY name
            """;

    private static final String INSERT_RESUME_SQL = """
            INSERT INTO resume (name)
            VALUES (?)
            """;

    private static final String UPDATE_RESUME_SQL = """
            UPDATE resume
            SET name = ?
            WHERE id = ?
            """;

    private static final String DELETE_RESUME_SQL = """
            DELETE FROM resume
            WHERE id = ?
            """;

    private static final String FIND_PROFILE_BY_RESUME_ID_SQL = """
            SELECT first_name, last_name, picture, job_title, bio, city, country, phone, email, site
            FROM profile
            WHERE resume_id = ?
            LIMIT 1
            """;

    private static final String DELETE_PROFILE_BY_RESUME_ID_SQL = """
            DELETE FROM profile
            WHERE resume_id = ?
            """;

    private static final String INSERT_PROFILE_SQL = """
            INSERT INTO profile (
                resume_id, first_name, last_name, picture, job_title, bio,
                city, country, phone, email, site
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String FIND_SOCIAL_ITEMS_BY_RESUME_ID_SQL = """
            SELECT name, url
            FROM social_item
            WHERE resume_id = ?
            ORDER BY sort_order
            """;

    private static final String DELETE_SOCIAL_ITEMS_BY_RESUME_ID_SQL = """
            DELETE FROM social_item
            WHERE resume_id = ?
            """;

    private static final String INSERT_SOCIAL_ITEM_SQL = """
            INSERT INTO social_item (resume_id, name, url, sort_order)
            VALUES (?, ?, ?, ?)
            """;

    private static final String FIND_BIO_SECTIONS_SQL = """
            SELECT id, title
            FROM bio_section
            WHERE resume_id = ?
            ORDER BY sort_order
            """;

    private static final String FIND_BIO_ITEMS_BY_SECTION_SQL = """
            SELECT id, header, title, link, content, logo_label, logo_image_url, logo_link,
                   collapsible, collapsed, ruler, parent_id, tags
            FROM bio_item
            WHERE section_id = ?
            ORDER BY sort_order
            """;

    private static final String DELETE_BIO_SECTIONS_SQL = """
            DELETE FROM bio_section WHERE resume_id = ?
            """;

    private static final String INSERT_BIO_SECTION_SQL = """
            INSERT INTO bio_section (resume_id, title, sort_order) VALUES (?, ?, ?)
            """;

    private static final String INSERT_BIO_ITEM_SQL = """
            INSERT INTO bio_item (section_id, header, title, link, content, logo_label, logo_image_url, logo_link,
                                  collapsible, collapsed, ruler, parent_id, tags, sort_order)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    @Inject
    DataSource dataSource;

    // --- Resume Management ---

    public List<Resume> listResumes() {
        List<Resume> resumes = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(FIND_ALL_RESUMES_SQL);
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
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(INSERT_RESUME_SQL, Statement.RETURN_GENERATED_KEYS)) {
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
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(UPDATE_RESUME_SQL)) {
            ps.setString(1, name);
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            Log.error("Error updating resume", e);
        }
    }

    public void deleteResume(Long id) {
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(DELETE_RESUME_SQL)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            Log.error("Error deleting resume", e);
        }
    }

    // --- Profile ---

    public Profile getProfile(Long resumeId) {
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(FIND_PROFILE_BY_RESUME_ID_SQL)) {
            ps.setLong(1, resumeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Profile(rs.getString("first_name"), rs.getString("last_name"),
                            rs.getString("picture"), rs.getString("job_title"), rs.getString("bio"),
                            rs.getString("city"), rs.getString("country"), rs.getString("phone"),
                            rs.getString("email"), rs.getString("site"));
                }
            }
        } catch (SQLException e) {
            Log.error("Error getting profile", e);
        }
        return new Profile("", "", "", "", "", "", "", "", "", "");
    }

    public void saveProfile(Long resumeId, Profile profile) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psDelete = conn.prepareStatement(DELETE_PROFILE_BY_RESUME_ID_SQL);
                    PreparedStatement psInsert = conn.prepareStatement(INSERT_PROFILE_SQL)) {
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
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(FIND_SOCIAL_ITEMS_BY_RESUME_ID_SQL)) {
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
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psDelete = conn.prepareStatement(DELETE_SOCIAL_ITEMS_BY_RESUME_ID_SQL);
                    PreparedStatement psInsert = conn.prepareStatement(INSERT_SOCIAL_ITEM_SQL)) {
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
        List<Bio.Section> sections = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
                PreparedStatement psSections = conn.prepareStatement(FIND_BIO_SECTIONS_SQL);
                PreparedStatement psItems = conn.prepareStatement(FIND_BIO_ITEMS_BY_SECTION_SQL)) {

            psSections.setLong(1, resumeId);
            try (ResultSet rsSections = psSections.executeQuery()) {
                while (rsSections.next()) {
                    long sectionId = rsSections.getLong("id");
                    String title = rsSections.getString("title");

                    psItems.setLong(1, sectionId);
                    List<Bio.Item> allItems = new ArrayList<>();
                    java.util.Map<Long, Bio.Item> itemMap = new java.util.HashMap<>();
                    java.util.Map<Long, Long> parentMap = new java.util.HashMap<>();

                    try (ResultSet rsItems = psItems.executeQuery()) {
                        while (rsItems.next()) {
                            long id = rsItems.getLong("id");
                            long parentId = rsItems.getLong("parent_id");
                            boolean hasParent = !rsItems.wasNull();
                            String tagsStr = rsItems.getString("tags");
                            List<String> tags = tagsStr != null && !tagsStr.isEmpty()
                                    ? new ArrayList<>(List.of(tagsStr.split(",")))
                                    : new ArrayList<>();

                            Bio.Logo logo = null;
                            if (rsItems.getString("logo_label") != null) {
                                logo = new Bio.Logo(rsItems.getString("logo_label"), rsItems.getString("logo_image_url"),
                                        rsItems.getString("logo_link"));
                            }

                            Bio.Item item = new Bio.Item(
                                    id,
                                    rsItems.getString("header"),
                                    rsItems.getString("title"),
                                    rsItems.getString("link"),
                                    rsItems.getString("content"),
                                    logo,
                                    rsItems.getBoolean("collapsible"),
                                    rsItems.getBoolean("collapsed"),
                                    rsItems.getBoolean("ruler"),
                                    tags,
                                    new ArrayList<>());
                            itemMap.put(id, item);
                            parentMap.put(id, hasParent ? parentId : null);
                            allItems.add(item);
                        }
                    }

                    List<Bio.Item> rootItems = new ArrayList<>();
                    for (Bio.Item item : allItems) {
                        Long parentId = parentMap.get(item.id());
                        if (parentId == null || parentId == 0) {
                            rootItems.add(item);
                        } else {
                            Bio.Item parent = itemMap.get(parentId);
                            if (parent != null) {
                                parent.subItems().add(item);
                            } else {
                                rootItems.add(item);
                            }
                        }
                    }

                    sections.add(new Bio.Section(sectionId, title, rootItems));
                }
            }
        } catch (SQLException e) {
            Log.error("Error getting bio", e);
        }
        return new Bio(sections);
    }

    public void saveBio(Long resumeId, Bio bio) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psDelete = conn.prepareStatement(DELETE_BIO_SECTIONS_SQL);
                    PreparedStatement psInsertSection = conn.prepareStatement(INSERT_BIO_SECTION_SQL,
                            Statement.RETURN_GENERATED_KEYS);
                    PreparedStatement psInsertItem = conn.prepareStatement(INSERT_BIO_ITEM_SQL,
                            Statement.RETURN_GENERATED_KEYS)) {

                psDelete.setLong(1, resumeId);
                psDelete.executeUpdate();

                if (bio.list() != null) {
                    int sectionOrder = 0;
                    for (Bio.Section section : bio.list()) {
                        psInsertSection.setLong(1, resumeId);
                        psInsertSection.setString(2, section.title());
                        psInsertSection.setInt(3, sectionOrder++);
                        psInsertSection.executeUpdate();

                        long sectionId;
                        try (ResultSet rs = psInsertSection.getGeneratedKeys()) {
                            if (rs.next())
                                sectionId = rs.getLong(1);
                            else
                                throw new SQLException("Failed to get section id");
                        }

                        if (section.items() != null) {
                            saveBioItems(psInsertItem, sectionId, null, section.items());
                        }
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            Log.error("Error saving bio", e);
            throw new RuntimeException(e);
        }
    }

    private void saveBioItems(PreparedStatement psInsertItem, long sectionId, Long parentId, List<Bio.Item> items)
            throws SQLException {
        int order = 0;
        for (Bio.Item item : items) {
            psInsertItem.setLong(1, sectionId);
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

            if (item.collapsible() != null)
                psInsertItem.setInt(9, item.collapsible() ? 1 : 0);
            else
                psInsertItem.setNull(9, java.sql.Types.INTEGER);

            if (item.collapsed() != null)
                psInsertItem.setInt(10, item.collapsed() ? 1 : 0);
            else
                psInsertItem.setNull(10, java.sql.Types.INTEGER);

            if (item.ruler() != null)
                psInsertItem.setInt(11, item.ruler() ? 1 : 0);
            else
                psInsertItem.setNull(11, java.sql.Types.INTEGER);

            if (parentId != null)
                psInsertItem.setLong(12, parentId);
            else
                psInsertItem.setNull(12, java.sql.Types.INTEGER);

            String tagsStr = item.tags() != null ? String.join(",", item.tags()) : null;
            psInsertItem.setString(13, tagsStr);
            psInsertItem.setInt(14, order++);

            psInsertItem.executeUpdate();

            long itemId;
            try (ResultSet rs = psInsertItem.getGeneratedKeys()) {
                if (rs.next())
                    itemId = rs.getLong(1);
                else
                    throw new SQLException("Failed to get item id");
            }

            if (item.subItems() != null && !item.subItems().isEmpty()) {
                saveBioItems(psInsertItem, sectionId, itemId, item.subItems());
            }
        }
    }
}
