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
 * @bug 8163989 8208371
 * @summary Test basic workings of naming methods on ModuleElement
 * @library /tools/javac/lib
 * @modules java.compiler
 *          jdk.compiler
 * @build   JavacTestingAbstractProcessor TestModuleElementNames
 * @compile -processor TestModuleElementNames -proc:only TestModuleElementNames.java
 */

import java.util.Set;
import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.lang.model.util.*;

/**
 * Test basic workings of names of ModuleElement.
 */
public class TestModuleElementNames extends JavacTestingAbstractProcessor {
    public boolean process(Set<? extends TypeElement> annotations,
                           RoundEnvironment roundEnv) {
        if (!roundEnv.processingOver()) {
            checkNames(eltUtils.getModuleElement(""),          "",          "",          true);
            checkNames(eltUtils.getModuleElement("java.base"), "base",      "java.base", false);
        }
        return true;
    }

    private void checkNames(ModuleElement mod, String expectedSimple, String expectedQual, boolean expectedUnnamed) {
        boolean unnamed = mod.isUnnamed();
        String simpleName    = mod.getSimpleName().toString();
        String qualifiedName = mod.getQualifiedName().toString();

        if (unnamed != expectedUnnamed) {
            throw new RuntimeException("Unnamed mismatch on " + qualifiedName);
        }

        if (!simpleName.equals(expectedSimple) ||
            !qualifiedName.equals(expectedQual)) {
            throw new RuntimeException("Unexpected name,\tqualitifed ``" + qualifiedName +
                                       "''\tsimmple ``" + simpleName + "''");
        }
    }
}
