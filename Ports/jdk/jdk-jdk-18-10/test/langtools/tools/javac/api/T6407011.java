/*
 * Copyright (c) 2006, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6407011 6407066
 * @summary javac crashes in b78 with NPE in JavacFileManager:293
 * @author  Peter von der Ah\u00e9
 * @modules java.compiler
 *          jdk.compiler
 */

import java.io.File;
import javax.tools.*;

public class T6407011 {
    public static void main(String... args) {
        JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        String srcdir = System.getProperty("test.src");
        File file = new File(srcdir, T6407011.class.getSimpleName() + ".java");
        if (tool.run(null, null, null, "-sourcepath", "/x y z", "-d", ".", file.getPath()) != 0)
            throw new AssertionError("non zero return code from compiler");
    }
}
