/*
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.monitoring.share;

import java.util.*;
import java.io.*;
import java.util.ArrayList;

public class Generator {
        private String patternFileName;
        private String outDir;
        private List<String> lines = new ArrayList<>();
        private String prefix = "LoadableClass";

        public Generator(String patternFileName, String outDir) {
                this.patternFileName = patternFileName;
                this.outDir = outDir;
        }

        private void load() throws IOException {
                BufferedReader rd = null;
                try {
                        rd = new BufferedReader(new FileReader(patternFileName));
                        String s = rd.readLine();
                        while (s != null) {
                                lines.add(s);
                                s = rd.readLine();
                        }
                } finally {
                        if (rd != null) {
                                rd.close();
                        }
                }
        }

        private void generate(int n) throws IOException {
                PrintStream out = null;
                String tokens = Integer.toString(n);
                if (tokens.length() == 1)
                        tokens = "00" + tokens;
                else if (tokens.length() == 2)
                        tokens = "0" + tokens;
                try {
                        out = new PrintStream(new FileOutputStream(new File(outDir, prefix + tokens + ".java")));
                        for (int i = 0; i < lines.size(); ++i) {
                                String line = lines.get(i);
                                out.println(line.replaceAll("XYZ", tokens));
                        }
                } finally {
                        if (out != null) {
                                out.close();
                        }
                }
        }

        public void run() throws IOException {
                load();
                for (int i = 1; i < 101; ++i)
                        generate(i);
        }

        private static void usage() {
                System.out.println("Usage: nsk.monitoring.share.Generator <pattern file> <output directory>");
        }

        public static void main(String[] args) throws Exception {
                if (args.length != 2) {
                        usage();
                        throw new IllegalArgumentException("Need exactly two arguments.");
                }
                new Generator(args[0], args[1]).run();
        }
}
