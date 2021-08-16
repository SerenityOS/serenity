/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.jmx;

import java.nio.file.AccessDeniedException;
import java.nio.file.Path;
import java.nio.file.Paths;

import jdk.management.jfr.FlightRecorderMXBean;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.FileHelper;
import jdk.test.lib.jfr.SimpleEventHelper;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm jdk.jfr.jmx.TestCopyToReadOnlyDir
 */
public class TestCopyToReadOnlyDir {
    public static void main(String[] args) throws Throwable {
        FlightRecorderMXBean bean = JmxHelper.getFlighteRecorderMXBean();

        long recId = bean.newRecording();
        bean.startRecording(recId);
        SimpleEventHelper.createEvent(1);
        bean.stopRecording(recId);

        Path readOnlyDir = FileHelper.createReadOnlyDir(Paths.get(".", "readOnlyDir"));
        System.out.println("readOnlyDir=" + readOnlyDir.toString());
        Asserts.assertTrue(readOnlyDir.toFile().isDirectory(), "Could not create directory. Test error");
        if (!FileHelper.isReadOnlyPath(readOnlyDir)) {
            System.out.println("Failed to create read-only dir. Maybe running as root? Skipping test");
            return;
        }

        Path file = Paths.get(readOnlyDir.toString(), "my.jfr");
        System.out.println("file=" + file.toString());
        try {
            bean.copyTo(recId, file.toString());
            Asserts.fail("Should be able to dump to read only file");
        } catch (AccessDeniedException e) {
            // ok as expected
        }

        bean.closeRecording(recId);
    }
}
