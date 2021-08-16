/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4645152 4785453
 * @summary javac compiler incorrectly inserts <clinit> when -g is specified
 * @modules jdk.jdeps/com.sun.tools.classfile
 * @run compile -g ConstDebugTest.java
 * @run main ConstDebugTest
 */
import java.nio.file.Paths;
import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.Method;

public class ConstDebugTest {

    public static final long l = 12;

    public static void main(String args[]) throws Exception {
        ClassFile classFile = ClassFile.read(Paths.get(System.getProperty("test.classes"),
                ConstDebugTest.class.getSimpleName() + ".class"));
        for (Method method: classFile.methods) {
            if (method.getName(classFile.constant_pool).equals("<clinit>")) {
                throw new AssertionError(
                    "javac should not create a <clinit> method for ConstDebugTest class");
            }
        }
    }

}
