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

package jdk.jfr.api.recording.destination;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import jdk.jfr.Recording;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.FileHelper;

/**
 * @test
 * @summary Set destination to a read-only file. Expects exception.
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.recording.destination.TestDestFileReadOnly
 */
public class TestDestFileReadOnly {

    public static void main(String[] args) throws Throwable {
        Path dest = FileHelper.createReadOnlyFile(Paths.get(".", "readonly.txt"));
        System.out.println("dest=" + dest.toFile().getAbsolutePath());
        if (!FileHelper.isReadOnlyPath(dest)) {
            System.out.println("Failed to create a read-only file. Test ignored.");
            return;
        }

        Recording r = new Recording();
        r.setToDisk(true);
        try {
            r.setDestination(dest);
            Asserts.fail("No exception when destination is read-only");
        } catch (IOException e) {
            // Expected exception
        }
        r.close();
    }

}
