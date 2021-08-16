/*
 * Copyright (c) 2008, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6608214
 * @summary Exception throw while analysing a file with error
 * @author  Maurizio Cimadamore
 * @modules jdk.compiler
 */

import com.sun.source.util.JavacTask;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import static javax.tools.JavaFileObject.Kind;

public class T6608214 {
    public static void main(String[] args) throws IOException {
        JavaFileObject sfo = new SimpleJavaFileObject(URI.create(""),Kind.SOURCE) {
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return "class Test<S> { <T extends S & Runnable> void test(){}}";
            }
        };
        List<? extends JavaFileObject> files = Arrays.asList(sfo);
        List<String> opts = Arrays.asList("-Xjcov");
        JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        JavacTask ct = (JavacTask)tool.getTask(null, null, null,opts,null,files);
        ct.analyze();
    }
}
