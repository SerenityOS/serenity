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
 * @test SetDontInlineMethodTest
 * @bug 8006683 8007288 8022832
 * @summary testing of WB::testSetDontInlineMethod()
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+WhiteBoxAPI
 *                   -XX:CompileCommand=compileonly,compiler.whitebox.SimpleTestCaseHelper::*
 *                   compiler.whitebox.SetDontInlineMethodTest
 */

package compiler.whitebox;

public class SetDontInlineMethodTest extends CompilerWhiteBoxTest {

    public static void main(String[] args) throws Exception {
        CompilerWhiteBoxTest.main(SetDontInlineMethodTest::new, args);
    }

    private SetDontInlineMethodTest(TestCase testCase) {
        super(testCase);
    }

    /**
     * Tests {@code WB::testSetDontInlineMethod()} by sequential calling it and
     * checking of return value.
     *
     * @throws Exception if one of the checks fails.
     */
    @Override
    protected void test() throws Exception {
        if (WHITE_BOX.testSetDontInlineMethod(method, true)) {
            throw new RuntimeException("on start " + method
                    + " must be inlineable");
        }
        if (!WHITE_BOX.testSetDontInlineMethod(method, true)) {
            throw new RuntimeException("after first change to true " + method
                    + " must be not inlineable");
        }
        if (!WHITE_BOX.testSetDontInlineMethod(method, false)) {
            throw new RuntimeException("after second change to true " + method
                    + " must be still not inlineable");
        }
        if (WHITE_BOX.testSetDontInlineMethod(method, false)) {
            throw new RuntimeException("after first change to false" + method
                    + " must be inlineable");
        }
        if (WHITE_BOX.testSetDontInlineMethod(method, false)) {
            throw new RuntimeException("after second change to false " + method
                    + " must be inlineable");
        }
    }
}
