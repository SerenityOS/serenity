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
 * @bug 6458749
 * @summary  TypeParameterElement.getEnclosedElements() throws NPE within javac
 * @modules java.compiler
 *          jdk.compiler
 * @build T6458749
 * @compile -processor T6458749 -proc:only T6458749.java
 */

import java.util.Set;
import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.SourceVersion;

@SupportedAnnotationTypes("*")
public class T6458749<T> extends AbstractProcessor {
    public boolean process(Set<? extends TypeElement> tes, RoundEnvironment renv) {
        if (!renv.processingOver()) {
            for(TypeElement e : ElementFilter.typesIn(renv.getRootElements())) {
                System.out.printf("Element %s:%n", e.toString());
                try {
                    for (TypeParameterElement tp : e.getTypeParameters()) {
                        System.out.printf("Type param %s", tp.toString());
                        if (! tp.getEnclosedElements().isEmpty()) {
                            throw new AssertionError("TypeParameterElement.getEnclosedElements() should return empty list");
                        }
                    }
                } catch (NullPointerException npe) {
                    throw new AssertionError("NPE from TypeParameterElement.getEnclosedElements()", npe);
                }
            }
        }
        return true;
    }

    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }
}
