package kaasu_creator.config;

import javax.sql.DataSource;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

/**
 * DatabaseInitializer — runs schema.sql on startup then patches any
 * missing columns from older schema versions.
 *
 * PostgreSQL 15 (Supabase) supports ADD COLUMN IF NOT EXISTS natively,
 * so we use that instead of try/catch blocks.
 */
@Configuration
public class DatabaseInitializer {

    @Bean
    public CommandLineRunner initializeDatabase(DataSource dataSource) {
        return args -> {
            // Wait up to 60 s for Supabase to wake (free tier pauses after inactivity)
            boolean connected = false;
            for (int attempt = 1; attempt <= 12; attempt++) {
                try (var conn = dataSource.getConnection()) {
                    connected = true;
                    break;
                } catch (Exception e) {
                    System.out.println("DB not ready (attempt " + attempt + "/12), retrying in 5 s…");
                    try { Thread.sleep(5000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                }
            }

            if (!connected) {
                System.out.println("Could not reach database after 60 s — schema init skipped.");
                return;
            }

            try {
                ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
                populator.addScript(new ClassPathResource("schema.sql"));
                populator.setContinueOnError(true);
                populator.setSeparator(";");
                populator.execute(dataSource);
                System.out.println("Schema script executed.");
            } catch (Exception e) {
                System.out.println("Schema script skipped (tables may already exist): " + e.getMessage());
            }

            try {
                patchColumns(dataSource);
            } catch (Exception e) {
                System.out.println("Column patches skipped: " + e.getMessage());
            }

            System.out.println("Database initialization complete.");
        };
    }

    private void patchColumns(DataSource dataSource) {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        // PostgreSQL 9.6+ supports IF NOT EXISTS on ADD COLUMN
        String[] patches = {
            "ALTER TABLE users            ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP",
            "ALTER TABLE expenses         ADD COLUMN IF NOT EXISTS user_id BIGINT",
            "ALTER TABLE expenses         ADD COLUMN IF NOT EXISTS date TIMESTAMP DEFAULT CURRENT_TIMESTAMP",
            "ALTER TABLE goals            ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP",
            "ALTER TABLE timesheet_entries ADD COLUMN IF NOT EXISTS job_id BIGINT REFERENCES jobs(id) ON DELETE SET NULL",
            "ALTER TABLE incomes          ADD COLUMN IF NOT EXISTS category VARCHAR(100)"
        };

        for (String sql : patches) {
            try {
                jdbc.execute(sql);
            } catch (Exception ignored) {
                // Ignore errors — column likely already exists with a constraint conflict
            }
        }
    }
}
