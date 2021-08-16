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

/*
 * @test
 * @bug 6400383
 * @summary directory foo.java on javac command line causes javac to crash
 * @modules jdk.compiler/com.sun.tools.javac.api
 */

import java.io.*;
import com.sun.tools.javac.api.*;

public class T6400383 {
    public static void main(String... args) {
        File foo = new File("foo.java");
        foo.delete();

        // case 1: file not found
        JavacTool tool = JavacTool.create();
        StringStream out = new StringStream();
        tool.run(null, out, out, foo.getPath());
        check(out.toString());


        // case 2: file is a directory
        out.clear();
        try {
            foo.mkdir();
            tool.run(null, out, out, foo.getPath());
            check(out.toString());
        } finally {
            foo.delete();
        }
    }

    private static void check(String s) {
        System.err.println(s);
        // If the compiler crashed and caught the error, it will print out
        // the "oh golly, I crashed!" message, which will contain the Java
        // name of the exception in the stack trace ... so look for the
        // string "Exception" or "Error".
        if (s.indexOf("Exception") != -1 || s.indexOf("Error") != -1)
            throw new AssertionError("found exception");
    }

    private static class StringStream extends OutputStream {
        public void write(int i) {
            sb.append((char) i);
        }

        void clear() {
            sb.setLength(0);
        }

        public String toString() {
            return sb.toString();
        }

        private StringBuilder sb = new StringBuilder();
    }
}
