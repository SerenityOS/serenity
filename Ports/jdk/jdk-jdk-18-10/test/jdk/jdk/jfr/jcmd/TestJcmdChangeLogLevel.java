/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.jcmd;



import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import jdk.test.lib.dcmd.JcmdExecutor;
import jdk.test.lib.dcmd.PidJcmdExecutor;

/**
 * @test TestJcmdLogLevelChange
 * @key jfr
 * @summary Test changing log level
 * @requires vm.hasJFR
 *
 * @library /test/lib /test/jdk
 *
 * @run main/othervm -Xlog:jfr=info jdk.jfr.jcmd.TestJcmdChangeLogLevel
 */
public class TestJcmdChangeLogLevel {
    public static void main(String[] args) throws Exception {
        final String fileName = "jfr_trace.txt";
        final String findWhat = "[info][jfr] Flight Recorder initialized";
        boolean passed = false;

        JcmdExecutor je = new PidJcmdExecutor();
        je.execute("VM.log output='file=" + fileName + "' what='jfr=info'");
        je.execute("JFR.start duration=1s");
        List<String> lines;

        do {
            try {
                lines = Files.readAllLines(Paths.get(fileName));
            } catch (IOException e) {
                throw new Error(e);
            }
            for (String l : lines) {
                if (l.toString().contains(findWhat)) {
                    passed = true;
                    break;
                }
            }
            if (lines.size() > 100) {
                break; /* did not find it */
            }
        } while(!passed);

        if (!passed) {
            throw new Error("Not found " + findWhat  + " in stream" + lines);
        }

        System.out.println("PASSED");
    }
}
