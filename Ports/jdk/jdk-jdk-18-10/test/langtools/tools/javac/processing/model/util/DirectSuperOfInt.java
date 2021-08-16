/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     8034933
 * @summary Types.directSupertypes should return Object as the super type of interfaces
 * @library /tools/javac/lib
 * @modules java.compiler
 *          jdk.compiler
 * @build   JavacTestingAbstractProcessor DirectSuperOfInt
 * @compile -processor DirectSuperOfInt -proc:only DirectSuperOfInt.java
 */

import java.util.Set;
import java.util.List;
import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.*;
import static javax.lang.model.util.ElementFilter.*;

public class DirectSuperOfInt extends JavacTestingAbstractProcessor {
    public boolean process(Set<? extends TypeElement> tes,
                           RoundEnvironment round) {
        if (round.processingOver())
            return true;

        boolean tested = false;
        for (TypeElement te : typesIn(round.getRootElements())) {
            if (!te.getSimpleName().contentEquals("DirectSuperOfIntI"))
                continue;

            tested = true;
            List<? extends TypeMirror> supers = types.directSupertypes(te.asType());
            if (supers.size() != 1)
                throw new AssertionError("test failed");

            if (!elements.getTypeElement("java.lang.Object").asType().equals((supers.get(0))))
                throw new AssertionError("test failed");
        }
        if (!tested)
            throw new AssertionError("test failed");
        return true;
    }
}

interface DirectSuperOfIntI {}
