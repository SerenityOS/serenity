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
 * @bug     6468404
 * @summary ExecutableElement.getParameters() uses raw type for class loaded from -g bytecode
 * @author  jesse.glick@...
 * @author  Peter von der Ah\u00e9
 * @library ../lib
 * @modules java.compiler
 *          jdk.compiler
 * @build ToolTester
 * @compile T6468404.java
 * @run main T6468404
 */

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

public class T6468404 extends ToolTester {
    void test(String... args) {
        System.err.println("Compiling with sources:");
        task = tool.getTask(
                null, fm, null, null,
                null, Collections.singleton(new DummyFO("C")));
        task.setProcessors(Collections.singleton(new P()));
        if (!task.call())
            throw new AssertionError();

        System.err.println("Compiling with binaries w/o -g:");
        task = tool.getTask(
                null, fm, null, null,
                null, Collections.singleton(new DummyFO("Dummy")));
        task.setProcessors(Collections.singleton(new P()));
        if (!task.call())
            throw new AssertionError();

        task = tool.getTask(
                null, fm, null,
                Arrays.asList("-g"),
                null, Collections.singleton(new DummyFO("C")));
        if (!task.call())
            throw new AssertionError();

        System.err.println("Compiling with binaries w/ -g:");
        task = tool.getTask(
                null, fm, null, null,
                null, Collections.singleton(new DummyFO("Dummy")));
        task.setProcessors(Collections.singleton(new P()));
        if (!task.call())
            throw new AssertionError();
    }
    public static void main(String... args) throws IOException {
        try (T6468404 t = new T6468404()) {
            t.test(args);
        }
    }

}

class DummyFO extends SimpleJavaFileObject {
    String n;
    public DummyFO(String n) {
        super(URI.create("nowhere:/" + n + ".java"), JavaFileObject.Kind.SOURCE);
        this.n = n;
    }
    public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
        return "public class " + n + " {" + n + "(java.util.List<String> l) {}}";
    }
}

@SupportedAnnotationTypes("*")
class P extends AbstractProcessor {
    boolean ran = false;

    Elements elements;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        elements = processingEnv.getElementUtils();
    }

    ExecutableElement getFirstMethodIn(String name) {
        return (ExecutableElement)elements.getTypeElement(name).getEnclosedElements().get(0);
    }

    boolean isParameterized(TypeMirror type) {
        return !((DeclaredType)type).getTypeArguments().isEmpty();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (!ran) {
            ran = true;
            ExecutableElement m = getFirstMethodIn("C");
            System.err.println("method: " + m);

            TypeMirror type = (DeclaredType)m.getParameters().get(0).asType();
            System.err.println("parameters[0]: " + type);
            if (!isParameterized(type))
                throw new AssertionError(type);

            type = ((ExecutableType)m.asType()).getParameterTypes().get(0);
            System.err.println("parameterTypes[0]: " + type);
            if (!isParameterized(type))
                throw new AssertionError(type);
            System.err.println();
        }
        return true;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }
}
