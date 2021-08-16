/*
 * Copyright (c) 2010, 2019, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug     6551367
 * @summary javadoc throws ClassCastException when an link tag tries to reference constructor.
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @run main T6551367 T6551367.java
 */

/*
 * ***NOTE: This is not a tool test!, should be moved to doclets.***
 */
import java.io.File;

import jdk.javadoc.doclet.DocletEnvironment;

public class T6551367 {
    public T6551367() {}
    public boolean run(DocletEnvironment root) {
        return true;
    }
    /** Here, in the javadoc for this method, I try to link to
     *  {@link #<init> a constructor}.
     */
    public static void main(String... args) {
        File testSrc = new File(System.getProperty("test.src", "."));
        File destDir = new File(System.getProperty("user.dir", "."));
        for (String file : args) {
            File source = new File(testSrc, file);
            String[] array = {
                "-Xdoclint:none",
                source.getPath(),
                "-d",
                destDir.getAbsolutePath()
            };

            int rc = jdk.javadoc.internal.tool.Main.execute(array);
            if (rc != 0)
                throw new Error("unexpected exit from javadoc: " + rc);
        }
    }
}
