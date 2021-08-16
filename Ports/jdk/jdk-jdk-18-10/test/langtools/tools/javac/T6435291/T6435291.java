/*
 * Copyright (c) 2006, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6435291
 * @summary javac shouldn't throw NPE while compiling invalid RuntimeInvisibleParameterAnnotations
 * @author  Wei Tao
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.code
 *          jdk.compiler/com.sun.tools.javac.comp
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.util
 * @build T
 * @run main/othervm T6435291
 */

import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.code.ClassFinder.BadClassFile;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.util.Names;
import javax.tools.ToolProvider;

public class T6435291 {
    public static void main(String... args) {
        javax.tools.JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        JavacTaskImpl task = (JavacTaskImpl)tool.getTask(null, null, null, null, null, null);
        Symtab syms = Symtab.instance(task.getContext());
        Names names = Names.instance(task.getContext());
        task.ensureEntered();
        try {
            syms.enterClass(syms.unnamedModule, names.fromString("T")).complete();
        } catch (BadClassFile e) {
            System.err.println("Passed: expected completion failure " + e.getClass().getName());
            return;
        } catch (Exception e) {
            throw new RuntimeException("Failed: unexpected exception");
        }
        throw new RuntimeException("Failed: no error reported");
    }
}
