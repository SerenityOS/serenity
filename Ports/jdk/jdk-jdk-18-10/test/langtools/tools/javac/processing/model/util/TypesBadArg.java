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
 * @bug     6345812
 * @summary Validate argument kinds in Types utilities
 * @author  Scott Seligman
 * @library /tools/javac/lib
 * @modules java.compiler
 *          jdk.compiler
 * @build   JavacTestingAbstractProcessor TypesBadArg
 * @compile -processor TypesBadArg -proc:only TypesBadArg.java
 */

import java.util.Set;
import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.*;

public class TypesBadArg extends JavacTestingAbstractProcessor {
    boolean success = true;

    public boolean process(Set<? extends TypeElement> tes,
                           RoundEnvironment round) {
        if (round.processingOver()) return true;

        final Elements elements = processingEnv.getElementUtils();
        final Types types = processingEnv.getTypeUtils();

        final TypeMirror javaLang =
            elements.getPackageElement("java.lang").asType();

        makeBadCall(new Runnable() {
            public void run() {
                types.isSubtype(javaLang, javaLang);
            }
        });
        makeBadCall(new Runnable() {
            public void run() {
                types.isAssignable(javaLang, javaLang);
            }
        });
        makeBadCall(new Runnable() {
            public void run() {
                types.contains(javaLang, javaLang);
            }
        });
        makeBadCall(new Runnable() {
            public void run() {
                types.directSupertypes(javaLang);
            }
        });
        makeBadCall(new Runnable() {
            public void run() {
                types.erasure(javaLang);
            }
        });
        makeBadCall(new Runnable() {
            public void run() {
                types.capture(javaLang);
            }
        });
        makeBadCall(new Runnable() {
            public void run() {
                types.unboxedType(javaLang);
            }
        });
        makeBadCall(new Runnable() {
            public void run() {
                types.unboxedType(types.getNoType(TypeKind.VOID));
            }
        });
        if (! success)
            throw new AssertionError("Some test(s) failed.");
        return true;
    }

    private void makeBadCall(Runnable runnable) {
        try {
            runnable.run();
            System.out.println("Failure: IllegalArgumentException expected");
            success = false;
        } catch (IllegalArgumentException e) {
            System.out.println("IllegalArgumentException as expected");
        }
    }
}
