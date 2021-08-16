/*
 * Copyright (c) 2003, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4897937
 * @run main MaskSyntheticModifierTest
 * @summary Verify that the presence of the JVM_ACC_SYNTHETIC bit in the
 *          modifiers of fields and methods does not affect default
 *          serialVersionUID calculation.
 */

import java.io.File;
import java.io.ObjectStreamClass;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class MaskSyntheticModifierTest {
    public static void main(String[] args) throws Exception {
        setup();

        long suid = ObjectStreamClass.lookup(Foo.class).getSerialVersionUID();
        if (suid != 8027844768744011556L) {
            throw new Error("incorrect serialVersionUID: " + suid);
        }
    }

    private static void setup() throws Exception {
        // Copy the class file to the first component of the class path
        String cp = System.getProperty("java.class.path");
        String cp1 = cp.substring(0, cp.indexOf(File.pathSeparatorChar));
        Files.copy(Paths.get(System.getProperty("test.src"), "Foo.class"),
                Paths.get(cp1, "Foo.class"), StandardCopyOption.REPLACE_EXISTING);
    }
}
