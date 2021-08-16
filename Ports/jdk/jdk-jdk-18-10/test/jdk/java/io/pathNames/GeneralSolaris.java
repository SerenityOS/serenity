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
   @bug 4035924 4095767
   @summary General exhaustive test of solaris pathname handling
   @author Mark Reinhold

   @build General GeneralSolaris
   @run main GeneralSolaris
 */

import java.io.*;
import java.util.*;
import java.nio.file.*;
import java.nio.file.attribute.*;

public class GeneralSolaris extends General {

    private static void checkUnreadable() throws Exception {
        Path file = Paths.get(baseDir, "unreadableFile");
        Path dir = Paths.get(baseDir, "unreadableDir");
        Set<PosixFilePermission> perms = PosixFilePermissions.fromString("---------");
        FileAttribute<Set<PosixFilePermission>> attr = PosixFilePermissions.asFileAttribute(perms);
        Files.createFile(file, attr);
        Files.createDirectory(dir, attr);

        String unreadableFile = file.toString();
        String unreadableDir = dir.toString();

        checkSlashes(2, false, unreadableDir, unreadableDir);
        checkSlashes(2, false, unreadableFile, unreadableFile);

        Files.delete(file);
        Files.delete(dir);
    }

    private static void checkPaths() throws Exception {
        // Make sure that an empty relative path is tested
        checkNames(1, true, userDir + File.separator, "");
        checkNames(3, true, baseDir + File.separator,
                   relative + File.separator);

        checkSlashes(2, true, baseDir, baseDir);
    }

    public static void main(String[] args) throws Exception {
        if (File.separatorChar != '/') {
            /* This test is only valid on Unix systems */
            return;
        }
        if (args.length > 0) debug = true;

        initTestData(3);
        checkUnreadable();
        checkPaths();
    }
}
