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

/**
 * @test
 * @bug 8144733
 * @summary Verify that Scope.remove removes the Symbol also from already running iterations.
 * @modules jdk.compiler/com.sun.tools.javac.code
 *          jdk.compiler/com.sun.tools.javac.util
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import com.sun.tools.javac.code.Scope;
import com.sun.tools.javac.code.Scope.WriteableScope;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.PackageSymbol;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;

public class IterateAndRemove {
    public static void main(String... args) {
        new IterateAndRemove().run();
    }

    void run() {
        Context ctx = new Context();
        Names names = Names.instance(ctx);
        Symbol root = new PackageSymbol(names.empty, null);
        Name one = names.fromString("1");
        PackageSymbol sym1 = new PackageSymbol(one, new PackageSymbol(names.fromString("a"), root));
        PackageSymbol sym2 = new PackageSymbol(one, new PackageSymbol(names.fromString("b"), root));
        PackageSymbol sym3 = new PackageSymbol(one, new PackageSymbol(names.fromString("c"), root));
        List<Symbol> symbols = Arrays.asList(sym1, sym2, sym3);

        List<Function<Scope, Iterable<Symbol>>> getters = Arrays.asList(
                scope -> scope.getSymbols(),
                scope -> scope.getSymbolsByName(one)
        );
        for (Function<Scope, Iterable<Symbol>> scope2Content : getters) {
            for (int removeAt : new int[] {0, 1, 2, 3}) {
                for (Symbol removeWhat : new Symbol[] {sym1, sym2, sym3}) {
                    WriteableScope s = WriteableScope.create(root);

                    symbols.forEach(s :: enter);

                    Iterator<Symbol> it = scope2Content.apply(s).iterator();
                    List<PackageSymbol> actual = new ArrayList<>();
                    int count = 0;

                    while (true) {
                        if (count++ == removeAt)
                            s.remove(removeWhat);
                        if (!it.hasNext())
                            break;
                        actual.add((PackageSymbol) it.next());
                    }

                    List<Symbol> copy = new ArrayList<>(symbols);

                    Collections.reverse(copy);

                    count = 0;

                    while (true) {
                        if (count == removeAt && copy.indexOf(removeWhat) >= count)
                            copy.remove(removeWhat);
                        count++;
                        if (count >= copy.size())
                            break;
                    }

                    if (!copy.equals(actual)) {
                        throw new AssertionError("differs: actual: " + actual + "; expected: " + copy);
                    }
                }
            }
        }
    }
}
