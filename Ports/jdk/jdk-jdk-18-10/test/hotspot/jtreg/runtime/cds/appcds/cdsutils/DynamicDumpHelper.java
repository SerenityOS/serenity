/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

package cdsutils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

/**
 * This class is used to simulate -Xshare:dump with -XX:ArchiveClassesAtExit.
 * It loads all classes specified in a classlist file. See patchJarForDynamicDump()
 * in ../TestCommon.java for details.
 */
public class DynamicDumpHelper {
    public static void main(String args[]) throws Throwable {
        File file = new File(args[0]);

        System.out.println("Loading classes to share...");
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                //System.out.println("Loading class: " + line);
                line = line.replace('/', '.');
                try {
                    Class.forName(line);
                } catch (java.lang.ClassNotFoundException ex) {
                    try {
                        Class.forName(line, true, null);
                    } catch (java.lang.ClassNotFoundException cnfe) {
                        System.out.println("Preload Warning: Cannot find " + line.replace('.', '/'));
                    }
                } catch (Throwable t) {
                    System.out.println("Error: failed to load \"" + line + "\": " + t);
                }
            }
        }
        System.out.println("Loading classes to share: done.");
    }
}
