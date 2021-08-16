/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8166700
 * @summary Check that local classes originating in static initializer can be loaded properly.
 * @modules jdk.compiler
 * @library /tools/javac/lib
 * @build LocalTest$1Local LocalTest$2Local LocalTest$3Local LocalTest$4Local LocalTest$5Local LocalTest JavacTestingAbstractProcessor
 * @compile LocalClassesModel.java
 * @compile/process/ref=LocalClassesModel.out -processor LocalClassesModel LocalTest$1Local LocalTest$2Local LocalTest$3Local LocalTest$4Local LocalTest$5Local LocalTest
 */

import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;

public class LocalClassesModel extends JavacTestingAbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement root : ElementFilter.typesIn(roundEnv.getRootElements())) {
            System.out.println(processingEnv.getElementUtils().getBinaryName(root));
            for (ExecutableElement constr : ElementFilter.constructorsIn(root.getEnclosedElements())) {
                System.out.print("  (");
                boolean first = true;
                for (VariableElement param : constr.getParameters()) {
                    if (!first) {
                        System.out.print(", ");
                    }
                    first = false;
                    System.out.print(param.asType().toString());
                }
                System.out.println(")");
            }
        }

        return false;
    }
}
