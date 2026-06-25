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
}
