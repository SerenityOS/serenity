/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jdk.jfr.Enabled;
import jdk.jfr.RecordingState;
import jdk.jfr.internal.settings.CutoffSetting;
import jdk.jfr.internal.test.WhiteBox;

// The Old Object event could have been implemented as a periodic event, but
// due to chunk rotations and how settings are calculated when multiple recordings
// are running at the same time, it would lead to unacceptable overhead.
//
// Instead, the event is only emitted before a recording stops and
// if that recording has the event enabled.
//
// This requires special handling and the purpose of this class is to provide that
//
public final class OldObjectSample {

    private static final String EVENT_NAME = Type.EVENT_NAME_PREFIX + "OldObjectSample";
    private static final String OLD_OBJECT_CUTOFF = EVENT_NAME + "#" + Cutoff.NAME;
    private static final String OLD_OBJECT_ENABLED = EVENT_NAME + "#" + Enabled.NAME;

    // Emit if old object is enabled in recording with cutoff for that recording
    public static void emit(PlatformRecording recording) {
        if (isEnabled(recording)) {
            long nanos = CutoffSetting.parseValueSafe(recording.getSettings().get(OLD_OBJECT_CUTOFF));
            long ticks = Utils.nanosToTicks(nanos);
            emit(ticks);
        }
    }


    // Emit if old object is enabled for at least one recording, and use the largest
    // cutoff for an enabled recording
    public static void emit(List<PlatformRecording> recordings, Boolean pathToGcRoots) {
        boolean enabled = false;
        long cutoffNanos = Boolean.TRUE.equals(pathToGcRoots) ? Long.MAX_VALUE : 0L;
        for (PlatformRecording r : recordings) {
            if (r.getState() == RecordingState.RUNNING) {
                if (isEnabled(r)) {
                    enabled = true;
                    long c = CutoffSetting.parseValueSafe(r.getSettings().get(OLD_OBJECT_CUTOFF));
                    cutoffNanos = Math.max(c, cutoffNanos);
                }
            }
        }
        if (enabled) {
            long ticks = Utils.nanosToTicks(cutoffNanos);
            emit(ticks);
        }
    }

    private static void emit(long ticks) {
        boolean emitAll = WhiteBox.getWriteAllObjectSamples();
        boolean skipBFS = WhiteBox.getSkipBFS();
        JVM.getJVM().emitOldObjectSamples(ticks, emitAll, skipBFS);
    }

    public static void updateSettingPathToGcRoots(Map<String, String> s, Boolean pathToGcRoots) {
        if (pathToGcRoots != null) {
            s.put(OLD_OBJECT_CUTOFF, pathToGcRoots ? "infinity" : "0 ns");
        }
    }

    public static Map<String, String> createSettingsForSnapshot(PlatformRecording recording, Boolean pathToGcRoots) {
        Map<String, String> settings = new HashMap<>(recording.getSettings());
        updateSettingPathToGcRoots(settings, pathToGcRoots);
        return settings;
    }

    private static boolean isEnabled(PlatformRecording r) {
        Map<String, String> settings = r.getSettings();
        String s = settings.get(OLD_OBJECT_ENABLED);
        return "true".equals(s);
    }
}
