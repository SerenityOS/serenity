/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.tools.jlink.internal;

import java.io.*;
import java.util.spi.ToolProvider;

public class Main {
    public static void main(String... args) throws Exception {
        System.exit(run(new PrintWriter(System.out, true),
                        new PrintWriter(System.err, true),
                        args));
    }

    /**
     * Entry point that does <i>not</i> call System.exit.
     *
     * @param out output stream
     * @param err error output stream
     * @param args command line arguments
     * @return an exit code. 0 means success, non-zero means an error occurred.
     */
    @SuppressWarnings("removal")
    public static int run(PrintWriter out, PrintWriter err, String... args) {
        if (System.getSecurityManager() != null) {
            System.getSecurityManager().
                    checkPermission(new JlinkPermission("jlink"));
        }

        JlinkTask t = new JlinkTask();
        t.setLog(out, err);
        return t.run(args);
    }

    public static class JlinkToolProvider implements ToolProvider {
        @Override
        public String name() {
            return "jlink";
        }

        @Override
        public int run(PrintWriter out, PrintWriter err, String... args) {
            return Main.run(out, err, args);
        }
    }
}
