/*
 * Copyright (c) 2008, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4876942 6715251
 * @summary javap invoked without args does not print help screen
 * @modules jdk.jdeps/com.sun.tools.javap
 */

import java.io.*;
import java.util.zip.*;

public class T4876942 {
    public static void main(String[] args) throws Exception {
        new T4876942().run();
    }

    public void run() throws IOException {
        String output = javap();
        verify(output, "-public", "-protected", "-private"); // check that some of the options are listed

        if (errors > 0)
            throw new Error(errors + " found.");
    }

    String javap(String... args) {
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);
        //sun.tools.javap.Main.entry(args);
        int rc = com.sun.tools.javap.Main.run(args, out);
        if (rc != (args.length == 0 ? 2 : 0))
            throw new Error("javap failed. rc=" + rc);
        out.close();
        return sw.toString();
    }

    void verify(String output, String... expects) {
        for (String expect: expects) {
            if (output.indexOf(expect)< 0)
                error(expect + " not found");
        }
    }

    void error(String msg) {
        System.err.println(msg);
        errors++;
    }

    int errors;
}
