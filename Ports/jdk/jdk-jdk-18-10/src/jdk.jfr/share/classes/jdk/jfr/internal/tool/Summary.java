/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package jdk.jfr.internal.tool;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import jdk.jfr.EventType;
import jdk.jfr.internal.MetadataDescriptor;
import jdk.jfr.internal.Type;
import jdk.jfr.internal.consumer.ChunkHeader;
import jdk.jfr.internal.consumer.FileAccess;
import jdk.jfr.internal.consumer.RecordingInput;

final class Summary extends Command {
    private final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withLocale(Locale.UK).withZone(ZoneOffset.UTC);

    @Override
    public String getName() {
        return "summary";
    }

    private static class Statistics {
        Statistics(String name) {
            this.name = name;
        }
        String name;
        long count;
        long size;
    }

    @Override
    public List<String> getOptionSyntax() {
        return List.of("<file>");
    }

    @Override
    public void displayOptionUsage(PrintStream stream) {
        stream.println("  <file>   Location of the recording file (.jfr) to display information about");
    }

    @Override
    public String getDescription() {
        return "Display general information about a recording file (.jfr)";
    }

    @Override
    public void execute(Deque<String> options) throws UserSyntaxException, UserDataException {
        ensureMaxArgumentCount(options, 1);
        Path p = getJFRInputFile(options);
        try {
            printInformation(p);
        } catch (IOException e) {
            couldNotReadError(p, e);
        }
    }

    private void printInformation(Path p) throws IOException {
        long totalDuration = 0;
        long chunks = 0;

        try (RecordingInput input = new RecordingInput(p.toFile(), FileAccess.UNPRIVILEGED)) {
            ChunkHeader first = new ChunkHeader(input);
            ChunkHeader ch = first;
            String eventPrefix = Type.EVENT_NAME_PREFIX;
            if (first.getMajor() == 1) {
                eventPrefix = "com.oracle.jdk.";
            }
            HashMap<Long, Statistics> stats = new HashMap<>();
            stats.put(0L, new Statistics(eventPrefix + "Metadata"));
            stats.put(1L, new Statistics(eventPrefix + "CheckPoint"));
            int minWidth = 0;
            while (true) {
                long chunkEnd = ch.getEnd();
                MetadataDescriptor md = ch.readMetadata();

                for (EventType eventType : md.getEventTypes()) {
                    stats.computeIfAbsent(eventType.getId(), (e) -> new Statistics(eventType.getName()));
                    minWidth = Math.max(minWidth, eventType.getName().length());
                }

                totalDuration += ch.getDurationNanos();
                chunks++;
                input.position(ch.getEventStart());
                while (input.position() < chunkEnd) {
                    long pos = input.position();
                    int size = input.readInt();
                    long eventTypeId = input.readLong();
                    Statistics s = stats.get(eventTypeId);
                    if (s == null) {
                        s = new Statistics(eventTypeId + " (missing event metadata)");
                        stats.put(eventTypeId, s);
                    }
                    if (s != null) {
                        s.count++;
                        s.size += size;
                    }
                    input.position(pos + size);
                }
                if (ch.isLastChunk()) {
                    break;
                }
                ch = ch.nextHeader();
            }
            println();
            long epochSeconds = first.getStartNanos() / 1_000_000_000L;
            long adjustNanos = first.getStartNanos() - epochSeconds * 1_000_000_000L;
            println(" Version: " + first.getMajor() + "." + first.getMinor());
            println(" Chunks: " + chunks);
            println(" Start: " + DATE_FORMAT.format(Instant.ofEpochSecond(epochSeconds, adjustNanos)) + " (UTC)");
            println(" Duration: " + (totalDuration + 500_000_000) / 1_000_000_000 + " s");
            List<Statistics> statsList = new ArrayList<>(stats.values());
            Collections.sort(statsList, (u, v) -> Long.compare(v.count, u.count));
            println();
            String header = "      Count  Size (bytes) ";
            String typeHeader = " Event Type";
            minWidth = Math.max(minWidth, typeHeader.length());
            println(typeHeader + pad(minWidth - typeHeader.length(), ' ') + header);
            println(pad(minWidth + header.length(), '='));
            for (Statistics s : statsList) {
                System.out.printf(" %-" + minWidth + "s%10d  %12d\n", s.name, s.count, s.size);
            }
        }
    }

    private String pad(int count, char c) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(c);
        }
        return sb.toString();
    }
}
