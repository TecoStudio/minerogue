package com.roguelike.data;

import com.roguelike.RoguelikePlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class PlayerDataBackupService {
    private static final AtomicBoolean BACKUP_RUNNING = new AtomicBoolean(false);
    private static BackupSettings settings;

    public static void configure(BackupSettings settings) {
        PlayerDataBackupService.settings = settings;
    }

    public static boolean createBackup() {
        BackupSettings current = settings;
        if (current == null) return false;
        if (!BACKUP_RUNNING.compareAndSet(false, true)) {
            current.plugin().getLogger().warning("玩家数据备份已在进行中，跳过本次触发。");
            return false;
        }

        try {
            current.saveAll().run();
            File backupFolder = new File(current.plugin().getDataFolder(), "backups");
            if (!backupFolder.exists() && !backupFolder.mkdirs()) {
                throw new IOException("无法创建备份目录: " + backupFolder.getAbsolutePath());
            }
            String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
            File target = new File(backupFolder, "player-data-" + timestamp + ".zip");
            File sqliteSnapshot = null;
            try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(target))) {
                zipDirectory(current.dataFolder(), current.dataFolder(), zip);
                if (current.sqliteEnabled()) {
                    sqliteSnapshot = current.sqliteSnapshot().create(new File(backupFolder, "sqlite-snapshot-" + timestamp + ".db"));
                    if (sqliteSnapshot.exists() && sqliteSnapshot.isFile()) zipFile(sqliteSnapshot, current.sqliteFile().getName(), zip);
                }
            } finally {
                if (sqliteSnapshot != null) Files.deleteIfExists(sqliteSnapshot.toPath());
            }
            pruneBackups(backupFolder, current.keep());
            current.plugin().getLogger().info("玩家数据备份完成: " + target.getName());
            return true;
        } catch (IOException e) {
            current.plugin().getLogger().warning("玩家数据备份失败: " + e.getMessage());
            return false;
        } finally {
            BACKUP_RUNNING.set(false);
        }
    }

    private static void zipDirectory(File root, File current, ZipOutputStream zip) throws IOException {
        File[] files = current.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                zipDirectory(root, file, zip);
            } else if (file.isFile()) {
                String name = root.toPath().relativize(file.toPath()).toString().replace('\\', '/');
                zipFile(file, "player_data/" + name, zip);
            }
        }
    }

    private static void zipFile(File file, String entryName, ZipOutputStream zip) throws IOException {
        zip.putNextEntry(new ZipEntry(entryName));
        try (FileInputStream input = new FileInputStream(file)) {
            input.transferTo(zip);
        }
        zip.closeEntry();
    }

    private static void pruneBackups(File backupFolder, int keep) throws IOException {
        try (var stream = Files.list(backupFolder.toPath())) {
            var backups = stream
                    .filter(path -> path.getFileName().toString().startsWith("player-data-") && path.getFileName().toString().endsWith(".zip"))
                    .sorted((left, right) -> right.getFileName().toString().compareTo(left.getFileName().toString()))
                    .toList();
            for (int i = keep; i < backups.size(); i++) {
                Files.deleteIfExists(backups.get(i));
            }
        }
    }

    public record BackupSettings(RoguelikePlugin plugin, File dataFolder, File sqliteFile, boolean sqliteEnabled,
                                 int keep, Runnable saveAll, SqliteSnapshot sqliteSnapshot) {
    }

    public interface SqliteSnapshot {
        File create(File target) throws IOException;
    }
}
