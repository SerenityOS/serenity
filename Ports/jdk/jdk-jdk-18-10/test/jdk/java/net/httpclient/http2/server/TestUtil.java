/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.nio.file.*;
import java.util.Arrays;

public class TestUtil {

    static final Path CWD = Paths.get(".");
    final static String fileContent = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"; // repeated

    public static Path getAFile(int size) throws IOException {
        Path p = tempFile();
        BufferedWriter writer = Files.newBufferedWriter(p);
        int len = fileContent.length();
        int iterations = size / len;
        int remainder = size - (iterations * len);
        for (int i=0; i<iterations; i++)
            writer.write(fileContent, 0, len);
        writer.write(fileContent, 0, remainder);
        writer.close();
        return p;
    }

    public static Path tempFile() {
        try {
            Path p = Files.createTempFile(CWD, "TestUtil_tmp_", "_HTTPClient");
            return p;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static Void compareFiles(Path path1, Path path2) {
        //System.err.printf("Comparing %s and %s\n", path1.toString(), path2.toString());
        try {
            long size1 = Files.size(path1);
            long size2 = Files.size(path2);
            if (size1 != size2) {
                String msg = "File sizes do not match " +
                        Long.toString(size1) + "/" + Long.toString(size2);
                throw new RuntimeException(msg);
            }
            compareContents(path1, path2);
            return null;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static void compareContents(Path path1, Path path2) {
        try {
            byte[] b1 = Files.readAllBytes(path1);
            byte[] b2 = Files.readAllBytes(path2);
            if (!Arrays.equals(b1, b2))
                throw new RuntimeException ("Files do not match");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
