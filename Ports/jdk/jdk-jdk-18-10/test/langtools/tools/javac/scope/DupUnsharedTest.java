/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @summary WriteableScope.dupUnshared not working properly for shared Scopes.
 * @modules jdk.compiler/com.sun.tools.javac.code:+open
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.util
 */

import com.sun.tools.javac.util.*;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.code.Scope.*;
import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.file.JavacFileManager;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Set;

public class DupUnsharedTest {
    public static void main(String... args) throws Exception {
        new DupUnsharedTest().run();
    }

    Context context;
    Names names;
    Symtab symtab;
    Name a;
    Name b;
    int errors;

    public DupUnsharedTest() {
        context = new Context();
        JavacFileManager.preRegister(context); // required by ClassReader which is required by Symtab
        names = Names.instance(context);
        symtab = Symtab.instance(context);
        a = names.fromString("a");
        b = names.fromString("b");
    }

    void run() throws Exception {
        runScopeContentTest();
        runClashTest();

        if (errors > 0)
            throw new AssertionError("Errors detected (" + errors + ").");
    }

    void runScopeContentTest() throws Exception {
        Set<Symbol> expected = Collections.newSetFromMap(new IdentityHashMap<>() {});
        Set<Symbol> notExpected = Collections.newSetFromMap(new IdentityHashMap<>());
        WriteableScope s1 = WriteableScope.create(symtab.unnamedModule.unnamedPackage);
        ClassSymbol acceptSym = symtab.arrayClass;
        s1.enter(acceptSym);
        expected.add(acceptSym);
        WriteableScope s2 = s1.dup();
        fillScope(s2, notExpected, a);
        WriteableScope s3 = s2.dup();
        fillScope(s3, notExpected, b);
        WriteableScope s4 = s1.dupUnshared();
        assertEquals(toSet(s4.getSymbols()), expected);
        assertEquals(toSet(s4.getSymbolsByName(a)), Collections.emptySet());
        assertEquals(toSet(s4.getSymbolsByName(b)), Collections.emptySet());
        assertEquals(toSet(s4.getSymbolsByName(acceptSym.name)), expected);
        for (Symbol sym : notExpected) {
            try {
                s4.remove(sym);
            } catch (Exception ex) {
                System.err.println("s4.remove(" + sym + "); crashes with exception:");
                ex.printStackTrace();
                errors++;
            }
        }
    }

    void fillScope(WriteableScope scope, Set<Symbol> notExpected, Name name) {
        VarSymbol var1 = new VarSymbol(0, name, Type.noType, symtab.arrayClass);
        VarSymbol var2 = new VarSymbol(0, name, Type.noType, symtab.autoCloseableClose.owner);
        scope.enter(var1);
        scope.enter(var2);
        scope.remove(var1);
        notExpected.add(var1);
        notExpected.add(var2);
    }

    Set<Symbol> toSet(Iterable<Symbol> it) {
        Set<Symbol> result = Collections.newSetFromMap(new IdentityHashMap<>() {});

        for (Symbol sym : it) {
            result.add(sym);
        }

        return result;
    }

    void assertEquals(Set<Symbol> set1, Set<Symbol> set2) {
        if (!Objects.equals(set1, set2)) {
            System.err.println("Sets are not equals: s1=" + set1 + "; s2=" + set2);
            errors++;
        }
    }

    /**
     * This tests tests the following situation.
     * - consider empty Scope S1
     * - a Symbol with name 'A' is added into S1
     * - S1 is dupped into S2
     * - a Symbol with name 'B', clashing with 'A', is added into S2
     * - so the table now looks like: [..., A, ..., B, ...]
     * - S2 is doubled. As a consequence, the table is re-hashed, and looks like:
     *   [..., B, ..., A, ...] (note that re-hashing goes from the end, hence the original order).
     * - B has been chosen so that it clashes in the doubled scope as well. So when looking up 'A',
     *   B is found (and rejected) first, and only then the A's bucket is tested.
     * - S2 is dupUshared - the resulting table needs to look like: [..., /sentinel/, ..., A, ...], not
     *   [..., null, ..., A, ...], as in the latter case lookups would see 'null' while looking for
     *   'A' and would stop the search prematurely.
     */
    void runClashTest() throws Exception {
        WriteableScope emptyScope = WriteableScope.create(symtab.unnamedModule.unnamedPackage);
        Field tableField = emptyScope.getClass().getDeclaredField("table");
        tableField.setAccessible(true);
        Method dble = emptyScope.getClass().getDeclaredMethod("dble");
        dble.setAccessible(true);
        Method getIndex = emptyScope.getClass().getDeclaredMethod("getIndex", Name.class);
        getIndex.setAccessible(true);

        int tries = 0;

        //find a name that will be in the first bucket in table (so that a conflicting name
        //will be in placed in a bucket after this one).
        Name first = names.fromString("a");
        while ((Integer) getIndex.invoke(emptyScope, first) != 0) {
            if (tries++ > MAX_TRIES) {
                System.err.println("could not find a name that would be placed in the first bucket");
                errors++;
                return ;
            }
            first = names.fromString("a" + first.toString());
        }

        System.out.println("first name: " + first);

        //now, find another name, that will clash with the first one both in the empty and a doubled scope:
        Scope doubledEmptyScope = WriteableScope.create(symtab.unnamedModule.unnamedPackage);
        dble.invoke(doubledEmptyScope);
        Integer firstNameTestScopeIndex = (Integer) getIndex.invoke(emptyScope, first);
        Integer firstNameDoubleScopeIndex = (Integer) getIndex.invoke(doubledEmptyScope, first);
        Name other = names.fromString("b");
        while (!Objects.equals(firstNameTestScopeIndex, getIndex.invoke(emptyScope, other)) ||
               !Objects.equals(firstNameDoubleScopeIndex, getIndex.invoke(doubledEmptyScope, other))) {
            if (tries++ > MAX_TRIES) {
                System.err.println("could not find a name that would properly clash with the first chosen name");
                errors++;
                return ;
            }
            other = names.fromString("b" + other);
        }

        System.out.println("other name: " + other);

        Symbol firstSymbol = new VarSymbol(0, first, Type.noType, null);
        Symbol otherSymbol = new VarSymbol(0, other, Type.noType, null);

        //test the situation described above:
        WriteableScope testScope1 = WriteableScope.create(symtab.unnamedModule.unnamedPackage);
        testScope1.enter(firstSymbol);

        WriteableScope dupped1 = testScope1.dup();

        dupped1.enter(otherSymbol);
        dble.invoke(dupped1);

        if (testScope1.dupUnshared().findFirst(first) != firstSymbol) {
            System.err.println("cannot find the Symbol in the dupUnshared scope (1)");
            errors++;
        }

        //also check a situation where the clashing Symbol is removed from the dupped scope:
        WriteableScope testScope2 = WriteableScope.create(symtab.unnamedModule.unnamedPackage);
        testScope2.enter(firstSymbol);

        WriteableScope dupped2 = testScope2.dup();

        dupped2.enter(otherSymbol);
        dble.invoke(dupped2);
        dupped2.remove(otherSymbol);

        if (testScope2.dupUnshared().findFirst(first) != firstSymbol) {
            System.err.println("cannot find the Symbol in the dupUnshared scope (2)");
            errors++;
        }
    }

    int MAX_TRIES = 100; // max tries to find a hash clash before giving up.

}
