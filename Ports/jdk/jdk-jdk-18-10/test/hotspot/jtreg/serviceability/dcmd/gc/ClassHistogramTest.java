/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.regex.Pattern;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.dcmd.CommandExecutor;
import jdk.test.lib.dcmd.JMXExecutor;

/*
 * @test
 * @summary Test of diagnostic command GC.class_histogram
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.compiler
 *          java.management
 *          jdk.internal.jvmstat/sun.jvmstat.monitor
 * @run testng ClassHistogramTest
 */
public class ClassHistogramTest {
    public static class TestClass {}
    public static TestClass[] instances = new TestClass[1024];

    static {
        for (int i = 0; i < instances.length; ++i) {
            instances[i] = new TestClass();
        }
    }

    public void run(CommandExecutor executor, String classHistogramArgs, String expactedErrMsg) {
        OutputAnalyzer output = executor.execute("GC.class_histogram " + classHistogramArgs);
        if (!expactedErrMsg.isEmpty()) {
            output.shouldMatch(expactedErrMsg);
            return;
        }

        /*
         * example output:
         *  num     #instances         #bytes  class name (module)
         * -------------------------------------------------------
         *    1:          7991         757792  [B (java.base@9-internal)
         *    2:          1811         217872  java.lang.Class (java.base@9-internal)
         *    3:          6724         215168  java.util.HashMap$Node (java.base@9-internal)
         *    4:          7852         188448  java.lang.String (java.base@9-internal)
         *    5:          1378         105040  [Ljava.util.HashMap$Node; (java.base@9-internal)
         *    6:          1863          95096  [Ljava.lang.Object; (java.base@9-internal)

         * ...
         */

        String moduleRegex = "\\(java.base(?:@\\S*)?\\)";

        /* Require at least one java.lang.Class */
        output.shouldMatch("^\\s+\\d+:\\s+\\d+\\s+\\d+\\s+java.lang.Class " + moduleRegex + "\\s*$");

        /* Require at least one java.lang.String */
        output.shouldMatch("^\\s+\\d+:\\s+\\d+\\s+\\d+\\s+java.lang.String " + moduleRegex + "\\s*$");

        /* Require at least one java.lang.Object */
        output.shouldMatch("^\\s+\\d+:\\s+\\d+\\s+\\d+\\s+java.lang.Object " + moduleRegex + "\\s*$");

        /* Require at exactly one TestClass[] */
        output.shouldMatch("^\\s+\\d+:\\s+1\\s+\\d+\\s+" +
            Pattern.quote(TestClass[].class.getName()) + "\\s*$");

        /* Require at exactly 1024 TestClass */
        output.shouldMatch("^\\s+\\d+:\\s+1024\\s+\\d+\\s+" +
            Pattern.quote(TestClass.class.getName()) + "\\s*$");
    }

    @DataProvider(name="ArgsProvider")
    private Object[][] getArgs() {
        String parallelErr = "Parallel thread number out of range";
        return new Object[][] {
                // valid args
                {"", ""},
                {"-parallel=0", ""},
                {"-parallel=1", ""},
                {"-parallel=2", ""},
                {"-parallel="+Long.MAX_VALUE, ""},
                {"-all=false -parallel=0", ""},
                {"-all=false -parallel=1", ""},
                {"-all=false -parallel=2", ""},
                {"-all=true", ""},
                {"-all=true -parallel=0", ""},
                {"-all=true -parallel=1", ""},
                {"-all=true -parallel=2", ""},
                {"-parallel=2 -all=true", ""},
                // invalid args
                {"-parallel=-1", parallelErr},
                {"-parallel="+Long.MIN_VALUE, parallelErr},
                {"-all=false -parallel=-10", parallelErr},
                {"-all=true -parallel=-100", parallelErr},
        };
    }

    @Test(dataProvider="ArgsProvider")
    public void jmx(String args, String expactedErrMsg) {
        run(new JMXExecutor(), args, expactedErrMsg);
    }
}
