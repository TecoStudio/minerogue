package com.roguelike.debug;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class DebugBuffer {
    private final int capacity;
    private final Deque<DebugRecord> records = new ArrayDeque<>();
    private long droppedCount;

    public DebugBuffer(int capacity) {
        this.capacity = Math.max(1, capacity);
    }

    public synchronized void add(DebugRecord record) {
        if (records.size() == capacity) { records.removeFirst(); droppedCount++; }
        records.addLast(record);
    }
    public synchronized List<DebugRecord> tail(int count) {
        int limit = Math.max(0, count);
        List<DebugRecord> result = new ArrayList<>(records);
        return result.subList(Math.max(0, result.size() - limit), result.size()).stream().toList();
    }
    public synchronized List<DebugRecord> snapshot() { return List.copyOf(records); }
    public synchronized void clear() {
        records.clear();
        droppedCount = 0L;
    }
    public synchronized int size() { return records.size(); }
    public synchronized long droppedCount() { return droppedCount; }
}
