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

/*
 * @test TestCompilerDirectivesCompatibilityBase
 * @bug 8137167
 * @summary Test compiler control compatibility with compile command
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 *          java.compiler
 *          java.management
 *
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run testng/othervm -Xbootclasspath/a:. -Xmixed -XX:+UnlockDiagnosticVMOptions
 *      -XX:+WhiteBoxAPI
 *      compiler.compilercontrol.TestCompilerDirectivesCompatibilityBase
 */

package compiler.compilercontrol;

import compiler.testlibrary.CompilerUtils;
import compiler.whitebox.CompilerWhiteBoxTest;
import jdk.test.lib.dcmd.CommandExecutor;
import jdk.test.lib.dcmd.JMXExecutor;
import org.testng.annotations.Test;
import sun.hotspot.WhiteBox;

import java.io.File;
import java.lang.reflect.Method;

public class TestCompilerDirectivesCompatibilityBase {

    public static final WhiteBox WB = WhiteBox.getWhiteBox();
    public static String control_on, control_off;
    Method method, nomatch;

    public void run(CommandExecutor executor) throws Exception {

        control_on = System.getProperty("test.src", ".") + File.separator + "control_on.txt";
        control_off = System.getProperty("test.src", ".") + File.separator + "control_off.txt";
        method  = getMethod(TestCompilerDirectivesCompatibilityBase.class, "helper");
        nomatch = getMethod(TestCompilerDirectivesCompatibilityBase.class, "another");

        int[] levels = CompilerUtils.getAvailableCompilationLevels();
        for (int complevel : levels) {
            // Only test the major compilers, ignore profiling levels
            if (complevel == CompilerWhiteBoxTest.COMP_LEVEL_SIMPLE || complevel == CompilerWhiteBoxTest.COMP_LEVEL_FULL_OPTIMIZATION){
                testCompatibility(executor, complevel);
            }
        }
    }

    public void testCompatibility(CommandExecutor executor, int comp_level) throws Exception {

        // Call all validation twice to catch error when overwriting a directive
        // Flag is default off
        expect(!WB.getBooleanVMFlag("PrintAssembly"));
        expect(!WB.shouldPrintAssembly(method, comp_level));
        expect(!WB.shouldPrintAssembly(nomatch, comp_level));
        expect(!WB.shouldPrintAssembly(method, comp_level));
        expect(!WB.shouldPrintAssembly(nomatch, comp_level));

        // load directives that turn it on
        executor.execute("Compiler.directives_add " + control_on);
        expect(WB.shouldPrintAssembly(method, comp_level));
        expect(!WB.shouldPrintAssembly(nomatch, comp_level));
        expect(WB.shouldPrintAssembly(method, comp_level));
        expect(!WB.shouldPrintAssembly(nomatch, comp_level));

        // remove and see that it is true again
        executor.execute("Compiler.directives_remove");
        expect(!WB.shouldPrintAssembly(method, comp_level));
        expect(!WB.shouldPrintAssembly(nomatch, comp_level));
        expect(!WB.shouldPrintAssembly(method, comp_level));
        expect(!WB.shouldPrintAssembly(nomatch, comp_level));
    }

    public void expect(boolean test) throws Exception {
        if (!test) {
            throw new Exception("Test failed");
        }
    }

    public void expect(boolean test, String msg) throws Exception {
        if (!test) {
            throw new Exception(msg);
        }
    }

    @Test
    public void jmx() throws Exception {
        run(new JMXExecutor());
    }

    public void helper() {
    }

    public void another() {
    }

    public static Method getMethod(Class klass, String name, Class<?>... parameterTypes) {
        try {
            return klass.getDeclaredMethod(name, parameterTypes);
        } catch (NoSuchMethodException | SecurityException e) {
            throw new RuntimeException("exception on getting method Helper." + name, e);
        }
    }
}
