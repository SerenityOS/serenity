/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

package jdk.jfr.event.gc.collection;

import static java.lang.System.gc;
import static java.lang.Thread.sleep;
import static java.util.Set.of;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.IntStream.range;
import static jdk.test.lib.Asserts.assertEquals;
import static jdk.test.lib.Asserts.assertTrue;
import static jdk.test.lib.jfr.Events.fromRecording;
import static sun.hotspot.WhiteBox.getWhiteBox;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import gc.testlibrary.g1.MixedGCProvoker;
import jdk.jfr.Recording;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.EventNames;
import sun.hotspot.WhiteBox;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @requires vm.gc == "G1" | vm.gc == null
 * @library /test/lib /test/jdk /test/hotspot/jtreg
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+AlwaysTenure
 *      -Xms20M -Xmx20M -Xlog:gc=debug,gc+heap*=debug,gc+ergo*=debug,gc+start=debug
 *      -XX:G1MixedGCLiveThresholdPercent=100 -XX:G1HeapWastePercent=0 -XX:G1HeapRegionSize=1m
 *      -XX:+UseG1GC -XX:+UseStringDeduplication
 *      -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *      jdk.jfr.event.gc.collection.TestG1ParallelPhases
 */

public class TestG1ParallelPhases {
    public static List<WeakReference<byte[]>> weakRefs;

    public static void main(String[] args) throws IOException {
        Recording recording = new Recording();
        recording.enable(EventNames.GCPhaseParallel);
        recording.start();

        // create more weak garbage than can fit in this heap (-Xmx20m), will force collection of weak references
        weakRefs = range(1, 30)
            .mapToObj(n -> new WeakReference<>(new byte[1_000_000]))
            .collect(toList()); // force evaluation of lazy stream (all weak refs must be created)

        final var MEG = 1024 * 1024;
        MixedGCProvoker.allocateAndProvokeMixedGC(MEG);
        recording.stop();

        Set<String> usedPhases = fromRecording(recording).stream()
            .map(e -> e.getValue("name").toString())
            .collect(toSet());

        Set<String> allPhases = of(
            "ExtRootScan",
            "ThreadRoots",
            "VM Global",
            "JNI Global",
            "Thread OopStorage",
            "ThreadService OopStorage",
            "JVMTI OopStorage",
            "CLDGRoots",
            "CMRefRoots",
            "MergeER",
            "MergeHCC",
            "MergeRS",
            "MergeLB",
            "ScanHR",
            "CodeRoots",
            "ObjCopy",
            "Termination",
            "RedirtyCards",
            "RecalculateUsed",
            "ResetHotCardCache",
            "FreeCSet",
            "PurgeCodeRoots",
            "UpdateDerivedPointers",
            "EagerlyReclaimHumongousObjects",
            "ClearLoggedCards",
            "MergePSS",
            "NonYoungFreeCSet",
            "YoungFreeCSet",
            "RebuildFreeList",
            "SampleCandidates"
        );

        // Some GC phases may or may not occur depending on environment. Filter them out
        // since we can not reliably guarantee that they occur (or not).
        Set<String> optPhases = of(
            // The following two phases only occur on evacuation failure.
            "RemoveSelfForwardingPtr",
            "RestorePreservedMarks",

            "OptScanHR",
            "OptMergeRS",
            "OptCodeRoots",
            "OptObjCopy"
        );
        usedPhases.removeAll(optPhases);

        assertTrue(usedPhases.equals(allPhases), "Compare events expected and received"
            + ", Not found phases: " + allPhases.stream().filter(p -> !usedPhases.contains(p)).collect(joining(", "))
            + ", Not expected phases: " + usedPhases.stream().filter(p -> !allPhases.contains(p)).collect(joining(", ")));
    }
}
