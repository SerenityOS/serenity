/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8168615 8172102
 * @summary Test all the ToolSimpleTest tests, but in local execution. Verify --execution flag
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.jdeps/com.sun.tools.javap
 *          jdk.jshell/jdk.internal.jshell.tool
 * @build KullaTesting TestingInputStream ToolSimpleTest
 * @run testng/othervm ToolLocalSimpleTest
 */

import java.util.Locale;
import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;

public class ToolLocalSimpleTest extends ToolSimpleTest {

    @Override
    public void test(Locale locale, boolean isDefaultStartUp, String[] args, String startUpMessage, ReplTest... tests) {
        String[] wargs = new String[args.length + 2];
        wargs[0] = "--execution";
        wargs[1] = "local";
        System.arraycopy(args, 0, wargs, 2, args.length);
        super.test(locale, isDefaultStartUp, wargs, startUpMessage, tests);
    }

    @Test
    public void verifyLocal() {
        System.setProperty("LOCAL_CHECK", "Here");
        assertEquals(System.getProperty("LOCAL_CHECK"), "Here");
        test(new String[]{"--no-startup"},
                a -> assertCommand(a, "System.getProperty(\"LOCAL_CHECK\")", "$1 ==> \"Here\""),
                a -> assertCommand(a, "System.setProperty(\"LOCAL_CHECK\", \"After\")", "$2 ==> \"Here\"")
        );
        assertEquals(System.getProperty("LOCAL_CHECK"), "After");
    }

    @Override
    @Test
    public void testOptionR() {
        test(new String[]{"-R-Dthe.sound=blorp", "--no-startup"},
                (a) -> assertCommand(a, "System.getProperty(\"the.sound\")",
                        "$1 ==> null")
        );
    }

    @Override
    @Test
    public void testCompoundStart() {
        test(new String[]{"--startup", "DEFAULT", "--startup", "PRINTING"},
                (a) -> assertCommandOutputContains(a, "/list -start",
                        "System.out.println", "import java.util.concurrent")
        );
    }

    @Test
    public void testOptionBadR() {
        test(new String[]{"-R-RottenLiver"},
                (a) -> assertCommand(a, "43",
                        "$1 ==> 43")
        );
    }

}
