/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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


/*
 * @test
 *
 * @summary converted from VM Testbase nsk/jvmti/Allocate/alloc001.
 * VM Testbase keywords: [jpda, jvmti, noras, nonconcurrent]
 * VM Testbase readme:
 * DESCRIPTION
 *     The test exercise JVMTI function Allocate(size, memPtr).
 *     The test checks the following:
 *       - if JVMTI_ERROR_NULL_POINTER is returned when memPtr is null
 *       - if allocated memory is available to access
 *       - if JVMTI_ERROR_OUT_OF_MEMORY is returned when there is
 *         insufficient memory available
 * COMMENTS
 *     Ported from JVMDI.
 *
 * @library /vmTestbase
 *          /test/lib
 *
 * @comment Not run on AIX as it does not support ulimit -v
 * @requires os.family != "aix"
 * @run main/native nsk.jvmti.Allocate.alloc001.alloc001
 */

package nsk.jvmti.Allocate.alloc001;

import jdk.test.lib.Platform;
import jdk.test.lib.Utils;
import jdk.test.lib.process.ProcessTools;
import jtreg.SkippedException;

import java.io.File;

class Test {
    static {
        try {
            System.loadLibrary("alloc001");
        } catch (UnsatisfiedLinkError ule) {
            System.err.println("Could not load alloc001 library");
            System.err.println("java.library.path:" + System.getProperty("java.library.path"));
            throw ule;
        }
    }

    native static int check();

    public static void main(String[] args) {
        System.exit(alloc001.STATUS_BASE + check());
    }
}

public class alloc001 {
    public static final int STATUS_BASE = 95;
    private static final int STATUS_PASSED = 0 + STATUS_BASE;
    // FAILED_NO_OOM (as defined in alloc001.cpp) + STATUS_BASE
    private static final int STATUS_NO_OOM = 3 + STATUS_BASE;

    public static void main(String[] args) throws Throwable {
        String cmd = ProcessTools.getCommandLine(ProcessTools.createTestJvm(
                "-XX:MaxHeapSize=" + (Platform.is32bit() ? "256m" : "512m"),
                "-Djava.library.path=" + Utils.TEST_NATIVE_PATH,
                "-agentpath:" + Utils.TEST_NATIVE_PATH + File.separator + System.mapLibraryName("alloc001"),
                "-XX:CompressedClassSpaceSize=64m",
                Test.class.getName()
        ));
        cmd = escapeCmd(cmd);

        int ulimitV = Platform.is32bit() ? 1048576 : 4194303;
        var pb = new ProcessBuilder(
                "sh", "-c",
                "ulimit -v " + ulimitV + "; " + cmd);

        // lower MALLOC_ARENA_MAX b/c we limited virtual memory, see JDK-8043516
        pb.environment().put("MALLOC_ARENA_MAX", "4");

        var oa = ProcessTools.executeCommand(pb);
        int exitCode = oa.getExitValue();
        if (exitCode == STATUS_NO_OOM && (Platform.isWindows() || Platform.isOSX())) {
            throw new SkippedException("Test did not get an OutOfMemory error");
        }
        oa.shouldHaveExitValue(STATUS_PASSED);
    }

    private static String escapeCmd(String cmd) {
        if (Platform.isWindows()) {
            return cmd.replace('\\', '/')
                    .replace(";", "\\;")
                    .replace("|", "\\|");
        }
        return cmd;
    }
}
