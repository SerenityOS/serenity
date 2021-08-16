/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4958170 4891531 4989534
 * @summary Test to see if default java locale settings are identical
          when launch jvm from java and javaw respectively. Test
          should be run on Windows with different user locale and
          system locale setting in ControlPanel's RegionSetting.
          Following 2 testing scenarios are recommended
          (1)systemLocale=Japanese, userLocale=English
          (2)systemLocale=English, userLocale=Japanese
 * @compile -XDignore.symbol.file DefaultLocaleTest.java
 * @run main DefaultLocaleTestRun
 */

public class DefaultLocaleTestRun extends TestHelper {
    public static void main(String... args) {
        if (!isWindows) {
            System.out.println("Test passes vacuously on non-windows");
            return;
        }
        TestResult tr;
        tr = doExec(javaCmd,
                "-cp", TEST_CLASSES_DIR.getAbsolutePath(),
                "DefaultLocaleTest", "-w", "x.out");
        System.out.println(tr.testOutput);
        tr = doExec(javawCmd,
                "-cp", TEST_CLASSES_DIR.getAbsolutePath(),
                "DefaultLocaleTest", "-r", "x.out");
        System.out.println(tr.testOutput);
        if (!tr.isOK()) {
            throw new RuntimeException("Test failed");
        }
    }
}
