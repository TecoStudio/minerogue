package com.roguelike.debug;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public final class DebugFileSink implements AutoCloseable {
    private final Path directory;
    private final long maxBytes;
    private final int keepFiles;
    private final int queueCapacity;
    private final Deque<QueuedLine> queue;
    private final Thread worker;
    private volatile boolean running = true;
    private final AtomicLong failedWrites = new AtomicLong();
    private long offeredSequence;
    private long completedSequence;
    private final Set<Long> failedSequences = new HashSet<>();
    private BufferedWriter writer;
    private Path currentFile;
    private long currentBytes;

    public DebugFileSink(Path directory, long maxBytes, int keepFiles, int queueCapacity) {
        this.directory = directory;
        this.maxBytes = Math.max(1L, maxBytes);
        this.keepFiles = Math.max(1, keepFiles);
        this.queueCapacity = Math.max(1, queueCapacity);
        this.queue = new ArrayDeque<>(this.queueCapacity);
        this.worker = new Thread(this::run, "minerogue-debug-writer");
        this.worker.setDaemon(true);
        this.worker.start();
    }

    public boolean offer(String line) {
        if (!running || line == null) return false;
        synchronized (queue) {
            if (!running || queue.size() >= queueCapacity) return false;
            queue.addLast(new QueuedLine(++offeredSequence, line));
            queue.notifyAll();
            return true;
        }
    }

    public long failedWrites() { return failedWrites.get(); }

    public boolean flush() {
        final long target;
        synchronized (queue) {
            target = offeredSequence;
            queue.notifyAll();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
            while (completedSequence < target && worker.isAlive()) {
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) break;
                try {
                    TimeUnit.NANOSECONDS.timedWait(queue, remaining);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            boolean failed = false;
            for (long sequence = 1; sequence <= target; sequence++) {
                if (failedSequences.contains(sequence)) {
                    failed = true;
                    break;
                }
            }
            return completedSequence >= target && !failed;
        }
    }

    @Override
    public void close() {
        synchronized (queue) {
            running = false;
            queue.notifyAll();
        }
        try {
            worker.join(2_000L);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
        if (worker.isAlive()) {
            synchronized (queue) {
                failedWrites.addAndGet(queue.size());
                queue.clear();
                queue.notifyAll();
            }
        }
    }

    private void run() {
        try {
            while (running || hasQueuedLines()) {
                QueuedLine queued = take();
                if (queued == null) continue;
                boolean writeFailed = false;
                try {
                    writeFailed = !writeLine(queued.line());
                } catch (RuntimeException ignored) {
                    writeFailed = true;
                } finally {
                    synchronized (queue) {
                        if (writeFailed) failedSequences.add(queued.sequence());
                        completedSequence = Math.max(completedSequence, queued.sequence());
                        queue.notifyAll();
                    }
                }
            }
        } finally {
            closeWriter();
            synchronized (queue) {
                queue.notifyAll();
            }
        }
    }

    private QueuedLine take() {
        synchronized (queue) {
            while (queue.isEmpty() && running) {
                try {
                    queue.wait(250L);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    running = false;
                    break;
                }
            }
            return queue.pollFirst();
        }
    }

    private boolean hasQueuedLines() {
        synchronized (queue) {
            return !queue.isEmpty();
        }
    }

    private boolean writeLine(String line) {
        try {
            Files.createDirectories(directory);
            byte[] bytes = (line + System.lineSeparator()).getBytes(StandardCharsets.UTF_8);
            if (writer == null || currentBytes + bytes.length > maxBytes) rotate(bytes.length);
            writer.write(line);
            writer.newLine();
            writer.flush();
            currentBytes += bytes.length;
            return true;
        } catch (Exception ignored) {
            failedWrites.incrementAndGet();
            closeWriter();
            return false;
        }
    }

    private void rotate(long nextBytes) throws IOException {
        closeWriter();
        currentFile = directory.resolve("debug-" + System.currentTimeMillis() + ".log");
        writer = Files.newBufferedWriter(currentFile, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        currentBytes = Files.size(currentFile);
        if (currentBytes + nextBytes > maxBytes && currentBytes > 0) {
            closeWriter();
            currentFile = directory.resolve("debug-" + System.currentTimeMillis() + "-" + System.nanoTime() + ".log");
            writer = Files.newBufferedWriter(currentFile, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            currentBytes = 0;
        }
        pruneFiles();
    }

    private void pruneFiles() {
        try (Stream<Path> files = Files.list(directory)) {
            files.filter(path -> path.getFileName().toString().startsWith("debug-") && path.toString().endsWith(".log"))
                    .sorted(Comparator.comparingLong(this::modified).reversed()).skip(keepFiles).forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                            failedWrites.incrementAndGet();
                        }
                    });
        } catch (IOException ignored) {
            failedWrites.incrementAndGet();
        }
    }

    private long modified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException ignored) {
            return 0L;
        }
    }

    private void closeWriter() {
        if (writer == null) return;
        try {
            writer.close();
        } catch (IOException ignored) {
            failedWrites.incrementAndGet();
        } finally {
            writer = null;
        }
    }

    private record QueuedLine(long sequence, String line) {}
}
