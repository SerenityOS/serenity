/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8005046 8011052 8025087
 * @summary Test basic properties of javax.lang.element.ExecutableElement
 * @author  Joseph D. Darcy
 * @library /tools/javac/lib
 * @modules java.compiler
 *          jdk.compiler
 * @build   JavacTestingAbstractProcessor TestExecutableElement
 * @compile -processor TestExecutableElement -proc:only -AexpectedMethodCount=7 TestExecutableElement.java
 * @compile/process -processor TestExecutableElement -proc:only -AexpectedMethodCount=3 ProviderOfDefault
 */

import java.lang.annotation.*;
import java.util.Formatter;
import java.util.Set;
import java.util.regex.*;
import javax.annotation.processing.*;
import javax.lang.model.element.*;
import static javax.lang.model.util.ElementFilter.*;
import static javax.tools.Diagnostic.Kind.*;

/**
 * Test some basic workings of javax.lang.element.ExecutableElement
 */
@SupportedOptions("expectedMethodCount")
public class TestExecutableElement extends JavacTestingAbstractProcessor implements ProviderOfDefault {
    private int seenMethods = 0;
    @IsDefault(false)
    public boolean process(Set<? extends TypeElement> annotations,
                           RoundEnvironment roundEnv) {
        if (!roundEnv.processingOver()) {
            for (Element element : roundEnv.getRootElements()) {
                for (ExecutableElement method : methodsIn(element.getEnclosedElements())) {
                    checkIsDefault(method);
                    seenMethods++;
                }
            }
        } else {
            String expectedMethodCountStr = processingEnv.getOptions().get("expectedMethodCount");
            if (expectedMethodCountStr == null) {
                messager.printMessage(ERROR, "No expected method count specified.");
            } else {
                int expectedMethodCount = Integer.parseInt(expectedMethodCountStr);

                if (seenMethods != expectedMethodCount) {
                    messager.printMessage(ERROR, "Wrong number of seen methods: " + seenMethods);
                }
            }
        }
        return true;
    }

    @IsDefault(false)
    void checkIsDefault(ExecutableElement method) {
        System.out.println("Testing " + method);
        IsDefault expectedIsDefault = method.getAnnotation(IsDefault.class);

        boolean expectedDefault = (expectedIsDefault != null) ?
            expectedIsDefault.value() :
            false;

        boolean methodIsDefault = method.isDefault();

        if (expectedDefault) {
            if (!method.getModifiers().contains(Modifier.DEFAULT)) {
                messager.printMessage(ERROR,
                                      "Modifier \"default\" not present as expected.",
                                      method);
            }

            // Check printing output
            java.io.Writer stringWriter = new java.io.StringWriter();
            eltUtils.printElements(stringWriter, method);
            Pattern p = Pattern.compile(expectedIsDefault.expectedTextRegex(), Pattern.DOTALL);

            if (! p.matcher(stringWriter.toString()).matches()) {
                messager.printMessage(ERROR,
                                      new Formatter().format("Unexpected printing ouptput:%n\tgot %s,%n\texpected pattern %s.",
                                                             stringWriter.toString(),
                                                             expectedIsDefault.expectedTextRegex()).toString(),
                                      method);
            }

            System.out.println("\t" + stringWriter.toString());

        } else {
            if (method.getModifiers().contains(Modifier.DEFAULT)) {
                messager.printMessage(ERROR,
                                      "Modifier \"default\" present when not expected.",
                                      method);
            }
        }

        if (methodIsDefault != expectedDefault) {
            messager.printMessage(ERROR,
                                  new Formatter().format("Unexpected Executable.isDefault result: got ``%s'', expected ``%s''.",
                                                         expectedDefault,
                                                         methodIsDefault).toString(),
                                  method);
        }
    }
}

/**
 * Expected value of the ExecutableElement.isDefault method.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface IsDefault {
    boolean value();
    String expectedTextRegex() default "";
}

/**
 * Test interface to provide a default method.
 */
interface ProviderOfDefault {
    @IsDefault(false)
    boolean process(Set<? extends TypeElement> annotations,
                    RoundEnvironment roundEnv);

    @IsDefault(value=true, expectedTextRegex="\\s*@IsDefault\\(.*\\)\\s*default void quux\\(\\);\\s*$")
    default void quux() {};
    @IsDefault(false)
    static void statik() {}
}
