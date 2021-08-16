/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package com.sun.tools.jdeps;

import java.io.*;
import java.util.spi.ToolProvider;

/**
 *
 * Usage:
 *    jdeps [options] files ...
 * where options include:
 *    -p package-name   restrict analysis to classes in this package
 *                      (may be given multiple times)
 *    -e regex          restrict analysis to packages matching pattern
 *                      (-p and -e are exclusive)
 *    -v                show class-level dependencies
 *                      default: package-level dependencies
 *    -r --recursive    transitive dependencies analysis
 *    -classpath paths  Classpath to locate class files
 *    -all              process all class files in the given classpath
 */
public class Main {
    public static void main(String... args) throws Exception {
        JdepsTask t = new JdepsTask();
        int rc = t.run(args);
        System.exit(rc);
    }


    /**
     * Entry point that does <i>not</i> call System.exit.
     *
     * @param args command line arguments
     * @param out output stream
     * @return an exit code. 0 means success, non-zero means an error occurred.
     */
    public static int run(String[] args, PrintWriter out) {
        JdepsTask t = new JdepsTask();
        t.setLog(out);
        return t.run(args);
    }

    public static class JDepsToolProvider implements ToolProvider {
        public String name() {
            return "jdeps";
        }

        public int run(PrintWriter out, PrintWriter err, String... args) {
            return Main.run(args, out);
        }
    }
}
