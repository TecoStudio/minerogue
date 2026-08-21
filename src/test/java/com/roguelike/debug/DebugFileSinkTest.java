package com.roguelike.debug;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DebugFileSinkTest {
    @Test
    void flushWritesEveryLineAcceptedBeforeFlush() throws Exception {
        Path directory = Files.createTempDirectory("minerogue-debug-sink");
        DebugFileSink sink = new DebugFileSink(directory, 4096, 5, 16);
        try {
            assertTrue(sink.offer("第一条记录"));
            assertTrue(sink.offer("second"));

            sink.flush();

            assertEquals(List.of("第一条记录", "second"), readAllLines(directory));
        } finally {
            sink.close();
        }
    }

    @Test
    void flushReturnsSuccessOnlyWhenQueuedLinesWereWritten() throws Exception {
        Path directory = Files.createTempDirectory("minerogue-debug-flush-result");
        DebugFileSink sink = new DebugFileSink(directory, 4096, 5, 4);
        try {
            assertTrue(sink.offer("flush result"));
            assertTrue(sink.flush());
            assertEquals(List.of("flush result"), readAllLines(directory));
        } finally {
            sink.close();
        }
    }

    @Test
    void flushReportsFailureWhenTheTargetCannotBeWritten() throws Exception {
        Path directory = Files.createTempFile("minerogue-debug-not-directory", ".tmp");
        DebugFileSink sink = new DebugFileSink(directory, 4096, 5, 4);
        try {
            assertTrue(sink.offer("unwritable target"));
            assertFalse(sink.flush());
            assertTrue(sink.failedWrites() > 0);
        } finally {
            sink.close();
            Files.deleteIfExists(directory);
        }
    }

    @Test
    void closeDoesNotLeaveWriterThreadRunning() throws Exception {
        Path directory = Files.createTempDirectory("minerogue-debug-close");
        DebugFileSink sink = new DebugFileSink(directory, 4096, 5, 4);

        sink.offer("line");
        sink.close();
        sink.close();

        assertFalse(sink.offer("after-close"));
    }

    @Test
    void offerIsRejectedAfterClose() throws Exception {
        Path directory = Files.createTempDirectory("minerogue-debug-queue");
        DebugFileSink sink = new DebugFileSink(directory, 4096, 5, 1);

        sink.close();

        assertFalse(sink.offer("after-close"));
    }

    private static List<String> readAllLines(Path directory) throws Exception {
        try (Stream<Path> files = Files.list(directory)) {
            return files.filter(path -> path.getFileName().toString().endsWith(".log"))
                    .flatMap(path -> {
                        try {
                            return Files.readAllLines(path, StandardCharsets.UTF_8).stream();
                        } catch (Exception exception) {
                            throw new RuntimeException(exception);
                        }
                    }).toList();
        }
    }
}
