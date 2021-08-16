/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

import static java.util.stream.Collectors.joining;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;

@SupportedAnnotationTypes("MethodParameterProcessor.ParameterNames")
public class MethodParameterProcessor extends JavacTestingAbstractProcessor {

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
    @interface ParameterNames {
        String[] value() default {};
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(ParameterNames.class)) {
            ExecutableElement exec = (ExecutableElement)element;
            String message = printNamesAndAnnotations(exec);
            messager.printMessage(Kind.NOTE, message);
        }
        return false;
    }

    private String printNamesAndAnnotations(ExecutableElement exec) {
        return String.format("%s.%s(%s)",
                exec.getEnclosingElement(),
                exec.getSimpleName(),
                exec.getParameters().stream().map(this::printParameter).collect(joining(", ")));
    }

    private String printParameter(VariableElement param) {
        return param.getAnnotationMirrors().stream().map(String::valueOf).collect(joining(" "))
                + (param.getAnnotationMirrors().isEmpty() ? "" : " ")
                + param.getSimpleName();
    }
}
