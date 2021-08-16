/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Elements hides does not work correctly with interface methods
 * @library /tools/javac/lib
 * @modules java.compiler
 *          jdk.compiler
 * @build   JavacTestingAbstractProcessor Hides
 * @compile -processor Hides -proc:only C.java I.java
 */

import java.util.Set;
import java.util.List;
import javax.annotation.processing.*;
import javax.lang.model.element.*;

import static javax.lang.model.util.ElementFilter.*;

public class Hides extends JavacTestingAbstractProcessor {

    VariableElement getField(TypeElement te) {
        List<VariableElement> fields = fieldsIn(te.getEnclosedElements());
        if (fields.size() != 1) {
            throw new AssertionError("Expected only one field in: " + te);
        }
        return fields.get(0);
    }

    ExecutableElement getMethod(TypeElement te) {
        List<ExecutableElement> methods = methodsIn(te.getEnclosedElements());
        if (methods.size() != 1) {
            throw new AssertionError("Expected only one method in: " + te);
        }
        return methods.get(0);
    }

    TypeElement getIC(TypeElement te) {
        List<TypeElement> ics = typesIn(te.getEnclosedElements());
        if (ics.size() != 1) {
            throw new AssertionError("Expected only one inner class in: " + te);
        }
        return ics.get(0);
    }

    public boolean process(Set<? extends TypeElement> tes,
                           RoundEnvironment round) {
        if (round.processingOver())
            return true;

        TypeElement klass = null;
        TypeElement intfc = null;

        for (TypeElement te : typesIn(round.getRootElements())) {
            switch (te.getKind()) {
                case INTERFACE:
                    intfc = te;
                    break;
                case CLASS:
                    klass = te;
                    break;
                default:
                    throw new AssertionError("don't know what this is: " + te);
            }
        }

        for (Element e : klass.getEnclosedElements()) {
            switch (e.getKind()) {
                case FIELD:
                    check(e, getField(intfc));
                    break;
                case METHOD:
                    check(e, getMethod(intfc));
                    break;
                case CLASS:
                    check(e, getIC(intfc));
                default:
                    break;
            }
        }

        if (!status)
            throw new Error("Test fails");
        return true;
    }
    boolean status = true;

    String getFQN(Element e) {
        return e.getEnclosingElement() + "." + e;
    }

    void check(Element e1, Element e2) {
        if (eltUtils.hides(e1, e2)) {
            System.err.println("Pass: " + getFQN(e1) + " hides: " + getFQN(e2));
        } else {
            System.err.println("Fail: Expected: " + getFQN(e1) + " to hide: " + getFQN(e2));
            status = false;
        }
    }
}
