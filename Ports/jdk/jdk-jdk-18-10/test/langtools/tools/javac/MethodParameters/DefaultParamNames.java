/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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
 * @bug 8194268
 * @summary Incorrect parameter names for synthetic methods
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.code
 *          jdk.compiler/com.sun.tools.javac.tree
 *          jdk.compiler/com.sun.tools.javac.util
 * @compile DefaultParamNames.java
 * @run main DefaultParamNames
 */

import static java.util.stream.Collectors.joining;

import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type.MethodType;
import com.sun.tools.javac.util.Assert;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Names;
import java.util.Objects;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

public class DefaultParamNames {

    public static void main(String[] args) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        BasicJavacTask task = (BasicJavacTask) compiler.getTask(null, null, null, null, null, null);
        Context context = task.getContext();
        Names names = Names.instance(context);
        Symtab symtab = Symtab.instance(context);
        MethodType mt =
                new MethodType(
                        List.of(symtab.intType, symtab.stringType, symtab.objectType),
                        symtab.voidType,
                        List.nil(),
                        symtab.methodClass);
        MethodSymbol ms =
                new MethodSymbol(Flags.SYNTHETIC, names.fromString("test"), mt, symtab.methodClass);
        String paramNames =
                ms.params().stream().map(p -> p.getSimpleName().toString()).collect(joining(","));
        assertEquals("arg0,arg1,arg2", paramNames);
    }

    static void assertEquals(Object expected, Object actual) {
        Assert.check(
                Objects.equals(expected, actual),
                String.format("expected: %s, but was: %s", expected, actual));
    }
}
