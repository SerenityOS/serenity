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
 * @bug 8042251
 * @summary Test that outer_class_info_index of local and anonymous class is zero.
 * @library /tools/lib /tools/javac/lib ../lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.jdeps/com.sun.tools.classfile
 * @build toolbox.ToolBox InMemoryFileManager TestResult TestBase
 * @run main InnerClassesIndexTest
 */

import java.io.File;
import java.io.FilenameFilter;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sun.tools.classfile.Attribute;
import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.InnerClasses_attribute;
import com.sun.tools.classfile.InnerClasses_attribute.Info;

public class InnerClassesIndexTest extends TestResult {

    public static void main(String[] args) throws TestFailedException {
        new InnerClassesIndexTest().test();
    }

    private boolean isExcluded(String className) {
        return !className.startsWith(InnerClassesIndexTest.class.getName())
                || "InnerClassesIndexTest$Inner".equals(className);
    }

    private Set<String> getInnerClasses() {
        FilenameFilter filter = (dir, name) -> name.matches("InnerClassesIndexTest\\$.*\\.class");
        return Stream.of(getClassDir().listFiles(filter))
                .map(File::getName)
                .map(s -> s.replace(".class", ""))
                .collect(Collectors.toSet());
    }

    public void test() throws TestFailedException {
        try {
            addTestCase("Source is InnerClassesIndexTest.java");
            ClassFile classFile = readClassFile(InnerClassesIndexTest.class);
            InnerClasses_attribute attr = (InnerClasses_attribute)
                    classFile.getAttribute(Attribute.InnerClasses);

            Set<String> foundClasses = new HashSet<>();
            for (Info info : attr.classes) {
                String innerName = classFile.constant_pool.
                        getClassInfo(info.inner_class_info_index).getBaseName();
                echo("Testing class : " + innerName);
                if (isExcluded(innerName)) {
                    echo("Ignored : " + innerName);
                    continue;
                }
                foundClasses.add(innerName);
                checkEquals(info.outer_class_info_index, 0,
                        "outer_class_info_index of " + innerName);
                if (innerName.matches("\\$\\d+")) {
                    checkEquals(info.inner_name_index, 0,
                            "inner_name_index of anonymous class");
                }
            }
            Set<String> expectedClasses = getInnerClasses();
            expectedClasses.remove("InnerClassesIndexTest$Inner");
            checkEquals(foundClasses, expectedClasses, "All classes are found");
        } catch (Exception e) {
            addFailure(e);
        } finally {
            checkStatus();
        }
    }

    static class Inner {
    }

    Inner inner1 = new Inner() {
    };

    static Inner inner2 = new Inner() {
    };

    Runnable r = () -> {
        class Local {
        }
        new Local() {
        };
    };

    public void local() {
        class Local {
        }
        new Local() {
        };
    }

    public InnerClassesIndexTest() {
        class Local {
        }
        new Local() {
        };
    }

    {
        class Local {
        }
        new Local() {
        };
    }

    static {
        class Local {
        }
        new Local() {
        };
    }
}
