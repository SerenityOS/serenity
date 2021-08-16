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
 * @bug 8038455
 * @summary Ensure that Symbols for members (methods and fields) are dropped across annotation
 *          processing rounds. ClassSymbols need to be kept.
 * @library /tools/javac/lib
 * @modules java.compiler
 *          jdk.compiler
 * @build JavacTestingAbstractProcessor MethodsDroppedBetweenRounds
 * @compile/process -processor MethodsDroppedBetweenRounds MethodsDroppedBetweenRounds.java
 */

import java.lang.ref.WeakReference;
import java.util.*;
import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.lang.model.util.ElementFilter;

public class MethodsDroppedBetweenRounds extends JavacTestingAbstractProcessor {
    private static TypeElement currentClassSymbol;
    private static WeakReference<ExecutableElement> keptMethod = null;
    public boolean process(Set<? extends TypeElement> annos,RoundEnvironment rEnv) {
        if (keptMethod != null) {
            //force GC:
            List<byte[]> hold = new ArrayList<>();
            try {
                while (true)
                    hold.add(new byte[1024 * 1024 * 1024]);
            } catch (OutOfMemoryError err) { }
            hold.clear();
            if (keptMethod.get() != null) {
                throw new IllegalStateException("Holds method across rounds.");
            }
        }

        TypeElement currentClass = elements.getTypeElement("MethodsDroppedBetweenRounds");

        if (currentClassSymbol != null && currentClassSymbol != currentClass) {
            throw new IllegalStateException("Different ClassSymbols across rounds");
        }

        ExecutableElement method = ElementFilter.methodsIn(currentClass.getEnclosedElements()).get(0);

        keptMethod = new WeakReference<>(method);

        return true;
    }
}
