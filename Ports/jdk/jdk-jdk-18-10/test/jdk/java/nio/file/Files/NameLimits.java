/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 8011128
 * @summary Test file and directory name limits. This test is primarily
 *   intended to test Files.createDirectory on resolved paths at or around
 *   the short path limit of 248 on Windows.
 */

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class NameLimits {

    static final int MAX_PATH = 255;
    static final int MIN_PATH = 8;     // arbitrarily chosen

    static Path generatePath(int len) {
        if (len < MIN_PATH)
            throw new RuntimeException("Attempting to generate path less than MIN_PATH");
        StringBuilder sb = new StringBuilder(len);
        sb.append("name");
        while (sb.length() < len) {
            sb.append('X');
        }
        return Paths.get(sb.toString());
    }

    static boolean tryCreateFile(int len) throws IOException {
        Path name = generatePath(len);
        try {
            Files.createFile(name);
        } catch (IOException ioe) {
            System.err.format("Unable to create file of length %d (full path %d), %s%n",
                name.toString().length(), name.toAbsolutePath().toString().length(), ioe);
            return false;
        }
        Files.delete(name);
        return true;
    }

    static boolean tryCreateDirectory(int len) throws IOException {
        Path name = generatePath(len);
        try {
            Files.createDirectory(name);
        } catch (IOException ioe) {
            System.err.format("Unable to create directory of length %d (full path %d), %s%n",
                name.toString().length(), name.toAbsolutePath().toString().length(), ioe);
            return false;
        }
        Files.delete(name);
        return true;
    }

    public static void main(String[] args) throws Exception {
        int len;

        // find the maximum file name if MAX_PATH or less
        len = MAX_PATH;
        while (!tryCreateFile(len)) {
            len--;
        }
        System.out.format("Testing createFile on paths %d .. %d%n", MIN_PATH, len);
        while (len >= MIN_PATH) {
            if (!tryCreateFile(len--))
                throw new RuntimeException("Test failed");
        }

        // find the maximum directory name if MAX_PATH or less
        len = MAX_PATH;
        while (!tryCreateDirectory(len)) {
            len--;
        }
        System.out.format("Testing createDirectory on paths %d .. %d%n", MIN_PATH, len);
        while (len >= MIN_PATH) {
            if (!tryCreateDirectory(len--))
                throw new RuntimeException("Test failed");
        }
    }
}
