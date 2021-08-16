/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8215407
 * @summary Verify broken EnclosingMethod attribute does not break ClassReader.
 * @library /tools/javac/lib
 * @modules java.compiler
 * @build JavacTestingAbstractProcessor
 * @compile BrokenEnclosingClass.java UnrelatedClass.jcod Enclosing$1.jcod
 * @compile -processor BrokenEnclosingClass BrokenEnclosingClass.java
 */

import java.util.Set;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;

public class BrokenEnclosingClass extends JavacTestingAbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (processingEnv.getElementUtils().getTypeElement("UnrelatedClass") == null) {
            throw new AssertionError("Cannot find UnrelatedClass.");
        }
        if (processingEnv.getElementUtils().getTypeElement("Enclosing$1") != null) {
            throw new AssertionError("Enclosing$1 was found.");
        }
        return false;
    }

}
