/*
 * Copyright (c) 2005, 2012, Oracle and/or its affiliates. All rights reserved.
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
   @bug 4167472 5097703 6216563 6284003 6728842 6464744
   @summary Basic test for setWritable/Readable/Executable methods
   @build SetAccess Util
   @run main SetAccess
 */

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;

public class SetAccess {
    public static void main(String[] args) throws Exception {
        if (Util.isPrivileged()) {
            System.out.println("Unable to test file permissions when running with privileges");
            return;
        }

        File d = new File(System.getProperty("test.dir", "."));

        File f = new File(d, "x.SetAccessPermission");
        if (f.exists() && !f.delete())
            throw new Exception("Can't delete test file: " + f);
        OutputStream o = new FileOutputStream(f);
        o.write('x');
        o.close();
        doTest(f);

        f = new File(d, "x.SetAccessPermission.dir");
        if (f.exists() && !f.delete())
            throw new Exception("Can't delete test dir: " + f);
        if (!f.mkdir())
            throw new Exception(f + ": Cannot create directory");
        doTest(f);
    }

    public static void doTest(File f) throws Exception {
        if (!System.getProperty("os.name").startsWith("Windows")) {
            if (!f.setReadOnly())
                 throw new Exception(f + ": setReadOnly Failed");
            if (!f.setWritable(true, true) ||
                !f.canWrite() ||
                permission(f).charAt(2) != 'w')
                throw new Exception(f + ": setWritable(true, ture) Failed");
            if (!f.setWritable(false, true) ||
                f.canWrite() ||
                permission(f).charAt(2) != '-')
                throw new Exception(f + ": setWritable(false, true) Failed");
            if (!f.setWritable(true, false) ||
                !f.canWrite() ||
                !permission(f).matches(".(.w.){3}"))
                throw new Exception(f + ": setWritable(true, false) Failed");
            if (!f.setWritable(false, false) ||
                f.canWrite() ||
                !permission(f).matches(".(.-.){3}"))
                throw new Exception(f + ": setWritable(false, true) Failed");
            if (!f.setWritable(true) || !f.canWrite() ||
                permission(f).charAt(2) != 'w')
                throw new Exception(f + ": setWritable(true, ture) Failed");
            if (!f.setWritable(false) || f.canWrite() ||
                permission(f).charAt(2) != '-')
                throw new Exception(f + ": setWritable(false, true) Failed");
            if (!f.setExecutable(true, true) ||
                !f.canExecute() ||
                permission(f).charAt(3) != 'x')
                throw new Exception(f + ": setExecutable(true, true) Failed");
            if (!f.setExecutable(false, true) ||
                f.canExecute() ||
                permission(f).charAt(3) != '-')
                throw new Exception(f + ": setExecutable(false, true) Failed");
            if (!f.setExecutable(true, false) ||
                !f.canExecute() ||
                !permission(f).matches(".(..x){3}"))
                throw new Exception(f + ": setExecutable(true, false) Failed");
            if (!f.setExecutable(false, false) ||
                f.canExecute() ||
                !permission(f).matches(".(..-){3}"))
                throw new Exception(f + ": setExecutable(false, false) Failed");
            if (!f.setExecutable(true) || !f.canExecute() ||
                permission(f).charAt(3) != 'x')
                throw new Exception(f + ": setExecutable(true, true) Failed");
            if (!f.setExecutable(false) || f.canExecute() ||
                permission(f).charAt(3) != '-')
                throw new Exception(f + ": setExecutable(false, true) Failed");
            if (!f.setReadable(true, true) ||
                !f.canRead() ||
                permission(f).charAt(1) != 'r')
                throw new Exception(f + ": setReadable(true, true) Failed");
            if (!f.setReadable(false, true) ||
                f.canRead() ||
                permission(f).charAt(1) != '-')
                throw new Exception(f + ": setReadable(false, true) Failed");
            if (!f.setReadable(true, false) ||
                !f.canRead() ||
                !permission(f).matches(".(r..){3}"))
                throw new Exception(f + ": setReadable(true, false) Failed");
            if (!f.setReadable(false, false) ||
                f.canRead() ||
                !permission(f).matches(".(-..){3}"))
                throw new Exception(f + ": setReadable(false, false) Failed");
            if (!f.setReadable(true) || !f.canRead() ||
                permission(f).charAt(1) != 'r')
                throw new Exception(f + ": setReadable(true, true) Failed");
            if (!f.setReadable(false) || f.canRead() ||
                permission(f).charAt(1) != '-')
                throw new Exception(f + ": setReadable(false, true) Failed");
        } else {
            //Windows platform
            if (f.isFile()) {
                if (!f.setReadOnly())
                    throw new Exception(f + ": setReadOnly Failed");
                if (!f.setWritable(true, true) || !f.canWrite())
                    throw new Exception(f + ": setWritable(true, ture) Failed");
                if (!f.setWritable(true, false) || !f.canWrite())
                    throw new Exception(f + ": setWritable(true, false) Failed");
                if (!f.setWritable(true) || !f.canWrite())
                    throw new Exception(f + ": setWritable(true, ture) Failed");
                if (!f.setExecutable(true, true) || !f.canExecute())
                    throw new Exception(f + ": setExecutable(true, true) Failed");
                if (!f.setExecutable(true, false) || !f.canExecute())
                    throw new Exception(f + ": setExecutable(true, false) Failed");
                if (!f.setExecutable(true) || !f.canExecute())
                    throw new Exception(f + ": setExecutable(true, true) Failed");
                if (!f.setReadable(true, true) || !f.canRead())
                    throw new Exception(f + ": setReadable(true, true) Failed");
                if (!f.setReadable(true, false) || !f.canRead())
                    throw new Exception(f + ": setReadable(true, false) Failed");
                if (!f.setReadable(true) || !f.canRead())
                    throw new Exception(f + ": setReadable(true, true) Failed");
            }
            if (f.isDirectory()) {
                // setWritable should fail on directories because the DOS readonly
                // attribute prevents a directory from being deleted.
                if (f.setWritable(false, true))
                    throw new Exception(f + ": setWritable(false, true) Succeeded");
                if (f.setWritable(false, false))
                    throw new Exception(f + ": setWritable(false, false) Succeeded");
                if (f.setWritable(false))
                    throw new Exception(f + ": setWritable(false) Succeeded");
            } else {
                if (!f.setWritable(false, true) || f.canWrite())
                    throw new Exception(f + ": setWritable(false, true) Failed");
                if (!f.setWritable(false, false) || f.canWrite())
                    throw new Exception(f + ": setWritable(false, false) Failed");
                if (!f.setWritable(false) || f.canWrite())
                    throw new Exception(f + ": setWritable(false) Failed");
            }
            if (f.setExecutable(false, true))
                throw new Exception(f + ": setExecutable(false, true) Failed");
            if (f.setExecutable(false, false))
                throw new Exception(f + ": setExecutable(false, false) Failed");
            if (f.setExecutable(false))
                throw new Exception(f + ": setExecutable(false, true) Failed");
            if (f.setReadable(false, true))
                throw new Exception(f + ": setReadable(false, true) Failed");
            if (f.setReadable(false, false))
                throw new Exception(f + ": setReadable(false, false) Failed");
            if (f.setReadable(false))
                throw new Exception(f + ": setReadable(false, true) Failed");
        }
        if (f.exists() && !f.delete())
            throw new Exception("Can't delete test dir: " + f);
    }

    private static String permission(File f) throws Exception {
        PosixFileAttributes attrs = Files.readAttributes(f.toPath(), PosixFileAttributes.class);
        String type = attrs.isDirectory() ? "d" : " ";
        return type + PosixFilePermissions.toString(attrs.permissions());
    }
}
