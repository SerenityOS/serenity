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

package jdk.jfr.jvm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * @test TestLogOutput
 * @key jfr
 * @summary Sanity test jfr logging output
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm -Xlog:disable -Xlog:jfr*=trace:file=jfr_trace.txt -XX:StartFlightRecording:duration=1s,filename=recording.jfr jdk.jfr.jvm.TestLogOutput
 */
public class TestLogOutput {
    public static void main(String[] args) throws Exception {
        final String fileName = "jfr_trace.txt";
        final List<String>findWhat = new ArrayList<>();
        findWhat.add("Starting a recording");
        findWhat.add("Flight Recorder initialized");
        boolean passed = false;
        List<String> matches = new ArrayList<String>(findWhat);

        do {
            List<String> lines;
            try {
                lines = Files.readAllLines(Paths.get(fileName));
            } catch (IOException e) {
                throw new Error(e);
            }
            for (String l : lines) {
                for (String m : matches) {
                    if (l.toString().contains(m)) {
                        matches.remove(m);
                        break;
                    }
                }
            }
            if (matches.size() < 1) {
                passed = true;
                break;
            }
            if (lines.size() > 100) {
                break; /* did not find it */
            }
        } while(!passed);

        if (!passed) {
            throw new Error("Not found " + findWhat  + " in stream");
        }

       System.out.println("PASSED");
    }
}
