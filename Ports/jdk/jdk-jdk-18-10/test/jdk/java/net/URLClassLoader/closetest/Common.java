/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
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
import java.net.*;
import java.nio.file.Files;
import jdk.test.lib.util.FileUtils;
import static java.nio.file.StandardCopyOption.*;

public class Common {

    static void copyFile (String src, String dst) {
        copyFile (new File(src), new File(dst));
    }

    static void copyDir (String src, String dst) {
        copyDir (new File(src), new File(dst));
    }

    static void copyFile (File src, File dst) {
        try {
            if (!src.isFile()) {
                throw new RuntimeException ("File not found: " + src.toString());
            }
            Files.copy(src.toPath(), dst.toPath(), REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException (e);
        }
    }

    static void rm_minus_rf (File path) throws IOException, InterruptedException {
        if (!path.exists())
            return;
        FileUtils.deleteFileTreeWithRetry(path.toPath());
    }

    static void copyDir (File src, File dst) {
        if (!src.isDirectory()) {
            throw new RuntimeException ("Dir not found: " + src.toString());
        }
        if (dst.exists()) {
            throw new RuntimeException ("Dir exists: " + dst.toString());
        }
        dst.mkdir();
        String[] names = src.list();
        File[] files = src.listFiles();
        for (int i=0; i<files.length; i++) {
            String f = names[i];
            if (files[i].isDirectory()) {
                copyDir (files[i], new File (dst, f));
            } else {
                copyFile (new File (src, f), new File (dst, f));
            }
        }
    }

    /* expect is true if you expect to find it, false if you expect not to */
    static Class loadClass (String name, URLClassLoader loader, boolean expect){
        try {
            Class clazz = Class.forName (name, true, loader);
            if (!expect) {
                throw new RuntimeException ("loadClass: "+name+" unexpected");
            }
            return clazz;
        } catch (ClassNotFoundException e) {
            if (expect) {
                throw new RuntimeException ("loadClass: " +name + " not found");
            }
        }
        return null;
    }
}
