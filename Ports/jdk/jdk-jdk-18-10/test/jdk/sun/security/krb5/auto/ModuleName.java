/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8164437 8194486
 * @summary GSSContext type when jdk.security.jgss is not available
 * @library /test/lib
 * @compile -XDignore.symbol.file ModuleName.java
 * @build jdk.test.lib.Utils
 *        jdk.test.lib.Asserts
 *        jdk.test.lib.JDKToolFinder
 *        jdk.test.lib.JDKToolLauncher
 *        jdk.test.lib.Platform
 *        jdk.test.lib.process.*
 * @run main jdk.test.lib.FileInstaller TestHosts TestHosts
 * @run main/othervm -Djdk.net.hosts.file=TestHosts ModuleName
 */

import jdk.test.lib.process.ProcessTools;
import sun.security.jgss.GSSUtil;

import java.util.List;
import java.util.stream.Stream;

public class ModuleName {

    public static void main(String[] args) throws Throwable {

        if (args.length == 0) { // jtreg launched here

            // With all modules
            test("jdk.security.jgss");

            // With limited modules
            List<String> cmd = ProcessTools.createJavaProcessBuilder().command();
            Stream.of(jdk.internal.misc.VM.getRuntimeArguments())
                    .filter(arg -> arg.startsWith("--add-exports=") ||
                            arg.startsWith("--add-opens="))
                    .forEach(cmd::add);
            cmd.addAll(List.of(
                    "-Djdk.net.hosts.file=TestHosts",
                    "-Dtest.src=" + System.getProperty("test.src"),
                    "--add-modules",
                        "java.base,java.security.jgss,jdk.security.auth",
                    "--limit-modules",
                        "java.security.jgss,jdk.security.auth",
                    "ModuleName",
                    "launched-limited"));
            ProcessTools.executeCommand(cmd.toArray(new String[cmd.size()]))
                    .shouldHaveExitValue(0);
        } else { // Launched by ProcessTools above, with limited modules.
            test("java.security.jgss");
        }
    }

    static void test(String expected) throws Exception {

        new OneKDC(null).writeJAASConf();

        Context c = Context.fromJAAS("client");
        c.startAsClient(OneKDC.SERVER, GSSUtil.GSS_KRB5_MECH_OID);

        String moduleName = c.x().getClass().getModule().getName();
        if (!moduleName.equals(expected)) {
            throw new Exception("Expected: " + expected
                    + ". Actual: " + moduleName);
        }
    }
}
