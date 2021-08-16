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
 * @bug 6987384
 * @summary -XprintProcessorRoundsInfo message printed with different timing than previous
 * @library /tools/javac/lib
 * @modules java.compiler
 *          jdk.compiler
 * @build JavacTestingAbstractProcessor Test
 * @compile/fail/ref=Test.out -XDrawDiagnostics -XprintProcessorInfo -Werror -proc:only -processor Test Test.java
 */

import java.io.*;
import java.util.*;
import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;
import javax.lang.model.util.*;
import javax.tools.*;

public class Test extends JavacTestingAbstractProcessor {
    final int MAX_ROUNDS = 3;
    int round = 0;

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        round++;
        messager.printMessage(Diagnostic.Kind.NOTE, "round " + round);
        if (round <= MAX_ROUNDS)
            generateSource("Gen" + round);
        if (roundEnv.processingOver())
            messager.printMessage(Diagnostic.Kind.WARNING, "last round");
        return true;
    }

    void generateSource(String name) {
        String text = "class " + name + " { }\n";

        // avoid try-with-resources so test can be run on older builds
        try {
            Writer out = filer.createSourceFile(name).openWriter();
            try {
                out.write(text);
            } finally {
                out.close();
            }
        } catch (IOException e) {
            throw new Error(e);
        }
    }
}



