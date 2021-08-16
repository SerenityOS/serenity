/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.api.recording.dump;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import jdk.jfr.Recording;
import jdk.test.lib.jfr.CommonHelper;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.FileHelper;
import jdk.test.lib.jfr.VoidFunction;

/**
 * @test
 * @summary Test copyTo and parse file
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.recording.dump.TestDumpReadOnly
 */
public class TestDumpReadOnly {

    private static final String OS_INFORMATION = EventNames.OSInformation;

    public static void main(String[] args) throws Throwable {

        Path readOnlyDir = FileHelper.createReadOnlyDir(Paths.get(".", "readonlydir"));
        if (!FileHelper.isReadOnlyPath(readOnlyDir)) {
            System.out.println("Failed to create read-only path. Maybe running a root? Test skipped");
            return;
        }
        Recording r = new Recording();
        r.enable(OS_INFORMATION);
        r.start();
        r.stop();
        Path path = Paths.get(readOnlyDir.toString(), "my.jfr");
        verifyException(()->{r.dump(path);}, "No Exception when dumping read-only dir");
        r.close();
    }

    private static void verifyException(VoidFunction f, String msg) throws Throwable {
        CommonHelper.verifyException(f, msg, IOException.class);
    }

}
