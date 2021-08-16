/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8159970
 * @summary javac, JLS8 18.2.4 is not completely implemented by the compiler
 * @library /tools/lib/types
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.code
 *          jdk.compiler/com.sun.tools.javac.comp
 *          jdk.compiler/com.sun.tools.javac.tree
 *          jdk.compiler/com.sun.tools.javac.util
 *          jdk.compiler/com.sun.tools.javac.file
 * @build TypeHarness
 * @run main TypeEqualityInInferenceTest
 */

import java.util.ArrayList;
import java.util.List;

import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.UndetVar;
import com.sun.tools.javac.code.Type.UndetVar.InferenceBound;
import com.sun.tools.javac.util.Assert;

public class TypeEqualityInInferenceTest extends TypeHarness {
    StrToTypeFactory strToTypeFactory;

    public static void main(String... args) throws Exception {
        new TypeEqualityInInferenceTest().runAll();
    }

    void runAll() {
        List<String> imports = new ArrayList<>();
        imports.add("java.util.*");
        List<String> typeVars = new ArrayList<>();
        typeVars.add("T");
        strToTypeFactory = new StrToTypeFactory(null, imports, typeVars);

        runTest("List<? extends T>", "List<? extends String>", predef.stringType);
        runTest("List<? extends T>", "List<?>", predef.objectType);
        runTest("List<? super T>", "List<? super String>", predef.stringType);
    }

    void runTest(String freeTypeStr, String typeStr, Type equalityBoundType) {
        Type freeType = strToTypeFactory.getType(freeTypeStr);
        Type aType = strToTypeFactory.getType(typeStr);

        withInferenceContext(strToTypeFactory.getTypeVars(), inferenceContext -> {
            assertSameType(inferenceContext.asUndetVar(freeType), aType);
            UndetVar undetVarForT = (UndetVar)inferenceContext.undetVars().head;
            checkEqualityBound(undetVarForT, equalityBoundType);
        });

        withInferenceContext(strToTypeFactory.getTypeVars(), inferenceContext -> {
            assertSameType(aType, inferenceContext.asUndetVar(freeType));
            UndetVar undetVarForT = (UndetVar)inferenceContext.undetVars().head;
            checkEqualityBound(undetVarForT, equalityBoundType);
        });
    }

    void checkEqualityBound(UndetVar uv, Type boundType) {
        com.sun.tools.javac.util.List<Type> equalBounds = uv.getBounds(InferenceBound.EQ);
        Assert.check(!equalBounds.isEmpty() && equalBounds.length() == 1,
                "undetVar must have only one equality bound");
        Type bound = equalBounds.head;
        Assert.check(bound == boundType, "equal bound must be of type " + boundType);
    }
}
