/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;

import java.nio.file.Path;
import java.nio.file.Paths;

import jdk.test.lib.compiler.CompilerUtils;
import jdk.test.lib.Utils;


interface Test {
    void test();
}

public class HiddenClassApp {
    static void log(String str) { System.out.println(str); }

    static final String HCName = "HiddenClass.java";
    static final Path SRC_DIR = Paths.get(Utils.TEST_SRC, "hidden");
    static final Path CLASSES_DIR = Paths.get(Utils.TEST_CLASSES, "hidden");

    static void compileSources(String sourceFile) throws Throwable {
        boolean ok = CompilerUtils.compile(SRC_DIR.resolve(sourceFile), CLASSES_DIR,
                                           "-cp", Utils.TEST_CLASSES.toString());
        if (!ok){
            throw new RuntimeException("HiddenClassApp: Compilation of the test failed. ");
        }
    }

    static byte[] readClassFile(String classFileName) throws Exception {
        File classFile = new File(CLASSES_DIR + File.separator + classFileName);
        try (FileInputStream in = new FileInputStream(classFile);
             ByteArrayOutputStream out = new ByteArrayOutputStream())
        {
            int b;
            while ((b = in.read()) != -1) {
                out.write(b);
            }
            return out.toByteArray();
        }
    }

    static Class<?> defineHiddenClass(String name) throws Exception {
        Lookup lookup = MethodHandles.lookup();
        byte[] bytes = readClassFile(name + ".class");
        Class<?> hc = lookup.defineHiddenClass(bytes, false).lookupClass();
        return hc;
    }

    public static void main(String args[]) throws Exception {
        log("HiddenClassApp: started");
        try {
            compileSources(HCName);
            log("HiddenClassApp: compiled " + HCName);
        } catch (Throwable t) {
            t.printStackTrace();
            throw new Exception("HiddenClassApp: Failed to compile " + HCName);
        }

        Class<?> c = defineHiddenClass("HiddenClass");
        log("HiddenClassApp: Defined HiddenClass with name: " + c.getName());
        HiddenClassAgent.setHiddenClassLoaded();

        Test t = (Test) c.newInstance();
        t.test();
        log("HiddenClassApp: Tested HiddenClass");

        if (!HiddenClassAgent.checkWaitForCompleteness()) {
            throw new Exception("HiddenClassApp: FAIL: HiddenClassAgent did not complete");
        }
        if (HiddenClassAgent.failed()) {
            throw new Exception("HiddenClassApp: FAIL: HiddenClassAgent failed");
        }
        log("HiddenClassApp: finished");
    }
}
