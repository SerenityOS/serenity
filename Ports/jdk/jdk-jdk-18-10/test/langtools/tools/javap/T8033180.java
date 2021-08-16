/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8033180
 * @summary Bad newline characters
 * @modules jdk.jdeps/com.sun.tools.javap
 */

import java.io.*;
import java.util.*;

public class T8033180 {

    public static void main(String... args) throws Exception {
        new T8033180().run();
    }

    void run() throws Exception {
        // fast-track this case, because test cannot fail in this case
        if (lineSep.equals(nl))
            return;

        test("-help");
        test("-version");

        if (errors > 0)
            throw new Exception(errors + " errors occurred");
    }

    static final String lineSep = System.getProperty("line.separator");
    static final String nl = "\n";

    void test(String... opts) throws Exception {
        System.err.println("test " + Arrays.asList(opts));
        List<String> args = new ArrayList<String>();
        args.addAll(Arrays.asList(opts));
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        int rc = com.sun.tools.javap.Main.run(args.toArray(new String[args.size()]), pw);
        pw.close();
        String out = sw.toString();
        if (rc != 0)
            throw new Exception("javap failed unexpectedly: rc=" + rc);

        // remove all valid platform newline sequences
        String out2 = out.replace(lineSep, "");

        // count the remaining simple newline characters
        int count = 0;
        int i = out2.indexOf(nl, 0);
        while (i != -1) {
            count++;
            i = out2.indexOf(nl, i + nl.length());
        }

        if (count > 0)
            error(count + " newline characters found");
    }

    void error(String msg) {
        System.err.println("Error: " + msg);
        errors++;
    }

    int errors = 0;
}

