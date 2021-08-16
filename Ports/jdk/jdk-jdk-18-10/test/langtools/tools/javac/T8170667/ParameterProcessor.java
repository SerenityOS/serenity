/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

import static java.util.stream.Collectors.toCollection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;

/*
 * @test
 * @bug 8170667
 * @summary ClassReader assigns method parameters from MethodParameters incorrectly when long/double
 * parameters are present
 * @library /tools/javac/lib
 * @modules java.compiler
 *          jdk.compiler
 * @compile -parameters ParameterProcessor.java
 * @compile/process -proc:only -processor ParameterProcessor ParameterProcessor
 */
@SupportedAnnotationTypes("ParameterProcessor.ParameterNames")
public class ParameterProcessor extends JavacTestingAbstractProcessor {

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface ParameterNames {
        String[] value() default {};
    }

    @ParameterProcessor.ParameterNames({"a", "b", "c"})
    void f(int a, int b, int c) {}

    @ParameterProcessor.ParameterNames({"d", "e", "f"})
    void g(int d, long e, int f) {}

    @ParameterProcessor.ParameterNames({"g", "h", "i", "j"})
    void h(int g, double h, int i, int j) {}

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(ParameterNames.class)) {
            ParameterNames names = element.getAnnotation(ParameterNames.class);
            if (names == null) {
                continue;
            }
            List<String> expected = Arrays.asList(names.value());
            List<String> actual =
                    ((ExecutableElement) element)
                            .getParameters()
                            .stream()
                            .map(p -> p.getSimpleName().toString())
                            .collect(toCollection(ArrayList::new));
            if (!expected.equals(actual)) {
                String message =
                        String.format(
                                "bad parameter names for %s#%s; expected: %s, was: %s",
                                element, element, expected, actual);
                messager.printMessage(Kind.ERROR, message);
            }
        }
        return false;
    }
}
