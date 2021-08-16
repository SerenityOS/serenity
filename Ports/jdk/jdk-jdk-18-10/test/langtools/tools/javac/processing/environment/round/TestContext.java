/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6988836
 * @summary A new JavacElements is created for each round of annotation processing
 * @library /tools/javac/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.model
 *          jdk.compiler/com.sun.tools.javac.processing
 *          jdk.compiler/com.sun.tools.javac.util
 * @build JavacTestingAbstractProcessor TestContext
 * @compile/process -processor TestContext -XprintRounds TestContext
 */

import java.io.*;
import java.util.*;
import javax.annotation.processing.*;
import javax.lang.model.element.*;
import static javax.tools.Diagnostic.Kind.*;

import com.sun.source.util.Trees;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.model.JavacTypes;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;

public class TestContext extends JavacTestingAbstractProcessor {

    Trees treeUtils;
    int round = 0;

    @Override
    public void init(ProcessingEnvironment pEnv) {
        super.init(pEnv);
        treeUtils = Trees.instance(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        round++;

        JavacProcessingEnvironment jpe = (JavacProcessingEnvironment) processingEnv;
        Context c = jpe.getContext();
        check(c.get(JavacElements.class), eltUtils);
        check(c.get(JavacTypes.class), typeUtils);
        check(c.get(JavacTrees.class), treeUtils);

        final int MAXROUNDS = 3;
        if (round < MAXROUNDS)
            generateSource("Gen" + round);

        return true;
    }

    <T> void check(T actual, T expected) {
//        messager.printMessage(NOTE, "expect: " + expected);
//        messager.printMessage(NOTE, "actual: " + actual);

        if (actual != expected) {
            messager.printMessage(ERROR,
                "round " + round + " unexpected value for " + expected.getClass().getName() + ": " + actual);
        }
    }

    void generateSource(String name) {
        String text = "class " + name + " { }\n";

        try (Writer out = filer.createSourceFile(name).openWriter()) {
                out.write(text);
        } catch (IOException e) {
            throw new Error(e);
        }
    }

}

