/*
 * Copyright (c) 2004, Oracle and/or its affiliates. All rights reserved.
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

/*
 *
 *
 * Used by BootClassPath.sh to remove a directory and its contents after a
 * test run.
 */
import java.io.File;

public class Cleanup {

    public static void main(String[] args) {
        File file = new File(args[0]);

        // paranoia is healthy in rm -rf
        String name = file.toString();
        if (name.equals(".") || name.equals("..") ||
                name.endsWith(File.separator + ".") ||
                name.endsWith(File.separator + "..")) {
            throw new RuntimeException("too risky to process: " + name);
        }
        if (file.isDirectory()) {
            File[] contents = file.listFiles();
            for (int i = 0; i < contents.length; i++) {
                contents[i].delete();
            }
        }
        if (!file.delete()) {
            throw new RuntimeException("Unable to delete " + file);
        }
    }
}
