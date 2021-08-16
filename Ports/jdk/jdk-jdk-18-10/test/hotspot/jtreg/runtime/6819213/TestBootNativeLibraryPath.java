/*
 * Copyright (c) 2008, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @test TestBootNativeLibraryPath.java
 * @bug 6819213
 * @summary verify sun.boot.native.library.path is expandable on 32 bit systems
 * @author ksrini
 * @modules java.compiler
 * @library /test/lib
 * @requires vm.bits == 32
 * @compile -XDignore.symbol.file TestBootNativeLibraryPath.java
 * @run main TestBootNativeLibraryPath
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

public class TestBootNativeLibraryPath {

    private static final String TESTFILE = "Test6";

    static void createTestClass() throws IOException {
        FileOutputStream fos = new FileOutputStream(TESTFILE + ".java");
        PrintStream ps = new PrintStream(fos);
        ps.println("public class " + TESTFILE + "{");
        ps.println("public static void main(String[] args) {\n");
        ps.println("System.out.println(System.getProperty(\"sun.boot.library.path\"));\n");
        ps.println("}}\n");
        ps.close();
        fos.close();

        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        String javacOpts[] = {TESTFILE + ".java"};
        if (javac.run(null, null, null,  javacOpts) != 0) {
            throw new RuntimeException("compilation of " + TESTFILE + ".java Failed");
        }
    }

    static List<String> doExec(String... args) {
        String javaCmd = System.getProperty("java.home") + "/bin/java";
        if (!new File(javaCmd).exists()) {
            javaCmd = System.getProperty("java.home") + "/bin/java.exe";
        }

        ArrayList<String> cmds = new ArrayList<String>();
        cmds.add(javaCmd);
        for (String x : args) {
            cmds.add(x);
        }
        System.out.println("cmds=" + cmds);
        ProcessBuilder pb = new ProcessBuilder(cmds);

        Map<String, String> env = pb.environment();
        pb.directory(new File("."));

        List<String> out = new ArrayList<String>();
        try {
            pb.redirectErrorStream(true);
            Process p = pb.start();
            BufferedReader rd = new BufferedReader(new InputStreamReader(p.getInputStream()),8192);
            String in = rd.readLine();
            while (in != null) {
                out.add(in);
                System.out.println(in);
                in = rd.readLine();
            }
            int retval = p.waitFor();
            p.destroy();
            if (retval != 0) {
                throw new RuntimeException("Error: test returned non-zero value");
            }
            return out;
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex.getMessage());
        }
    }

    public static void main(String[] args) {
        try {
            String osname = System.getProperty("os.name");
            if (osname.startsWith("Windows")) {
                osname = "Windows";
            }

            createTestClass();

            // Test a simple path
            String libpath = File.pathSeparator + "tmp" + File.pathSeparator + "foobar";
            List<String> processOut = null;
            String sunbootlibrarypath = "-Dsun.boot.library.path=" + libpath;
            processOut = doExec(sunbootlibrarypath, "-cp", ".", TESTFILE);
            if (processOut == null || !processOut.get(0).endsWith(libpath)) {
                throw new RuntimeException("Error: did not get expected error string");
            }
        } catch (IOException ex) {
            throw new RuntimeException("Unexpected error " + ex);
        }
    }
}
