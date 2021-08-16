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
 * @bug     6413690 6380018
 * @summary JavacProcessingEnvironment does not enter trees from preceding rounds
 * @author  Peter von der Ah\u00e9
 * @library /tools/javac/lib
 * @modules java.compiler
 *          jdk.compiler
 * @build   JavacTestingAbstractProcessor
 * @compile T6413690.java
 * @compile -XDfatalEnterError -verbose -processor T6413690 src/Super.java TestMe.java
 */

import java.io.IOException;
import java.io.Writer;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

@SupportedAnnotationTypes("TestMe")
public class T6413690 extends JavacTestingAbstractProcessor {
    public boolean process(Set<? extends TypeElement> annotations,
                           RoundEnvironment roundEnvironment) {
        TypeElement testMe = elements.getTypeElement(TestMe.class.getName());
        Set<? extends Element> supers = roundEnvironment.getElementsAnnotatedWith(testMe);
        try {
            for (Element sup : supers) {
                Writer sub = filer.createSourceFile(sup.getSimpleName() + "_GENERATED").openWriter();
                sub.write(String.format("class %s_GENERATED extends %s {}",
                                        sup.getSimpleName(),
                                        ((TypeElement)sup).getQualifiedName()));
                sub.close();
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return true;
    }
}
