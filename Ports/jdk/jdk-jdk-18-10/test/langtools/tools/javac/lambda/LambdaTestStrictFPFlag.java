/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8046060
 * @summary Different results of floating point multiplication for lambda code block
 * @modules jdk.jdeps/com.sun.tools.classfile
 * @compile -source 16 -target 16 LambdaTestStrictFPFlag.java
 * @run main LambdaTestStrictFPFlag
 */

import java.io.*;
import java.net.URL;
import com.sun.tools.classfile.*;
import static com.sun.tools.classfile.AccessFlags.ACC_STRICT;

public class LambdaTestStrictFPFlag {
    public static void main(String[] args) throws Exception {
        new LambdaTestStrictFPFlag().run();
    }

    void run() throws Exception {
        ClassFile cf = getClassFile("LambdaTestStrictFPFlag$Test.class");
        ConstantPool cp = cf.constant_pool;
        boolean found = false;
        for (Method meth: cf.methods) {
            if (meth.getName(cp).startsWith("lambda$")) {
                if ((meth.access_flags.flags & ACC_STRICT) == 0) {
                    throw new Exception("strict flag missing from lambda");
                }
                found = true;
            }
        }
        if (!found) {
            throw new Exception("did not find lambda method");
        }
    }

    ClassFile getClassFile(String name) throws IOException, ConstantPoolException {
        URL url = getClass().getResource(name);
        InputStream in = url.openStream();
        try {
            return ClassFile.read(in);
        } finally {
            in.close();
        }
    }

    class Test {
        strictfp void test() {
            Face itf = () -> { };
        }
    }

    interface Face {
        void m();
    }
}
