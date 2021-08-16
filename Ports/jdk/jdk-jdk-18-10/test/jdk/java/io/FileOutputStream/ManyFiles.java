/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4189011 5019303
 * @summary Test opening over 2048 files
 * @run main/timeout=300 ManyFiles
 */

import java.io.*;
import java.util.*;

public class ManyFiles {
    static int count;
    static List files = new ArrayList();
    static List streams = new ArrayList();
    static int NUM_FILES = 2050;

    public static void main(String args[]) throws Exception {
        // Linux does not yet allow opening this many files; Solaris
        // 8 requires an explicit allocation of more file descriptors
        // to succeed. Since this test is written to check for a
        // Windows capability it is much simpler to only run it
        // on that platform.
        String osName = System.getProperty("os.name");
        if (osName.startsWith("Linux"))
            return;

        for (int n = 0; n < NUM_FILES; n++) {
            File f = new File("file" + count++);
            files.add(f);
            streams.add(new FileOutputStream(f));
        }

        Iterator i = streams.iterator();
        while(i.hasNext()) {
            FileOutputStream fos = (FileOutputStream)i.next();
            fos.close();
        }

        i = files.iterator();
        while(i.hasNext()) {
            File f = (File)i.next();
            f.delete();
        }
    }
}
