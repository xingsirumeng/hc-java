package jxnu.hc_re.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 应用启动时自动执行轻量数据库迁移（幂等操作）
 */
@Component
public class DatabaseMigration {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMigration.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void migrate() {
        addColumnIfNotExists("submission", "annotated_filepath", "VARCHAR(500)");
        addColumnIfNotExists("submission", "status", "VARCHAR(20) DEFAULT 'submitted'");
        addColumnIfNotExists("submission", "return_reason", "TEXT");
        addColumnIfNotExists("submission", "grade_attachments", "JSONB");

        createTableIfNotExists("deadline_extension",
            "CREATE TABLE deadline_extension (" +
            "id SERIAL PRIMARY KEY, " +
            "assignment_name VARCHAR(50) NOT NULL, " +
            "student_id CHAR(12) NOT NULL, " +
            "extended_end_time TIMESTAMPTZ NOT NULL, " +
            "reason TEXT, " +
            "created_at TIMESTAMPTZ DEFAULT NOW(), " +
            "UNIQUE(assignment_name, student_id), " +
            "FOREIGN KEY (assignment_name) REFERENCES assignment(name) ON DELETE CASCADE, " +
            "FOREIGN KEY (student_id) REFERENCES student(student_id) ON DELETE CASCADE)");

        createTableIfNotExists("assignment_similarity",
            "CREATE TABLE assignment_similarity (" +
            "id SERIAL PRIMARY KEY, " +
            "assignment_name VARCHAR(50) NOT NULL, " +
            "student_id_1 CHAR(12) NOT NULL, " +
            "student_id_2 CHAR(12) NOT NULL, " +
            "similarity_score DECIMAL(5,2), " +
            "detail TEXT, " +
            "created_at TIMESTAMPTZ DEFAULT NOW(), " +
            "UNIQUE(assignment_name, student_id_1, student_id_2))");
    }

    private void addColumnIfNotExists(String table, String column, String type) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns " +
                "WHERE table_name = ? AND column_name = ?",
                Integer.class, table, column
            );
            if (count != null && count == 0) {
                jdbcTemplate.execute(
                    "ALTER TABLE " + table + " ADD COLUMN " + column + " " + type
                );
                log.info("✅ 已自动添加数据库列: {}.{} ({})", table, column, type);
            } else {
                log.info("数据库列已存在: {}.{}", table, column);
            }
        } catch (Exception e) {
            log.error("数据库迁移失败: {}.{} — {}", table, column, e.getMessage());
        }
    }

    private void createTableIfNotExists(String tableName, String createSql) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables " +
                "WHERE table_name = ?",
                Integer.class, tableName
            );
            if (count != null && count == 0) {
                jdbcTemplate.execute(createSql);
                log.info("✅ 已自动创建数据库表: {}", tableName);
            } else {
                log.info("数据库表已存在: {}", tableName);
            }
        } catch (Exception e) {
            log.error("数据库表创建失败: {} — {}", tableName, e.getMessage());
        }
    }
}
