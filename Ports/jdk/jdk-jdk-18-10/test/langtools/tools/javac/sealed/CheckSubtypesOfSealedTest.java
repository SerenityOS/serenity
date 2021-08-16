/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary check subtypes of sealed classes
 * @library /tools/lib /tools/javac/lib /tools/javac/classfiles/attributes/lib
 * @modules jdk.jdeps/com.sun.tools.classfile
 *          jdk.compiler/com.sun.tools.javac.code
 *          jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.util
 * @build toolbox.ToolBox toolbox.JavacTask InMemoryFileManager TestBase
 * @run main CheckSubtypesOfSealedTest
 */

import java.util.List;
import com.sun.tools.classfile.*;
import com.sun.tools.classfile.ConstantPool.CONSTANT_Utf8_info;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.util.Assert;

public class CheckSubtypesOfSealedTest extends TestBase {

    static final String testSource =
            "public class SealedClasses {\n" +
            "    sealed abstract class SAC {}\n" +
            "    sealed abstract class SAC2 extends SAC {}\n" +
            "    final class SAC3 extends SAC {}\n" +
            "    final class SAC4 extends SAC2 {}\n" +
            "    sealed interface SI {}\n" +
            "    sealed interface SSI extends SI {}\n" +
            "    final class SAC5 implements SI, SSI {}\n" +
            "    non-sealed abstract class SAC6 extends SAC {}\n" +
            "    non-sealed class SAC7 extends SAC {}\n" +
            "}";

    public static void main(String[] args) throws Exception {
        new CheckSubtypesOfSealedTest().run();
    }

    void run() throws Exception {
        InMemoryFileManager fileManager = compile(testSource);
        checkClassFile(readClassFile(fileManager.getClasses().get("SealedClasses$SAC2")), CheckFor.SEALED);
        checkClassFile(readClassFile(fileManager.getClasses().get("SealedClasses$SAC3")), CheckFor.FINAL);
        checkClassFile(readClassFile(fileManager.getClasses().get("SealedClasses$SAC4")), CheckFor.FINAL);
        checkClassFile(readClassFile(fileManager.getClasses().get("SealedClasses$SSI")), CheckFor.SEALED);
        checkClassFile(readClassFile(fileManager.getClasses().get("SealedClasses$SAC5")), CheckFor.FINAL);
        checkClassFile(readClassFile(fileManager.getClasses().get("SealedClasses$SAC6")), CheckFor.NOT_SEALED);
        checkClassFile(readClassFile(fileManager.getClasses().get("SealedClasses$SAC7")), CheckFor.NOT_SEALED);
    }

    enum CheckFor {
        SEALED {
            void check(ClassFile classFile) throws Exception {
                boolean found = false;
                for (Attribute attr: classFile.attributes) {
                    if (attr.getName(classFile.constant_pool).equals("PermittedSubclasses")) {
                        PermittedSubclasses_attribute permittedSubclasses = (PermittedSubclasses_attribute)attr;
                        found = true;
                        if (permittedSubclasses.subtypes == null || permittedSubclasses.subtypes.length == 0) {
                            throw new AssertionError(classFile.getName() + " should be sealed");
                        }
                    }
                }
                if (!found) {
                    throw new AssertionError(classFile.getName() + " should be sealed");
                }
            }
        },
        FINAL {
            void check(ClassFile classFile) throws Exception {
                if ((classFile.access_flags.flags & Flags.FINAL) == 0) {
                    throw new AssertionError(classFile.getName() + " should be final");
                }
            }
        },
        NOT_SEALED {
            void check(ClassFile classFile) throws Exception {
                for (Attribute attr: classFile.attributes) {
                    if (attr.getName(classFile.constant_pool).equals("PermittedSubclasses")) {
                        throw new AssertionError(classFile.getName() + " should not be sealed");
                    }
                }
                if ((classFile.access_flags.flags & Flags.FINAL) != 0) {
                    throw new AssertionError(classFile.getName() + " should not be final");
                }
            }
        };

        abstract void check(ClassFile classFile) throws Exception;
    }

    void checkClassFile(final ClassFile classFile, CheckFor... checkFor) throws Exception {
        for (CheckFor whatToCheckFor : checkFor) {
            whatToCheckFor.check(classFile);
        }
    }
}
