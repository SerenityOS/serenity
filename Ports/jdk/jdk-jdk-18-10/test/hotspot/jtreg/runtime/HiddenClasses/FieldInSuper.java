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

/*
 * @test
 * @bug 8238550
 * @summary Test that verifer assignability checks involving the 'this' pointer
 *          work ok for hidden classes.
 * @compile FieldInSuperSub.jasm
 * @run main/othervm -Xverify:remote FieldInSuper
 */

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import static java.lang.invoke.MethodHandles.Lookup.ClassOption.*;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FieldInSuper {

    static final Path CLASSES_DIR = Paths.get(System.getProperty("test.classes"));
    String hello = "hello";

    static byte[] readClassFile(String classFileName) throws Exception {
        File classFile = new File(CLASSES_DIR + File.separator + classFileName);
        try (FileInputStream in = new FileInputStream(classFile);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            int b;
            while ((b = in.read()) != -1) {
                out.write(b);
            }
            return out.toByteArray();
        }
    }

    public static void main(String[] args) throws Throwable {
        Lookup lookup = MethodHandles.lookup();
        byte[] bytes = readClassFile("FieldInSuperSub.class");

        // Define a hidden class that loads the 'this' pointer and then
        // references field 'hello' using:
        //   getfield Field FieldInSuper.hello:"Ljava/lang/String;";
        // This will cause the verifier to check that the 'this' pointer for
        // the hidden class is assignable to FieldInSuper.
        Class<?> c = lookup.defineHiddenClass(bytes, true, NESTMATE).lookupClass();
        Object fss = c.newInstance();
        c.getMethod("printMe").invoke(fss);
    }

}
