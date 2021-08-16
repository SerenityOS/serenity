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

package jdk.jfr.api.recording.misc;

import static jdk.test.lib.Asserts.assertEquals;
import static jdk.test.lib.Asserts.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import jdk.jfr.Recording;
import jdk.test.lib.jfr.EventNames;

/**
 * @test
 * @summary Test recording file size with Recording.getSize()
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.recording.misc.TestGetSize
 */
public class TestGetSize {

    public static void main(String[] args) throws Throwable {
        Path dest = Paths.get(".", "my.jfr");
        Recording r = new Recording();
        r.setToDisk(true);
        r.enable(EventNames.OSInformation);
        assertEquals(r.getSize(), 0L, "getSize should return 0 before recording starts");
        r.start();
        r.stop();
        r.dump(dest);
        assertTrue(Files.exists(dest), "TestGetSize recording missing: " + dest);
        System.out.printf("%s size: %d%n", dest, Files.size(dest));
        System.out.printf("r.getSize(): %d%n", r.getSize());
        assertEquals(Files.size(dest), r.getSize(), "TestGetSize wrong recording size");
        r.close();
    }

}
