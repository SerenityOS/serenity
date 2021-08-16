/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7004029 8131915
 * @summary Ensure Scope impl can cope with hash collisions
 * @library /tools/javac/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.code:+open
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.tree
 *          jdk.compiler/com.sun.tools.javac.util
 * @build DPrinter HashCollisionTest
 * @run main HashCollisionTest
 */

import java.lang.reflect.*;
import java.io.*;
import java.util.function.BiConsumer;

import com.sun.source.util.Trees;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.util.*;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.code.Scope.*;
import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.tree.JCTree.JCImport;
import com.sun.tools.javac.tree.TreeMaker;

import static com.sun.tools.javac.code.Kinds.Kind.*;

public class HashCollisionTest {
    public static void main(String... args) throws Exception {
        new HashCollisionTest().run();
    }

    void run() throws Exception {
        // set up basic environment for test
        Context context = new Context();
        JavacFileManager.preRegister(context); // required by ClassReader which is required by Symtab
        make = TreeMaker.instance(context);
        names = Names.instance(context);       // Name.Table impls tied to an instance of Names
        symtab = Symtab.instance(context);
        trees = JavacTrees.instance(context);
        types = Types.instance(context);

        // determine hashMask for an empty scope
        Scope emptyScope = WriteableScope.create(symtab.unnamedModule.unnamedPackage); // any owner will do
        Field field = emptyScope.getClass().getDeclaredField("hashMask");
        field.setAccessible(true);
        scopeHashMask = field.getInt(emptyScope);
        log("scopeHashMask: " + scopeHashMask);

        // 1. determine the Name.hashCode of "Entry", and therefore the index of
        // Entry in an empty scope.  i.e. name.hashCode() & Scope.hashMask
        Name entry = names.fromString("Entry");

        // 2. create names of the form *$Entry until we find a name with a
        // hashcode which yields the same index as Entry in an empty scope.
        // Since Name.hashCode is a function of position (and not content) it
        // should work to create successively longer names until one with the
        // desired characteristics is found.
        Name outerName;
        Name innerName;
        StringBuilder sb = new StringBuilder("C");
        int i = 0;
        do {
            sb.append(Integer.toString(i % 10));
            innerName = names.fromString(sb + "$Entry");
        } while (!clash(entry, innerName) && (++i) < MAX_TRIES);

        if (clash(entry, innerName)) {
            log("Detected expected hash collision for " + entry + " and " + innerName
                    + " after " + i + " tries");
        } else {
            throw new Exception("No potential collision found after " + i + " tries");
        }

        outerName = names.fromString(sb.toString());

        /*
         * Now we can set up the scenario.
         */

        // 3. Create a nested class named Entry
        ClassSymbol cc = createClass(names.fromString("C"), symtab.unnamedModule.unnamedPackage);
        ClassSymbol ce = createClass(entry, cc);

        // 4. Create a package containing a nested class using the name from 2
        PackageSymbol p = new PackageSymbol(names.fromString("p"), symtab.rootPackage);
        p.members_field = WriteableScope.create(p);
        ClassSymbol inner = createClass(innerName, p);
        // we'll need this later when we "rename" cn
        ClassSymbol outer = createClass(outerName, p);

        // 5. Create a star-import scope
        log ("createStarImportScope");

        PackageSymbol pkg = new PackageSymbol(names.fromString("pkg"), symtab.rootPackage);
        StarImportScope starImportScope = new StarImportScope(pkg);

        dump("initial", starImportScope);

        // 6. Insert the contents of the package from 4.
        Scope fromScope = p.members();
        ImportFilter typeFilter = new ImportFilter() {
            @Override
            public boolean accepts(Scope origin, Symbol sym) {
                return sym.kind == TYP;
            }
        };
        BiConsumer<JCImport, CompletionFailure> noCompletionFailure =
                (imp, cf) -> { throw new IllegalStateException(); };
        starImportScope.importAll(types, fromScope, typeFilter, make.Import(null, false), noCompletionFailure);

        dump("imported p", starImportScope);

        // 7. Insert the class from 3.
        starImportScope.importAll(types, cc.members_field, typeFilter, make.Import(null, false), noCompletionFailure);
        dump("imported ce", starImportScope);

        /*
         * Set the trap.
         */

        // 8. Rename the nested class to Entry. so that there is a bogus entry in the star-import scope
        p.members_field.remove(inner);
        inner.name = entry;
        inner.owner = outer;
        outer.members_field.enter(inner);

        // 9. Lookup Entry
        Symbol found = starImportScope.findFirst(entry);
        if (found != ce)
            throw new Exception("correct symbol not found: " + entry + "; found=" + found);

        dump("final", starImportScope);
    }

    /*
     * Check for a (probable) hash collision in an empty scope.
     */
    boolean clash(Name n1, Name n2) {
        log(n1 + " hc:" + n1.hashCode() + " v:" + (n1.hashCode() & scopeHashMask) + ", " +
                n2 + " hc:" + n2.hashCode() + " v:" + (n2.hashCode() & scopeHashMask));
        return (n1.hashCode() & scopeHashMask) == (n2.hashCode() & scopeHashMask);
    }

    /**
     * Create a class symbol, init the members scope, and add it to owner's scope.
     */
    ClassSymbol createClass(Name name, Symbol owner) {
        ClassSymbol sym = new ClassSymbol(0, name, owner);
        sym.members_field = WriteableScope.create(sym);
        if (owner != symtab.unnamedModule.unnamedPackage)
            owner.members().enter(sym);
        return sym;
    }

    /**
     * Dump the contents of a scope to System.err.
     */
    void dump(String label, Scope s) throws Exception {
        PrintWriter pw = new PrintWriter(System.err);
        new DPrinter(pw, trees).printScope(label, s);
        pw.flush();
    }

    Object readField(Object scope, String fieldName) throws Exception {
        Field field = scope.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);

        return field.get(scope);
    }

    /**
     * Write a message to stderr.
     */
    void log(String msg) {
        System.err.println(msg);
    }

    int MAX_TRIES = 100; // max tries to find a hash clash before giving up.
    int scopeHashMask;

    TreeMaker make;
    Names names;
    Symtab symtab;
    Trees trees;
    Types types;
}
