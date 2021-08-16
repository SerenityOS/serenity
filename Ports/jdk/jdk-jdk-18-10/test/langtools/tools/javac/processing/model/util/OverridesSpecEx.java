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
 * @bug     6453386
 * @summary Verify that example code in Elements.overrides works as spec'ed.
 * @author  Scott Seligman
 * @library /tools/javac/lib
 * @modules java.compiler
 *          jdk.compiler
 * @build JavacTestingAbstractProcessor
 * @compile -g OverridesSpecEx.java
 * @compile -processor OverridesSpecEx -proc:only OverridesSpecEx.java
 */

import java.util.Set;
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.*;

import static javax.lang.model.util.ElementFilter.*;

public class OverridesSpecEx extends JavacTestingAbstractProcessor {
    public boolean process(Set<? extends TypeElement> annoTypes,
                           RoundEnvironment round) {
        if (!round.processingOver())
            doit(annoTypes, round);
        return true;
    }

    private void doit(Set<? extends TypeElement> annoTypes,
                      RoundEnvironment round) {
        TypeElement string = elements.getTypeElement("java.lang.String");
        TypeElement object = elements.getTypeElement("java.lang.Object");

        ExecutableElement m1 = null;
        ExecutableElement m2 = null;
        for (ExecutableElement m : methodsIn(string.getEnclosedElements())) {
            if (m.getSimpleName().contentEquals("hashCode")) {
                m1 = m;
                break;
            }
        }
        for (ExecutableElement m : methodsIn(object.getEnclosedElements())) {
            if (m.getSimpleName().contentEquals("hashCode")) {
                m2 = m;
                break;
            }
        }

        boolean res =
            elements.overrides(m1, m2, (TypeElement) m1.getEnclosingElement());
        System.out.println("String.hashCode overrides Object.hashCode?  " + res);
        checkResult(res);

        TypeElement a = elements.getTypeElement("OverridesSpecEx.A");
        TypeElement b = elements.getTypeElement("OverridesSpecEx.B");
        TypeElement c = elements.getTypeElement("OverridesSpecEx.C");

        m1 = null;
        m2 = null;
        for (ExecutableElement m : methodsIn(a.getEnclosedElements()))
            m1 = m;
        for (ExecutableElement m : methodsIn(b.getEnclosedElements()))
            m2 = m;

        res = elements.overrides(m1, m2, a);
        System.out.println("A.m overrides B.m in B?  " + res);
        checkResult(!res);
        res = elements.overrides(m1, m2, c);
        System.out.println("A.m overrides B.m in C?  " + res);
        checkResult(res);
    }

    private static void checkResult(boolean truthiness) {
        if (!truthiness)
            throw new AssertionError("Bogus result");
    }

    // Fodder for the processor
    class A {
        public void m() {}
    }
    interface B {
        void m();
    }
    class C extends A implements B {
    }
}
