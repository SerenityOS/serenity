/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8054029
 * @summary Block devices should not report size=0 on Linux
 */

import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.channels.FileChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;
import static java.nio.file.StandardOpenOption.*;


public class BlockDeviceSize {
    private static final String BLK_FNAME = "/dev/sda1";
    private static final Path BLK_PATH = Paths.get(BLK_FNAME);

    public static void main(String[] args) throws Throwable {
        try (FileChannel ch = FileChannel.open(BLK_PATH, READ);
             RandomAccessFile file = new RandomAccessFile(BLK_FNAME, "r")) {

            long size1 = ch.size();
            long size2 = file.length();
            if (size1 != size2) {
                throw new RuntimeException("size differs when retrieved" +
                        " in different ways: " + size1 + " != " + size2);
            }
            System.out.println("OK");

        } catch (NoSuchFileException nsfe) {
            System.err.println("File " + BLK_FNAME + " not found." +
                    " Skipping test");
        } catch (AccessDeniedException ade) {
            System.err.println("Access to " + BLK_FNAME + " is denied." +
                    " Run test as root.");
        }
    }
}
