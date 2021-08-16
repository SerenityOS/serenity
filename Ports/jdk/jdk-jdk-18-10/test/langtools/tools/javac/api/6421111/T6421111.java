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
 * @bug     6421111
 * @summary NullPointerException thrown when retrieving bounds for the type parameter
 * @author  Peter von der Ah\u00e9
 * @library ../lib
 * @modules java.compiler
 *          jdk.compiler
 * @build ToolTester
 * @compile -Xlint:all T6421111.java
 * @run main T6421111
 */

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeVariable;
import javax.tools.SimpleJavaFileObject;

import static javax.tools.JavaFileObject.Kind.SOURCE;

public class T6421111 extends ToolTester {
    void test(String... args) {
        class Test1 extends SimpleJavaFileObject {
            Test1() {
                super(URI.create("myfo:///Test1.java"), SOURCE);
            }
            @Override
            public String getCharContent(boolean ignoreEncodingErrors) {
                return "class Test1<T extends Thread & Runnable> {}";
            }
        }
        class Test2 extends SimpleJavaFileObject {
            Test2() {
                super(URI.create("myfo:///Test2.java"), SOURCE);
            }
            @Override
            public String getCharContent(boolean ignoreEncodingErrors) {
                return "class Test2<T extends Test2<T> & Runnable> {}";
            }
        }
        task = tool.getTask(null, fm, null, Collections.singleton("-Xlint:all"), null,
                            Arrays.asList(new Test1(), new Test2()));
        task.setProcessors(Collections.singleton(new MyProcessor()));
        if (!task.call())
            throw new AssertionError("Annotation processor failed");
    }
    @SupportedAnnotationTypes("*")
    static class MyProcessor extends AbstractProcessor {
        void test(TypeElement element, boolean fbound) {
            TypeParameterElement tpe = element.getTypeParameters().iterator().next();
            tpe.getBounds().getClass();
            if (fbound) {
                DeclaredType type = (DeclaredType)tpe.getBounds().get(0);
                if (type.asElement() != element)
                    throw error("%s != %s", type.asElement(), element);
                TypeVariable tv = (TypeVariable)type.getTypeArguments().get(0);
                if (tv.asElement() != tpe)
                    throw error("%s != %s", tv.asElement(), tpe);
            }
        }
        public boolean process(Set<? extends TypeElement> annotations,
                               RoundEnvironment roundEnv) {
            test(processingEnv.getElementUtils().getTypeElement("Test1"), false);
            test(processingEnv.getElementUtils().getTypeElement("Test2"), true);
            return false;
        }
        @Override
        public SourceVersion getSupportedSourceVersion() {
            return SourceVersion.latest();
        }
    }
    public static void main(String... args) throws IOException {
        try (T6421111 t = new T6421111()) {
            t.test(args);
        }
    }
    public static AssertionError error(String format, Object... args) {
        return new AssertionError(String.format(format, args));
    }
}
