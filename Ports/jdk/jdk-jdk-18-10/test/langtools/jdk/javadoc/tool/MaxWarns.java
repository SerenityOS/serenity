/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8005644 8182765
 * @summary set default max errs and max warns
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 */

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MaxWarns {
    public static void main(String... args) throws Exception {
        new MaxWarns().run();
    }

    void run() throws Exception {
        final int defaultMaxWarns = 100;
        final int genWarns = 150;
        File f = genSrc(genWarns);
        String out = javadoc(f);
        check(out, defaultMaxWarns);

        if (errors > 0)
            throw new Exception(errors + " errors occurred");
    }

    File genSrc(int count) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("package p;\n")
            .append("public class C {\n")
            .append("    /**\n");
        for (int i = 0; i < count; i++)
            sb.append("     * @param i").append(i).append(" does not exist!\n");
        sb.append("     */\n")
            .append("    public void m() { }\n")
            .append("}\n");
        File srcDir = new File("src");
        srcDir.mkdirs();
        File f = new File(srcDir, "C.java");
        try (FileWriter fw = new FileWriter(f)) {
            fw.write(sb.toString());
        }
        return f;
    }

    String javadoc(File f) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        String[] args = { "-Xdoclint:none", "--no-platform-links", "-d", "api", f.getPath() };
        int rc = jdk.javadoc.internal.tool.Main.execute(args, pw);
        pw.flush();
        return sw.toString();
    }

    void check(String out, int count) {
        System.err.println(out);
        Pattern warn = Pattern.compile("""
            warning: @param argument "i[0-9]+" is not a parameter name""");
        Matcher m = warn.matcher(out);
        int n = 0;
        for (int start = 0; m.find(start); start = m.start() + 1) {
            n++;
        }
        if (n != count)
            error("unexpected number of warnings reported: " + n + "; expected: " + count);

        Pattern warnCount = Pattern.compile("(?ms).*^([0-9]+) warnings$.*");
        m = warnCount.matcher(out);
        if (m.matches()) {
            n = Integer.parseInt(m.group(1));
            if (n != count)
                error("unexpected number of warnings reported: " + n + "; expected: " + count);
        } else
            error("total count not found");
    }

    void error(String msg) {
        System.err.println("Error: " + msg);
        errors++;
    }

    int errors;
}

