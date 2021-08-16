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
 * @bug 8028504
 * @summary javac generates LocalVariableTable even with -g:none
 * @modules jdk.jdeps/com.sun.tools.classfile
 * @compile -g:none DontGenerateLVTForGNoneOpTest.java
 * @run main DontGenerateLVTForGNoneOpTest
 */

import java.io.File;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.nio.file.Paths;

import com.sun.tools.classfile.Attribute;
import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.Code_attribute;
import com.sun.tools.classfile.Method;

public class DontGenerateLVTForGNoneOpTest {

    public static void main(String[] args) throws Exception {
        new DontGenerateLVTForGNoneOpTest().run();
    }

    void run() throws Exception {
        checkClassFile(new File(Paths.get(System.getProperty("test.classes"),
                this.getClass().getName() + ".class").toUri()));
    }

    void checkClassFile(final File cfile) throws Exception {
        ClassFile classFile = ClassFile.read(cfile);
        for (Method method : classFile.methods) {
            Code_attribute code = (Code_attribute)method.attributes.get(Attribute.Code);
            if (code != null) {
                if (code.attributes.get(Attribute.LocalVariableTable) != null) {
                    throw new AssertionError("LVT shouldn't be generated for g:none");
                }
            }
        }
    }

    public void bar() {
        try {
            System.out.println();
        } catch(@TA Exception e) {
        } catch(Throwable t) {}
    }

    @Target(ElementType.TYPE_USE)
    @interface TA {}
}
