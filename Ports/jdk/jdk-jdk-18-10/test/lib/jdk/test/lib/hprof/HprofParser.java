/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

package jdk.test.lib.hprof;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

import jdk.test.lib.hprof.model.Snapshot;
import jdk.test.lib.hprof.parser.Reader;

/**
 * Helper class to parse a java heap dump file.
 */
public class HprofParser {

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("No arguments supplied");
        }
        File dump = new File(args[0]);
        if (!dump.exists() || !dump.isFile()) {
            throw new RuntimeException("The dump file does not exist or not a file");
        }
        parse(dump);
    }

    /**
     * @see #parse(File, boolean, boolean, boolean)
     */
    public static File parse(File dump) throws Exception {
        return parse(dump, false, true, true);
    }

    /**
     * @see #parse(File, boolean, boolean, boolean)
     */
    public static File parseWithDebugInfo(File dump) throws Exception {
        return parse(dump, true, true, true);
    }

    /**
     * Parse a java heap dump file
     *
     * @param dump Heap dump file to parse
     * @param debug Turn on/off debug file parsing
     * @param callStack Turn on/off tracking of object allocation call stack
     * @param calculateRefs Turn on/off tracking object allocation call stack
     * @throws Exception
     * @return File containing output from the parser
     */
    public static File parse(File dump, boolean debug, boolean callStack, boolean calculateRefs) throws Exception {
        File out = new File("hprof." + System.currentTimeMillis() + ".out");
        if (out.exists()) {
            out.delete();
        }

        PrintStream psSystemOut = System.out;
        try (PrintStream psHprof = new PrintStream(new BufferedOutputStream(new FileOutputStream(out.getAbsolutePath())))) {
            System.setOut(psHprof);

            int debugLevel = debug ? 2 : 0;
            try (Snapshot snapshot = Reader.readFile(dump.getAbsolutePath(), callStack, debugLevel)) {
                System.out.println("Snapshot read, resolving...");
                snapshot.resolve(calculateRefs);
                System.out.println("Snapshot resolved.");
            }
       } finally {
           System.setOut(psSystemOut);
       }

        return out;
    }

}
