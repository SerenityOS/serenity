/*
 * Copyright (c) 2006, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6423972
 * @summary Tests TypeParameter.getBounds.
 * @author  Scott Seligman
 * @library /tools/javac/lib
 * @modules java.compiler
 *          jdk.compiler
 * @build   JavacTestingAbstractProcessor TypeParamBounds
 * @compile -processor TypeParamBounds -proc:only TypeParamBounds.java
 */

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.*;

public class TypeParamBounds extends JavacTestingAbstractProcessor {
    public boolean process(Set<? extends TypeElement> annoTypes,
                           RoundEnvironment round) {
        if (!round.processingOver())
            doit(annoTypes, round);
        return true;
    }

    private void doit(Set<? extends TypeElement> annoTypes,
                      RoundEnvironment round) {
        TypeElement gen = elements.getTypeElement("TypeParamBounds.Gen");

        // For each type parameter of Gen, compare its bounds with the
        // bounds that are expected.
        //
        for (TypeParameterElement tparam : gen.getTypeParameters()) {
            System.out.println(tparam);
            List<? extends TypeMirror> bounds = tparam.getBounds();
            String[] expected = Gen.boundNames.get(tparam + "");

            if (bounds.size() != expected.length)
                throw new AssertionError();
            int i = 0;
            for (TypeMirror bound : bounds) {
                Name got = types.asElement(bound).getSimpleName();
                String shoulda = expected[i++];
                System.out.println("  " + got);
                if (!got.contentEquals(shoulda))
                    throw new AssertionError(shoulda);
            }
        }
    }


    // Fodder for the processor
    static class Gen<T, U extends Object, V extends Number, W extends U,
                     X extends Runnable, Y extends CharSequence & Runnable,
                     Z extends Object & Runnable> {

        // The names of the bounds of each type parameter of Gen.
        static Map<String, String[]> boundNames =
            Map.of("T", new String[] {"Object"},
                   "U", new String[] {"Object"},
                   "V", new String[] {"Number"},
                   "W", new String[] {"U"},
                   "X", new String[] {"Runnable"},
                   "Y", new String[] {"CharSequence", "Runnable"},
                   "Z", new String[] {"Object", "Runnable"});
    }
}
