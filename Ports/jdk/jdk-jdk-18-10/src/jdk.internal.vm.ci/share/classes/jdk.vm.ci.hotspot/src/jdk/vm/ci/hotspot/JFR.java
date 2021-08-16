/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.vm.ci.hotspot;

import static jdk.vm.ci.hotspot.CompilerToVM.compilerToVM;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import jdk.vm.ci.meta.ResolvedJavaMethod;

/**
 * Helper methods for interacting with the Java Flight Recorder (JFR) to register events and notify
 * it when events occur. The JFR events are defined in {see @code
 * src/share/jfr/metadata/metadata.xml}.
 */
public final class JFR {

    /**
     * Provides access to current JFR time stamp.
     */
    public static final class Ticks {

        /**
         * @return current JFR time stamp
         */
        public static long now() {
            return compilerToVM().ticksNow();
        }
    }

    /**
     * Helper methods for managing JFR CompilerPhase events.
     * The events are defined in {see @code src/share/jfr/metadata/metadata.xml}.
     */
    public static final class CompilerPhaseEvent {

        private static final ConcurrentHashMap<String, Integer> phaseToId = new ConcurrentHashMap<>();

        private static int getPhaseToId(String phaseName) {
            return phaseToId.computeIfAbsent(phaseName, k -> compilerToVM().registerCompilerPhase(phaseName));
        }

        /**
         * Commits a CompilerPhase event.
         *
         * @param startTime time captured at the start of compiler phase
         * @param phaseName compiler phase name
         * @param compileId current compilation unit id
         * @param phaseLevel compiler phase nesting level
         */
        public static void write(long startTime, String phaseName, int compileId, int phaseLevel) {
            compilerToVM().notifyCompilerPhaseEvent(startTime, getPhaseToId(phaseName), compileId, phaseLevel);
        }
    }

    /**
     * Helper methods for managing JFR CompilerInlining events. The events are defined in {see @code
     * src/share/jfr/metadata/metadata.xml}.
     */
    public static final class CompilerInliningEvent {

        /**
         * Commits a CompilerInlining event.
         *
         * @param compileId current compilation unit id
         * @param caller caller method
         * @param callee callee method
         * @param succeeded inlining succeeded or not
         * @param message extra information on inlining
         * @param bci invocation byte code index
         */
        public static void write(int compileId, ResolvedJavaMethod caller, ResolvedJavaMethod callee, boolean succeeded, String message, int bci) {
            compilerToVM().notifyCompilerInliningEvent(compileId, (HotSpotResolvedJavaMethodImpl) caller, (HotSpotResolvedJavaMethodImpl) callee, succeeded, message, bci);
        }
    }
}
