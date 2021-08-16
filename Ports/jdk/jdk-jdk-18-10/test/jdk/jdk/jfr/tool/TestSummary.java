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

package jdk.jfr.tool;

import java.nio.file.Path;

import jdk.jfr.EventType;
import jdk.jfr.consumer.RecordingFile;
import jdk.test.lib.process.OutputAnalyzer;

/**
 * @test
 * @summary Test jfr info
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm jdk.jfr.tool.TestSummary
 */
public class TestSummary {

    public static void main(String[] args) throws Throwable {
        Path f = ExecuteHelper.createProfilingRecording().toAbsolutePath();
        String file = f.toAbsolutePath().toString();

        OutputAnalyzer output = ExecuteHelper.jfr("summary");
        output.shouldContain("missing file");

        output = ExecuteHelper.jfr("summary", "--wrongOption", file);
        output.shouldContain("too many arguments");

        output = ExecuteHelper.jfr("summary", file);
        try (RecordingFile rf = new RecordingFile(f)) {
            for (EventType t : rf.readEventTypes()) {
                output.shouldContain(t.getName());
            }
        }
        output.shouldContain("Version");
    }
}
