/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8006615
 * @summary move remaining messages into resource bundle
 * @modules jdk.javadoc/jdk.javadoc.internal.doclint
 */

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import jdk.javadoc.internal.doclint.DocLint;

public class ResourceTest {
    public static void main(String... args) throws Exception {
        Locale prev = Locale.getDefault();
        Locale.setDefault(Locale.ENGLISH);
        try {
            new ResourceTest().run();
        } finally {
           Locale.setDefault(prev);
        }
    }

    public void run() throws Exception {
        test(Arrays.asList("-help"),
                Arrays.asList("Usage:", "Options"));
        test(Arrays.asList("-foo"),
                Arrays.asList("bad option: -foo"));
    }

    void test(List<String> opts, List<String> expects) throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        try {
            new DocLint().run(pw, opts.toArray(new String[opts.size()]));
        } catch (DocLint.BadArgs e) {
            pw.println("BadArgs: " + e.getMessage());
        } catch (IOException e) {
            pw.println("IOException: " + e.getMessage());
        } finally {
            pw.close();
        }

        String out = sw.toString();
        if (!out.isEmpty()) {
            System.err.println(out);
        }

        for (String e: expects) {
            if (!out.contains(e))
                throw new Exception("expected string not found: " + e);
        }
    }
}

