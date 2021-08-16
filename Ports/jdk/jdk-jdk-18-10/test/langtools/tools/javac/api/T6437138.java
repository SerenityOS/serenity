/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6437138
 * @summary JSR 199: Compiler doesn't diagnose crash in user code
 * @modules java.compiler
 *          jdk.compiler
 */

import java.net.URI;
import java.util.Arrays;
import javax.tools.*;
import static javax.tools.JavaFileObject.Kind.*;


public class T6437138 {
    static class JFO extends SimpleJavaFileObject {
        public JFO(URI uri, JavaFileObject.Kind kind) {
            super(uri, kind);
        }
        // getCharContent not impl, will throw UnsupportedOperationException
    }

    public static void main(String... arg) throws Exception {
        try {
            JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
            JavaFileObject jfo = new JFO(new URI("JFOTest04.java"),SOURCE);
            JavaCompiler.CompilationTask ct = javac.getTask(null,null,null,null,
                        null, Arrays.asList(jfo));
            ct.call();
            throw new Exception("no exception thrown by JavaCompiler.CompilationTask");
        } catch (RuntimeException e) {
            if (e.getCause() instanceof UnsupportedOperationException) {
                System.err.println("RuntimeException(UnsupportedOperationException) caught as expected");
                return;
            }
            throw new Exception("unexpected exception caught", e);
        }
    }
}

