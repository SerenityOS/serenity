/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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
package jdk.jfr.internal.dcmd;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;

import jdk.jfr.FlightRecorder;
import jdk.jfr.Recording;
import jdk.jfr.internal.PlatformRecorder;
import jdk.jfr.internal.PlatformRecording;
import jdk.jfr.internal.PrivateAccess;
import jdk.jfr.internal.SecuritySupport.SafePath;
import jdk.jfr.internal.Utils;
import jdk.jfr.internal.WriteableUserPath;

/**
 * JFR.dump
 *
 */
// Instantiated by native
final class DCmdDump extends AbstractDCmd {

    @Override
    public void execute(ArgumentParser parser) throws DCmdException {
        parser.checkUnknownArguments();
        String name = parser.getOption("name");
        String filename = expandFilename(parser.getOption("filename"));
        Long maxAge = parser.getOption("maxage");
        Long maxSize = parser.getOption("maxsize");
        String begin = parser.getOption("begin");
        String end = parser.getOption("end");
        Boolean pathToGcRoots = parser.getOption("path-to-gc-roots");

        if (FlightRecorder.getFlightRecorder().getRecordings().isEmpty()) {
            throw new DCmdException("No recordings to dump from. Use JFR.start to start a recording.");
        }

        if (maxAge != null) {
            if (end != null || begin != null) {
                throw new DCmdException("Dump failed, maxage can't be combined with begin or end.");
            }

            if (maxAge < 0) {
                throw new DCmdException("Dump failed, maxage can't be negative.");
            }
            if (maxAge == 0) {
                maxAge = Long.MAX_VALUE / 2; // a high value that won't overflow
            }
        }

        if (maxSize!= null) {
            if (maxSize < 0) {
                throw new DCmdException("Dump failed, maxsize can't be negative.");
            }
            if (maxSize == 0) {
                maxSize = Long.MAX_VALUE / 2; // a high value that won't overflow
            }
        }

        Instant beginTime = parseTime(begin, "begin");
        Instant endTime = parseTime(end, "end");

        if (beginTime != null && endTime != null) {
            if (endTime.isBefore(beginTime)) {
                throw new DCmdException("Dump failed, begin must precede end.");
            }
        }

        Duration duration = null;
        if (maxAge != null) {
            duration = Duration.ofNanos(maxAge);
            beginTime = Instant.now().minus(duration);
        }
        Recording recording = null;
        if (name != null) {
            recording = findRecording(name);
        }
        PlatformRecorder recorder = PrivateAccess.getInstance().getPlatformRecorder();

        try {
            synchronized (recorder) {
                dump(recorder, recording, name, filename, maxSize, pathToGcRoots, beginTime, endTime);
            }
        } catch (IOException | InvalidPathException e) {
            throw new DCmdException("Dump failed. Could not copy recording data. %s", e.getMessage());
        }
    }

    public void dump(PlatformRecorder recorder, Recording recording, String name, String filename, Long maxSize, Boolean pathToGcRoots, Instant beginTime, Instant endTime) throws DCmdException, IOException {
        try (PlatformRecording r = newSnapShot(recorder, recording, pathToGcRoots)) {
            r.filter(beginTime, endTime, maxSize);
            if (r.getChunks().isEmpty()) {
                throw new DCmdException("Dump failed. No data found in the specified interval.");
            }
            // If a filename exist, use it
            // if a filename doesn't exist, use destination set earlier
            // if destination doesn't exist, generate a filename
            WriteableUserPath wup = null;
            if (recording != null) {
                PlatformRecording pRecording = PrivateAccess.getInstance().getPlatformRecording(recording);
                wup = pRecording.getDestination();
            }
            if (filename != null || (filename == null && wup == null) ) {
                SafePath safe = resolvePath(recording, filename);
                wup = new WriteableUserPath(safe.toPath());
            }
            r.dumpStopped(wup);
            reportOperationComplete("Dumped", name, new SafePath(wup.getRealPathText()));
        }
    }

    private Instant parseTime(String time, String parameter) throws DCmdException {
        if (time == null) {
            return null;
        }
        try {
            return Instant.parse(time);
        } catch (DateTimeParseException dtp) {
            // fall through
        }
        try {
            LocalDateTime ldt = LocalDateTime.parse(time);
            return ZonedDateTime.of(ldt, ZoneId.systemDefault()).toInstant();
        } catch (DateTimeParseException dtp) {
            // fall through
        }
        try {
            LocalTime lt = LocalTime.parse(time);
            LocalDate ld = LocalDate.now();
            Instant instant = ZonedDateTime.of(ld, lt, ZoneId.systemDefault()).toInstant();
            Instant now = Instant.now();
            if (instant.isAfter(now) && !instant.isBefore(now.plusSeconds(3600))) {
                // User must have meant previous day
                ld = ld.minusDays(1);
            }
            return ZonedDateTime.of(ld, lt, ZoneId.systemDefault()).toInstant();
        } catch (DateTimeParseException dtp) {
            // fall through
        }

        if (time.startsWith("-")) {
            try {
                long durationNanos = Utils.parseTimespan(time.substring(1));
                Duration duration = Duration.ofNanos(durationNanos);
                return Instant.now().minus(duration);
            } catch (NumberFormatException nfe) {
                // fall through
            }
        }
        throw new DCmdException("Dump failed, not a valid %s time.", parameter);
    }

    private PlatformRecording newSnapShot(PlatformRecorder recorder, Recording recording, Boolean pathToGcRoots) throws DCmdException, IOException {
        if (recording == null) {
            // Operate on all recordings
            PlatformRecording snapshot = recorder.newTemporaryRecording();
            recorder.fillWithRecordedData(snapshot, pathToGcRoots);
            return snapshot;
        }

        PlatformRecording pr = PrivateAccess.getInstance().getPlatformRecording(recording);
        return pr.newSnapshotClone("Dumped by user", pathToGcRoots);
    }

    @Override
    public String[] printHelp() {
            // 0123456789001234567890012345678900123456789001234567890012345678900123456789001234567890
        return """
               Syntax : JFR.dump [options]

               Options:

                 begin           (Optional) Specify the time from which recording data will be included
                                 in the dump file. The format is specified as local time.
                                 (STRING, no default value)

                 end             (Optional) Specify the time to which recording data will be included
                                 in the dump file. The format is specified as local time.
                                 (STRING, no default value)

                                 Note: For both begin and end, the time must be in a format that can
                                 be read by any of these methods:

                                  java.time.LocalTime::parse(String),
                                  java.time.LocalDateTime::parse(String)
                                  java.time.Instant::parse(String)

                                 For example, "13:20:15", "2020-03-17T09:00:00" or
                                 "2020-03-17T09:00:00Z".

                                 Note: begin and end times correspond to the timestamps found within
                                 the recorded information in the flight recording data.

                                 Another option is to use a time relative to the current time that is
                                 specified by a negative integer followed by "s", "m" or "h".
                                 For example, "-12h", "-15m" or "-30s"

                 filename        (Optional) Name of the file to which the flight recording data is
                                 dumped. If no filename is given, a filename is generated from the PID
                                 and the current date. The filename may also be a directory in which
                                 case, the filename is generated from the PID and the current date in
                                 the specified directory. (STRING, no default value)

                                 Note: If a filename is given, '%%p' in the filename will be
                                 replaced by the PID, and '%%t' will be replaced by the time in
                                 'yyyy_MM_dd_HH_mm_ss' format.

                 maxage          (Optional) Length of time for dumping the flight recording data to a
                                 file. (INTEGER followed by 's' for seconds 'm' for minutes or 'h' for
                                 hours, no default value)

                 maxsize         (Optional) Maximum size for the amount of data to dump from a flight
                                 recording in bytes if one of the following suffixes is not used:
                                 'm' or 'M' for megabytes OR 'g' or 'G' for gigabytes.
                                 (STRING, no default value)

                 name            (Optional) Name of the recording. If no name is given, data from all
                                 recordings is dumped. (STRING, no default value)

                 path-to-gc-root (Optional) Flag for saving the path to garbage collection (GC) roots
                                 at the time the recording data is dumped. The path information is
                                 useful for finding memory leaks but collecting it can cause the
                                 application to pause for a short period of time. Turn on this flag
                                 only when you have an application that you suspect has a memory
                                 leak. (BOOLEAN, false)

               Options must be specified using the <key> or <key>=<value> syntax.

               Example usage:

                $ jcmd <pid> JFR.dump
                $ jcmd <pid> JFR.dump filename=recording.jfr
                $ jcmd <pid> JFR.dump filename=%s
                $ jcmd <pid> JFR.dump name=1 filename=%s
                $ jcmd <pid> JFR.dump maxage=1h
                $ jcmd <pid> JFR.dump maxage=1h maxsize=50M
                $ jcmd <pid> JFR.dump fillename=leaks.jfr path-to-gc-root=true
                $ jcmd <pid> JFR.dump begin=13:15
                $ jcmd <pid> JFR.dump begin=13:15 end=21:30:00
                $ jcmd <pid> JFR.dump end=18:00 maxage=10m
                $ jcmd <pid> JFR.dump begin=2021-09-15T09:00:00 end=2021-09-15T10:00:00
                $ jcmd <pid> JFR.dump begin=-1h
                $ jcmd <pid> JFR.dump begin=-15m end=-5m

               """.formatted(exampleDirectory(), exampleFilename()).lines().toArray(String[]::new);
    }

    @Override
    public Argument[] getArgumentInfos() {
        return new Argument[] {
           new Argument("name",
               "Recording name, e.g. \\\"My Recording\\\"",
               "STRING", false, null, false),
           new Argument("filename",
               "Copy recording data to file, e.g. \\\"" + exampleFilename() + "\\\"",
               "STRING", false, null, false),
           new Argument("maxage",
               "Maximum duration to dump, in (s)econds, (m)inutes, (h)ours, or (d)ays, e.g. 60m, or 0 for no limit",
               "NANOTIME", false, null, false),
           new Argument("maxsize", "Maximum amount of bytes to dump, in (M)B or (G)B, e.g. 500M, or 0 for no limit",
               "MEMORY SIZE", false, "hotspot-pid-xxxxx-id-y-YYYY_MM_dd_HH_mm_ss.jfr", false),
           new Argument("begin",
               "Point in time to dump data from, e.g. 09:00, 21:35:00, 2018-06-03T18:12:56.827Z, 2018-06-03T20:13:46.832, -10m, -3h, or -1d",
               "STRING", false, null, false),
           new Argument("end",
               "Point in time to dump data to, e.g. 09:00, 21:35:00, 2018-06-03T18:12:56.827Z, 2018-06-03T20:13:46.832, -10m, -3h, or -1d",
               "STRING", false, null, false),
           new Argument("path-to-gc-roots",
               "Collect path to GC roots",
               "BOOLEAN", false, "false", false)
        };
    }
}

