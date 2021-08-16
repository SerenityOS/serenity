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
 * @bug     6346251
 * @summary Test Elements.getBinaryName
 * @author  Scott Seligman
 * @library /tools/javac/lib
 * @modules java.compiler
 *          jdk.compiler
 * @build   JavacTestingAbstractProcessor BinaryName
 * @compile -processor BinaryName -proc:only BinaryName.java
 */

import java.util.Set;
import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.*;

import static javax.lang.model.util.ElementFilter.typesIn;

@HelloIm("BinaryName")
public class BinaryName extends JavacTestingAbstractProcessor {
    public boolean process(Set<? extends TypeElement> tes,
                           RoundEnvironment round) {
        if (round.processingOver()) return true;

        Set<? extends TypeElement> ts = typesIn(round.getElementsAnnotatedWith(
                elements.getTypeElement("HelloIm")));

        boolean success = true;
        for (TypeElement t : ts) {
            String expected = t.getAnnotation(HelloIm.class).value();
            CharSequence found = elements.getBinaryName(t);
            if (expected.contentEquals(found)) {
                System.out.println(expected + " == " + found);
            } else {
                success = false;
                System.out.println(expected + " != " + found + "  [FAIL]");
            }
        }
        if (! success)
            throw new AssertionError();
        return true;
    }

    @HelloIm("BinaryName$Nested")
    private static class Nested {
    }
}

@interface HelloIm {
    String value();
}
