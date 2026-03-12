package io.quarkiverse.roq.theme.resume.editor.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

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

    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]+");

    @Inject
    DataSource dataSource;

    public List<ResumeSummary> listResumes() {
        return searchResumes(null);
    }

    public List<ResumeSummary> searchResumes(String query) {
        String normalizedQuery = normalizeQuery(query);
        String sql = """
                SELECT r.id, r.slug, r.title, p.first_name, p.last_name, p.job_title
                FROM resume r
                LEFT JOIN profile p ON p.resume_id = r.id
                WHERE (? IS NULL OR LOWER(r.title) LIKE ? OR LOWER(r.slug) LIKE ?
                    OR LOWER(COALESCE(p.first_name, '') || ' ' || COALESCE(p.last_name, '')) LIKE ?
                    OR LOWER(COALESCE(p.job_title, '')) LIKE ?)
                ORDER BY LOWER(r.title), r.id
                """;
        List<ResumeSummary> resumes = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            if (normalizedQuery == null) {
                ps.setObject(1, null);
                ps.setObject(2, null);
                ps.setObject(3, null);
                ps.setObject(4, null);
                ps.setObject(5, null);
            } else {
                String like = "%" + normalizedQuery + "%";
                ps.setString(1, normalizedQuery);
                ps.setString(2, like);
                ps.setString(3, like);
                ps.setString(4, like);
                ps.setString(5, like);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resumes.add(new ResumeSummary(
                            rs.getLong("id"),
                            rs.getString("slug"),
                            rs.getString("title"),
                            fullName(rs.getString("first_name"), rs.getString("last_name")),
                            rs.getString("job_title")));
                }
            }
        } catch (SQLException e) {
            Log.error(e);
            throw new RuntimeException(e);
        }
        return resumes;
    }

    public Optional<ResumeSummary> findResume(Long resumeId) {
        if (resumeId == null) {
            return Optional.empty();
        }
        String sql = """
                SELECT r.id, r.slug, r.title, p.first_name, p.last_name, p.job_title
                FROM resume r
                LEFT JOIN profile p ON p.resume_id = r.id
                WHERE r.id = ?
                LIMIT 1
                """;
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, resumeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new ResumeSummary(
                        rs.getLong("id"),
                        rs.getString("slug"),
                        rs.getString("title"),
                        fullName(rs.getString("first_name"), rs.getString("last_name")),
                        rs.getString("job_title")));
            }
        } catch (SQLException e) {
            Log.error(e);
            throw new RuntimeException(e);
        }
    }

    public Resume createResume(String title) {
        String normalizedTitle = title == null || title.isBlank() ? "Untitled CV" : title.trim();
        String slug = nextSlug(normalizedTitle);
        String insertResumeSql = "INSERT INTO resume (slug, title, created_at, updated_at) VALUES (?, ?, ?, ?)";
        String insertProfileSql = """
                INSERT INTO profile (
                    first_name, last_name, picture, job_title, bio, city, country, phone, email, site, resume_id
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        Instant now = Instant.now();
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement insertResume = conn.prepareStatement(insertResumeSql, Statement.RETURN_GENERATED_KEYS);
                    PreparedStatement insertProfile = conn.prepareStatement(insertProfileSql)) {
                insertResume.setString(1, slug);
                insertResume.setString(2, normalizedTitle);
                insertResume.setString(3, now.toString());
                insertResume.setString(4, now.toString());
                insertResume.executeUpdate();

                Integer resumeId = generatedId(insertResume);
                if (resumeId == null) {
                    throw new SQLException("Unable to create resume");
                }

                insertProfile.setString(1, "");
                insertProfile.setString(2, "");
                insertProfile.setString(3, "");
                insertProfile.setString(4, "");
                insertProfile.setString(5, "");
                insertProfile.setString(6, "");
                insertProfile.setString(7, "");
                insertProfile.setString(8, "");
                insertProfile.setString(9, "");
                insertProfile.setString(10, "");
                insertProfile.setInt(11, resumeId);
                insertProfile.executeUpdate();
                conn.commit();
                return new Resume((long) resumeId, slug, normalizedTitle, emptyProfile(), new Bio(List.of()),
                        new Social(List.of()));
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            Log.error(e);
            throw new RuntimeException(e);
        }
    }

    // --- Profile ---

    public Profile getProfile(Long resumeId) {
        if (resumeId == null) {
            return emptyProfile();
        }
        String sql = """
                SELECT first_name, last_name, picture, job_title, bio, city, country, phone, email, site
                FROM profile
                WHERE resume_id = ?
                ORDER BY id
                LIMIT 1
                """;
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, resumeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Profile(rs.getString("first_name"), rs.getString("last_name"), rs.getString("picture"),
                            rs.getString("job_title"), rs.getString("bio"), rs.getString("city"),
                            rs.getString("country"), rs.getString("phone"), rs.getString("email"),
                            rs.getString("site"));
                }
            }
        } catch (SQLException e) {
            Log.error(e);
            throw new RuntimeException(e);
        }
        return emptyProfile();
    }

    public void saveProfile(Long resumeId, Profile profile) {
        if (resumeId == null) {
            return;
        }
        String deleteSql = "DELETE FROM profile WHERE resume_id = ?";
        String insertSql = """
                INSERT INTO profile (first_name, last_name, picture, job_title, bio, city, country, phone, email, site, resume_id)
                VALUES (?,?,?,?,?,?,?,?,?,?,?)
                """;
        String updateResumeSql = "UPDATE resume SET title = ?, updated_at = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psDelete = conn.prepareStatement(deleteSql);
                    PreparedStatement psInsert = conn.prepareStatement(insertSql);
                    PreparedStatement updateResume = conn.prepareStatement(updateResumeSql)) {
                psDelete.setLong(1, resumeId);
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
                psInsert.setLong(11, resumeId);
                psInsert.execute();
                updateResume.setString(1, suggestedResumeTitle(profile));
                updateResume.setString(2, Instant.now().toString());
                updateResume.setLong(3, resumeId);
                updateResume.executeUpdate();
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

    // --- Social ---

    public Social getSocial(Long resumeId) {
        List<Social.Item> items = new ArrayList<>();
        if (resumeId == null) {
            return new Social(items);
        }
        String sql = "SELECT name, url FROM social_item WHERE resume_id = ? ORDER BY sort_order, id";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, resumeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(new Social.Item(rs.getString("name"), rs.getString("url")));
                }
            }
        } catch (SQLException e) {
            Log.error(e);
            throw new RuntimeException(e);
        }
        return new Social(items);
    }

    public void saveSocial(Long resumeId, Social social) {
        if (resumeId == null) {
            return;
        }
        String deleteSql = "DELETE FROM social_item WHERE resume_id = ?";
        String insertSql = "INSERT INTO social_item (name, url, sort_order, resume_id) VALUES (?,?,?,?)";
        String updateResumeSql = "UPDATE resume SET updated_at = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psDelete = conn.prepareStatement(deleteSql);
                    PreparedStatement psInsert = conn.prepareStatement(insertSql);
                    PreparedStatement updateResume = conn.prepareStatement(updateResumeSql)) {
                psDelete.setLong(1, resumeId);
                psDelete.execute();
                if (social.items() != null) {
                    int order = 0;
                    for (Social.Item item : social.items()) {
                        psInsert.setString(1, item.name());
                        psInsert.setString(2, item.url());
                        psInsert.setInt(3, order++);
                        psInsert.setLong(4, resumeId);
                        psInsert.addBatch();
                    }
                    psInsert.executeBatch();
                }
                updateResume.setString(1, Instant.now().toString());
                updateResume.setLong(2, resumeId);
                updateResume.executeUpdate();
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

    public Bio getBio(Long resumeId) {
        if (resumeId == null) {
            return new Bio(List.of());
        }
        String sectionsSql = "SELECT id, title FROM bio_section WHERE resume_id = ? ORDER BY sort_order, id";
        String itemsSql = """
                SELECT id, section_id, header, title, link, content, logo_label, logo_image_url, logo_link,
                       collapsible, collapsed, ruler, parent_id
                FROM bio_item
                WHERE section_id IN (SELECT id FROM bio_section WHERE resume_id = ?)
                ORDER BY sort_order, id
                """;

        try (Connection conn = dataSource.getConnection()) {
            Map<Integer, SectionDraft> sectionsById = new LinkedHashMap<>();
            try (PreparedStatement ps = conn.prepareStatement(sectionsSql)) {
                ps.setLong(1, resumeId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        sectionsById.put(rs.getInt("id"), new SectionDraft(rs.getString("title"), new ArrayList<>()));
                    }
                }
            }

            Map<Integer, Bio.Item> itemsById = new LinkedHashMap<>();
            Map<Integer, Integer> itemSectionIds = new LinkedHashMap<>();
            Map<Integer, Integer> itemParentIds = new LinkedHashMap<>();

            try (PreparedStatement ps = conn.prepareStatement(itemsSql)) {
                ps.setLong(1, resumeId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Integer itemId = rs.getInt("id");
                        Integer parentId = (Integer) rs.getObject("parent_id");
                        Integer sectionId = rs.getInt("section_id");
                        Bio.Logo logo = rs.getString("logo_label") != null || rs.getString("logo_image_url") != null
                                || rs.getString("logo_link") != null
                                        ? new Bio.Logo(rs.getString("logo_label"), rs.getString("logo_image_url"),
                                                rs.getString("logo_link"))
                                        : null;

                        Bio.Item item = new Bio.Item(rs.getString("header"), rs.getString("title"), rs.getString("link"),
                                rs.getString("content"), logo, toBoolean(rs.getObject("collapsible")),
                                toBoolean(rs.getObject("collapsed")), toBoolean(rs.getObject("ruler")), new ArrayList<>());

                        itemsById.put(itemId, item);
                        itemSectionIds.put(itemId, sectionId);
                        itemParentIds.put(itemId, parentId);
                    }
                }
            }

            for (Map.Entry<Integer, Bio.Item> entry : itemsById.entrySet()) {
                Integer itemId = entry.getKey();
                Bio.Item item = entry.getValue();
                Integer parentId = itemParentIds.get(itemId);
                if (parentId != null) {
                    Bio.Item parent = itemsById.get(parentId);
                    if (parent != null && parent.subItems() != null) {
                        parent.subItems().add(item);
                    }
                } else {
                    SectionDraft section = sectionsById.get(itemSectionIds.get(itemId));
                    if (section != null) {
                        section.items().add(item);
                    }
                }
            }

            List<Bio.Section> sections = sectionsById.values().stream()
                    .map(section -> new Bio.Section(section.title(), section.items()))
                    .toList();
            return new Bio(sections);
        } catch (SQLException e) {
            Log.error(e);
            throw new RuntimeException(e);
        }
    }

    public void saveBio(Long resumeId, Bio bio) {
        if (resumeId == null) {
            return;
        }
        String deleteItemsSql = "DELETE FROM bio_item WHERE section_id IN (SELECT id FROM bio_section WHERE resume_id = ?)";
        String deleteSectionsSql = "DELETE FROM bio_section WHERE resume_id = ?";
        String insertSectionSql = "INSERT INTO bio_section (title, sort_order, resume_id) VALUES (?, ?, ?)";
        String insertItemSql = """
                INSERT INTO bio_item (
                    section_id, header, title, link, content, logo_label, logo_image_url, logo_link,
                    collapsible, collapsed, ruler, parent_id, sort_order
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        String updateResumeSql = "UPDATE resume SET updated_at = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement deleteItems = conn.prepareStatement(deleteItemsSql);
                    PreparedStatement deleteSections = conn.prepareStatement(deleteSectionsSql);
                    PreparedStatement insertSection = conn.prepareStatement(insertSectionSql,
                            Statement.RETURN_GENERATED_KEYS);
                    PreparedStatement insertItem = conn.prepareStatement(insertItemSql, Statement.RETURN_GENERATED_KEYS);
                    PreparedStatement updateResume = conn.prepareStatement(updateResumeSql)) {
                deleteItems.setLong(1, resumeId);
                deleteItems.execute();
                deleteSections.setLong(1, resumeId);
                deleteSections.execute();

                if (bio.list() != null) {
                    for (int sectionIndex = 0; sectionIndex < bio.list().size(); sectionIndex++) {
                        Bio.Section section = bio.list().get(sectionIndex);
                        insertSection.setString(1, section.title());
                        insertSection.setInt(2, sectionIndex);
                        insertSection.setLong(3, resumeId);
                        insertSection.executeUpdate();

                        Integer sectionId = generatedId(insertSection);
                        if (sectionId == null) {
                            throw new SQLException("Unable to create bio section");
                        }
                        saveBioItems(insertItem, section.items(), sectionId, null);
                    }
                }
                updateResume.setString(1, Instant.now().toString());
                updateResume.setLong(2, resumeId);
                updateResume.executeUpdate();
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

    private void saveBioItems(PreparedStatement insertItem, List<Bio.Item> items, int sectionId, Integer parentId)
            throws SQLException {
        if (items == null) {
            return;
        }
        for (int itemIndex = 0; itemIndex < items.size(); itemIndex++) {
            Bio.Item item = items.get(itemIndex);
            Bio.Logo logo = item.logo();

            insertItem.setInt(1, sectionId);
            insertItem.setString(2, item.header());
            insertItem.setString(3, item.title());
            insertItem.setString(4, item.link());
            insertItem.setString(5, item.content());
            insertItem.setString(6, logo != null ? logo.label() : null);
            insertItem.setString(7, logo != null ? logo.imageUrl() : null);
            insertItem.setString(8, logo != null ? logo.link() : null);
            insertItem.setInt(9, Boolean.TRUE.equals(item.collapsible()) ? 1 : 0);
            insertItem.setInt(10, Boolean.TRUE.equals(item.collapsed()) ? 1 : 0);
            insertItem.setInt(11, Boolean.TRUE.equals(item.ruler()) ? 1 : 0);
            if (parentId != null) {
                insertItem.setInt(12, parentId);
            } else {
                insertItem.setObject(12, null);
            }
            insertItem.setInt(13, itemIndex);
            insertItem.executeUpdate();

            Integer itemId = generatedId(insertItem);
            if (itemId == null) {
                throw new SQLException("Unable to create bio item");
            }
            saveBioItems(insertItem, item.subItems(), sectionId, itemId);
        }
    }

    private Integer generatedId(PreparedStatement statement) throws SQLException {
        try (ResultSet keys = statement.getGeneratedKeys()) {
            if (keys.next()) {
                return keys.getInt(1);
            }
            return null;
        }
    }

    private Boolean toBoolean(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        return Boolean.valueOf(String.valueOf(value));
    }

    private String normalizeQuery(String query) {
        if (query == null) {
            return null;
        }
        String normalized = query.trim().toLowerCase();
        return normalized.isBlank() ? null : normalized;
    }

    private String fullName(String firstName, String lastName) {
        String fullName = ((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName)).trim();
        return fullName.isBlank() ? null : fullName;
    }

    private String suggestedResumeTitle(Profile profile) {
        String fullName = fullName(profile.firstName(), profile.lastName());
        if (fullName != null) {
            return fullName;
        }
        if (profile.jobTitle() != null && !profile.jobTitle().isBlank()) {
            return profile.jobTitle().trim();
        }
        return "Untitled CV";
    }

    private String nextSlug(String title) {
        String base = slugify(title);
        String candidate = base;
        int suffix = 2;
        while (slugExists(candidate)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }

    private boolean slugExists(String slug) {
        String sql = "SELECT 1 FROM resume WHERE slug = ? LIMIT 1";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, slug);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            Log.error(e);
            throw new RuntimeException(e);
        }
    }

    private String slugify(String value) {
        String ascii = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase();
        String slug = NON_ALNUM.matcher(ascii).replaceAll("-").replaceAll("(^-|-$)", "");
        return slug.isBlank() ? "cv" : slug;
    }

    private Profile emptyProfile() {
        return new Profile("", "", "", "", "", "", "", "", "", "");
    }

    private record SectionDraft(String title, List<Bio.Item> items) {
    }
}
