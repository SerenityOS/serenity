/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8136453
 * @summary Checking that javac's ClassReader expands its parameterNameIndices array properly.
 * @modules jdk.compiler
 * @build T T8136453
 * @run main T8136453
 */

import java.util.Arrays;
import java.util.List;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import com.sun.source.util.JavacTask;

public class T8136453 {
    public static void main(String... args) {
        new T8136453().run();
    }

    void run() {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        List<String> opts = Arrays.asList("-parameters");
        JavacTask task = (JavacTask) compiler.getTask(null, null, null, opts, null, null);
        TypeElement t = task.getElements().getTypeElement("T");
        ExecutableElement testMethod = ElementFilter.methodsIn(t.getEnclosedElements()).get(0);
        VariableElement param = testMethod.getParameters().get(0);
        Name paramName = param.getSimpleName();

        if (!paramName.contentEquals("p")) {
            throw new AssertionError("Wrong parameter name: " + paramName);
        }
    }
}
