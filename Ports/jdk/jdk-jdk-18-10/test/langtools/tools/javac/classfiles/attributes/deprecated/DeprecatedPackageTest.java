/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8042261
 * @summary Checking that deprecated attribute does not apply to classes of deprecated package.
 * @library /tools/lib /tools/javac/lib ../lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.util
 *          jdk.jdeps/com.sun.tools.classfile
 * @build toolbox.ToolBox InMemoryFileManager TestResult TestBase
 * @run main DeprecatedPackageTest
 */

import com.sun.tools.classfile.Attribute;
import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.Deprecated_attribute;

public class DeprecatedPackageTest extends TestResult {

    private static final String[] sourceTest = new String[]{
        "package deprecated;\n"
        + "public class notDeprecated{}",
        "package deprecated;\n"
        + "public interface notDeprecated{}",
        "package deprecated;\n"
        + "public @interface notDeprecated{}",
        "package deprecated;\n"
        + "public enum notDeprecated{}"
    };

    private static final String CLASS_NAME = "deprecated.notDeprecated";

    private static final String PACKAGE_INFO =
            "@Deprecated\n" +
            "package deprecated;";

    public static void main(String[] args) throws TestFailedException {
        new DeprecatedPackageTest().test();
    }

    private void test() throws TestFailedException {
        try {
            for (String src : sourceTest) {
                test(PACKAGE_INFO, src);
                test(PACKAGE_INFO.replaceAll("@Deprecated", "/** @deprecated */"), src);
            }
        } catch (Exception e) {
            addFailure(e);
        } finally {
            checkStatus();
        }
    }

    private void test(String package_info, String src) {
        addTestCase(src);
        printf("Testing test case: \n%s\n", src);
        try {
            ClassFile cf = readClassFile(compile(
                        new String[]{"package-info.java", package_info},
                        new String[]{"notDeprecated.java", src})
                    .getClasses().get(CLASS_NAME));
            Deprecated_attribute attr =
                    (Deprecated_attribute) cf.getAttribute(Attribute.Deprecated);
            checkNull(attr, "Class can not have deprecated attribute : " + CLASS_NAME);
        } catch (Exception e) {
            addFailure(e);
        }
    }
}
