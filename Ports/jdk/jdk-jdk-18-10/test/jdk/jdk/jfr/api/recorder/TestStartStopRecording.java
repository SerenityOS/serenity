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

package jdk.jfr.api.recorder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

import jdk.jfr.Configuration;
import jdk.jfr.Recording;
import jdk.test.lib.Utils;

/**
 * @test TestStartStopRecording
 *
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.recorder.TestStartStopRecording
 */
public class TestStartStopRecording {

    public static void main(String... args) throws Exception {
        Configuration defaultConfig = Configuration.getConfiguration("default");
        // Memory
        Recording inMemory = new Recording(defaultConfig);
        inMemory.setToDisk(false);

        inMemory.start();

        Path memoryFile = Utils.createTempFile("start-stop-memory-recording", ".jfr");
        inMemory.dump(memoryFile);
        assertValid(memoryFile, "Not a valid memory file.");
        inMemory.stop();
        inMemory.close();
        // Disk
        Recording toDisk = new Recording(defaultConfig);
        toDisk.setToDisk(true);

        toDisk.start();
        toDisk.stop();
        Path diskFile = Utils.createTempFile("start-stop-disk-recording", ".jfr");
        toDisk.dump(diskFile);
        assertValid(diskFile, "Not a valid disk file.");
        toDisk.close();
    }

    private static void assertValid(Path path, String message) throws IOException {
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new AssertionError(message + " Could not find file " + path);
        }
        if (Files.size(path) == 0) {
            throw new AssertionError(message + " File size = 0 for " + path);
        }
    }
}
