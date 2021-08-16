/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.*;
import java.util.Set;
import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.lang.model.util.ElementFilter;
import javax.tools.*;
import com.sun.tools.javac.util.Assert;

public class Processor extends JavacTestingAbstractProcessor {
    private boolean seenGenerated;

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element e : roundEnv.getElementsAnnotatedWith(Gen.class)) {
            Gen gen = e.getAnnotation(Gen.class);
            try {
                JavaFileObject source = processingEnv.getFiler().createSourceFile(gen.fileName());

                try (Writer out = source.openWriter()) {
                    out.write(gen.content());
                }
            } catch (IOException ex) {
                throw new IllegalStateException(ex);
            }
        }

        TypeElement generated = processingEnv.getElementUtils().getTypeElement("Generated");

        if (generated != null) {
            Check check = ElementFilter.methodsIn(generated.getEnclosedElements()).get(0).getAnnotation(Check.class);

            checkCorrectException(check::classValue, "java.lang.Class<java.lang.String>");
            checkCorrectException(check::intConstValue, "boolean");
            checkCorrectException(check::enumValue, "java.lang.String");
            checkCorrectException(check::incorrectAnnotationValue, "java.lang.Deprecated");
            checkCorrectException(check::incorrectArrayValue, "<any>");
            checkCorrectException(check::incorrectClassValue, "<any>");

            seenGenerated = true;
        }

        if (roundEnv.processingOver() && !seenGenerated) {
            Assert.error("Did not see the generated class!");
        }

        return true;
    }

    private static void checkCorrectException(Runnable runCheck, String expectedType) {
        try {
            runCheck.run();
            Assert.check(false); //Should not reach here
        } catch (AnnotationTypeMismatchException ex) {
            Assert.check(expectedType.equals(ex.foundType()), ex.foundType());
        }
    }

}

@interface Gen {
    String fileName();
    String content();
}

@interface Check {
    Class<? extends Number> classValue();
    int intConstValue();
    E enumValue();
    int incorrectAnnotationValue();
    int incorrectArrayValue();
    Class<?> incorrectClassValue();
}

enum E {
    A;
}
