/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug 8003228
 * @summary Test the value of sun.jnu.encoding on Mac
 * @requires (os.family == "mac")
 * @library /test/lib
 * @build jdk.test.lib.process.*
 *        ExpectedEncoding
 * @run main MacJNUEncoding US-ASCII UTF-8 C
 * @run main MacJNUEncoding UTF-8    UTF-8 en_US.UTF-8
 */

import java.util.Map;

import jdk.test.lib.process.ProcessTools;

public class MacJNUEncoding {

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.out.println("Usage:");
            System.out.println("  java MacJNUEncoding"
                    + " <expected file.encoding> <expected sun.jnu.encoding> <locale>");
            throw new IllegalArgumentException("missing arguments");
        }

        final String locale = args[2];
        System.out.println("Running test for locale: " + locale);
        ProcessBuilder pb = ProcessTools.createTestJvm(
                ExpectedEncoding.class.getName(), args[0], args[1]);
        Map<String, String> env = pb.environment();
        env.put("LANG", locale);
        env.put("LC_ALL", locale);
        ProcessTools.executeProcess(pb)
                    .outputTo(System.out)
                    .errorTo(System.err)
                    .shouldHaveExitValue(0);
    }
}
