/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug      8212703
 * @summary  Test JAVA2D_FONTPATH env. var does not set a system property
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FontPathEnvTest {

    public static void main(String args[]) {
        String env = System.getenv("JAVA2D_FONTPATH");
        if (env == null) {
           createChild();
        } else {
            String prop = System.getProperty("sun.java2d.fontpath");
            if (prop != null && env.equals(prop)) {
                throw new RuntimeException("sun.java2d.fontpath property set");
            }
        }
    }

    static void createChild() {
        String cpDir = System.getProperty("java.class.path");
        Map<String, String> env = new HashMap<String, String>();
        env.put("JAVA2D_FONTPATH", "anyValue");
        String jHome = System.getProperty("java.home");
        String jCmd = jHome + File.separator + "bin" + File.separator + "java";
        int exitValue = doExec(env, jCmd, "-cp", cpDir, "FontPathEnvTest");
        if (exitValue != 0) {
            throw new RuntimeException("Test Failed");
        }
    }

    static int doExec(Map<String, String> envToSet, String... cmds) {
        Process p = null;
        ProcessBuilder pb = new ProcessBuilder(cmds);
        Map<String, String> env = pb.environment();
        for (String cmd : cmds) {
            System.out.print(cmd + " ");
        }
        System.out.println();
        if (envToSet != null) {
            env.putAll(envToSet);
        }
        BufferedReader rdr = null;
        try {
            pb.redirectErrorStream(true);
            p = pb.start();
            rdr = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String in = rdr.readLine();
            while (in != null) {
                in = rdr.readLine();
                System.out.println(in);
            }
            p.waitFor();
            p.destroy();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return p.exitValue();
    }
}
