/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

package gc.arguments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import jdk.test.lib.Platform;
import jdk.test.lib.process.ProcessTools;

/**
 * Helper class for adding options to child processes that should be
 * used by all of the argument tests in this package.  The default
 * options are added at the front, to allow them to be overridden by
 * explicit options from any particular test.
 */

public final class GCArguments {

    // Avoid excessive execution time.
    static private void disableZapUnusedHeapArea(List<String> arguments) {
        // Develop option, only available in debug builds.
        if (Platform.isDebugBuild()) {
            arguments.add("-XX:-ZapUnusedHeapArea");
        }
    }

    // Avoid excessive execution time.
    static private void disableVerifyBeforeExit(List<String> arguments) {
        // Diagnostic option, default enabled in debug builds.
        if (Platform.isDebugBuild()) {
            arguments.add("-XX:-VerifyBeforeExit");
        }
    }

    static private void addDefaults(List<String> arguments) {
        disableZapUnusedHeapArea(arguments);
        disableVerifyBeforeExit(arguments);
    }

    static private String[] withDefaults(String[] arguments) {
        List<String> augmented = new ArrayList<String>();
        addDefaults(augmented);
        Collections.addAll(augmented, arguments);
        return augmented.toArray(new String[augmented.size()]);
    }

    static public ProcessBuilder createJavaProcessBuilder(List<String> arguments) {
        return createJavaProcessBuilder(arguments.toArray(String[]::new));
    }

    static public ProcessBuilder createJavaProcessBuilder(String... arguments) {
        return ProcessTools.createJavaProcessBuilder(withDefaults(arguments));
    }

    static public ProcessBuilder createTestJvm(List<String> arguments) {
        return createTestJvm(arguments.toArray(String[]::new));
    }

    static public ProcessBuilder createTestJvm(String... arguments) {
        return ProcessTools.createTestJvm(withDefaults(arguments));
    }
}
