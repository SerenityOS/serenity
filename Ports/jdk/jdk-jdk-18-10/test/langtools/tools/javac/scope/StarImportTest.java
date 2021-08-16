/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Basher for star-import scopes
 * @modules jdk.compiler/com.sun.tools.javac.code
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.tree
 *          jdk.compiler/com.sun.tools.javac.util
 */

import java.util.*;
import java.util.List;

import com.sun.tools.javac.code.*;
import com.sun.tools.javac.code.Scope.ImportFilter;
import com.sun.tools.javac.code.Scope.StarImportScope;
import com.sun.tools.javac.code.Scope.WriteableScope;
import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.*;

import static com.sun.tools.javac.code.Kinds.Kind.*;

public class StarImportTest {
    public static void main(String... args) throws Exception {
        new StarImportTest().run(args);
    }

    void run(String... args) throws Exception {
        int count = 1;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("-seed") && (i + 1 < args.length))
                seed = Long.parseLong(args[++i]);
            else if(arg.equals("-tests") && (i + 1 < args.length))
                count = Integer.parseInt(args[++i]);
            else
                throw new Exception("unknown arg: " + arg);
        }

        rgen = new Random(seed);

        for (int i = 0; i < count; i++) {
            Test t = new Test();
            t.run();
        }

        if (errors > 0)
            throw new Exception(errors + " errors found");
    }

    /**
     * Select a random element from an array of choices.
     */
    <T> T random(T... choices) {
        return choices[rgen.nextInt(choices.length)];
    }

    /**
     * Write a message to stderr.
     */
    void log(String msg) {
        System.err.println(msg);
    }

    /**
     * Write a message to stderr, and dump a scope.
     */
    void log(String msg, Scope s) {
        System.err.print(msg);
        System.err.print(": ");
        String sep = "(";
        for (Symbol sym : s.getSymbols()) {
            System.err.print(sep + sym.name + ":" + sym);
            sep = ",";
        }
        System.err.println();
    }

    /**
     * Write an error message to stderr.
     */
    void error(String msg) {
        System.err.println("Error: " + msg);
        errors++;
    }

    Random rgen;
    long seed = 0;

    int errors;

    enum SetupKind { NAMES, PACKAGE, CLASS };
    static final int MAX_SETUP_COUNT = 50;
    static final int MAX_SETUP_NAME_COUNT = 20;
    static final int MAX_SETUP_PACKAGE_COUNT = 20;
    static final int MAX_SETUP_CLASS_COUNT = 20;

    /** Class to encapsulate a test run. */
    class Test {
        /** Run the test. */
        void run() throws Exception {
            log ("starting test");
            setup();
            createStarImportScope();
            test();
        }

        /**
         * Setup env by creating pseudo-random collection of names, packages and classes.
         */
        void setup() {
            log ("setup");
            context = new Context();
            JavacFileManager.preRegister(context); // required by ClassReader which is required by Symtab
            make = TreeMaker.instance(context);
            names = Names.instance(context);       // Name.Table impls tied to an instance of Names
            symtab = Symtab.instance(context);
            types = Types.instance(context);
            int setupCount = rgen.nextInt(MAX_SETUP_COUNT);
            for (int i = 0; i < setupCount; i++) {
                switch (random(SetupKind.values())) {
                    case NAMES:
                        setupNames();
                        break;
                    case PACKAGE:
                        setupPackage();
                        break;
                    case CLASS:
                        setupClass();
                        break;
                }
            }
        }

        /**
         * Set up a random number of names.
         */
        void setupNames() {
            int count = rgen.nextInt(MAX_SETUP_NAME_COUNT);
            log("setup: creating " + count + " new names");
            for (int i = 0; i < count; i++) {
                names.fromString("n" + (++nextNameSerial));
            }
        }

        /**
         * Set up a package containing a random number of member elements.
         */
        void setupPackage() {
            Name name = names.fromString("p" + (++nextPackageSerial));
            int count = rgen.nextInt(MAX_SETUP_PACKAGE_COUNT);
            log("setup: creating package " + name + " with " + count + " entries");
            PackageSymbol p = new PackageSymbol(name, symtab.rootPackage);
            p.members_field = WriteableScope.create(p);
            for (int i = 0; i < count; i++) {
                String outer = name + "c" + i;
                String suffix = random(null, "$Entry", "$Entry2");
                ClassSymbol c1 = createClass(names.fromString(outer), p);
//                log("setup: created " + c1);
                if (suffix != null) {
                    ClassSymbol c2 = createClass(names.fromString(outer + suffix), p);
//                    log("setup: created " + c2);
                }
            }
//            log("package " + p, p.members_field);
            packages.add(p);
            imports.add(p);
        }

        /**
         * Set up a class containing a random number of member elements.
         */
        void setupClass() {
            Name name = names.fromString("c" + (++nextClassSerial));
            int count = rgen.nextInt(MAX_SETUP_CLASS_COUNT);
            log("setup: creating class " + name + " with " + count + " entries");
            ClassSymbol c = createClass(name, symtab.unnamedModule.unnamedPackage);
//            log("setup: created " + c);
            for (int i = 0; i < count; i++) {
                ClassSymbol ic = createClass(names.fromString("Entry" + i), c);
//                log("setup: created " + ic);
            }
            classes.add(c);
            imports.add(c);
        }

        /**
         * Create a star-import scope and a model thereof, from the packages and
         * classes created by setupPackages and setupClasses.
         * @throws Exception for fatal errors, such as from reflection
         */
        void createStarImportScope() throws Exception {
            log ("createStarImportScope");
            PackageSymbol pkg = new PackageSymbol(names.fromString("pkg"), symtab.rootPackage);

            starImportScope = new StarImportScope(pkg);
            starImportModel = new Model();

            for (Symbol imp: imports) {
                Scope members = imp.members();
//                    log("importAll", members);
                starImportScope.importAll(types, members, new ImportFilter() {
                    @Override
                    public boolean accepts(Scope origin, Symbol t) {
                        return t.kind == TYP;
                    }
                }, make.Import(null, false), (i, cf) -> { throw new IllegalStateException(); });

                for (Symbol sym : members.getSymbols()) {
                    starImportModel.enter(sym);
                }
            }

//            log("star-import scope", starImportScope);
            starImportModel.check(starImportScope);
        }

        /**
         * The core of the test. In a random order, move nested classes from
         * the package in which they created to the class which should own them.
         */
        void test() {
            log ("test");
            List<ClassSymbol> nestedClasses = new LinkedList<ClassSymbol>();
            for (PackageSymbol p: packages) {
                for (Symbol sym : p.members_field.getSymbols()) {
                    if (sym.name.toString().contains("$"))
                        nestedClasses.add((ClassSymbol) sym);
                }
            }

            for (int i = nestedClasses.size(); i > 0; i--) {
                // select a random nested class to move from package to class
                ClassSymbol sym = nestedClasses.remove(rgen.nextInt(i));
                log("adjusting class " + sym);

                // remove from star import model
                starImportModel.remove(sym);

                String s = sym.name.toString();
                int dollar = s.indexOf("$");

                // owner should be a package
                assert (sym.owner.kind == PCK);

                // determine new owner
                Name outerName = names.fromString(s.substring(0, dollar));
//                log(sym + " owner: " + sym.owner, sym.owner.members());
                ClassSymbol outer = (ClassSymbol)sym.owner.members().findFirst(outerName);
//                log("outer: " + outerName + " " + outer);

                // remove from package
                sym.owner.members().remove(sym);

                // rename and insert into class
                sym.name = names.fromString(s.substring(dollar + 1));
                outer.members().enter(sym);
                sym.owner = outer;

                // verify
                starImportModel.check(starImportScope);
            }
        }

        ClassSymbol createClass(Name name, Symbol owner) {
            ClassSymbol sym = new ClassSymbol(0, name, owner);
            sym.members_field = WriteableScope.create(sym);
            if (owner != symtab.unnamedModule.unnamedPackage)
                owner.members().enter(sym);
            return sym;
        }

        Context context;
        Symtab symtab;
        TreeMaker make;
        Names names;
        Types types;
        int nextNameSerial;
        List<PackageSymbol> packages = new ArrayList<PackageSymbol>();
        int nextPackageSerial;
        List<ClassSymbol> classes = new ArrayList<ClassSymbol>();
        List<Symbol> imports = new ArrayList<Symbol>();
        int nextClassSerial;

        StarImportScope starImportScope;
        Model starImportModel;
    }

    class Model {
        private Map<Name, Set<Symbol>> map = new HashMap<Name, Set<Symbol>>();
        private Set<Symbol> bogus = new HashSet<Symbol>();

        void enter(Symbol sym) {
            Set<Symbol> syms = map.get(sym.name);
            if (syms == null)
                map.put(sym.name, syms = new LinkedHashSet<Symbol>());
            syms.add(sym);
        }

        void remove(Symbol sym) {
            Set<Symbol> syms = map.get(sym.name);
            if (syms == null)
                error("no entries for " + sym.name + " found in reference model");
            else {
                boolean ok = syms.remove(sym);
                if (ok) {
//                        log(sym.name + "(" + sym + ") removed from reference model");
                } else {
                    error(sym.name + " not found in reference model");
                }
                if (syms.isEmpty())
                    map.remove(sym.name);
            }
        }

        /**
         * Check the contents of a scope
         */
        void check(Scope scope) {
            // First, check all entries in scope are in map
            int bogusCount = 0;
            for (Symbol sym : scope.getSymbols()) {
                if (sym.owner != scope.getOrigin(sym).owner) {
                    if (bogus.contains(sym)) {
                        bogusCount++;
                    } else {
                        log("Warning: " + sym.name + ":" + sym + " appears to be bogus");
                        bogus.add(sym);
                    }
                } else {
                    Set<Symbol> syms = map.get(sym.name);
                    if (syms == null) {
                        error("check: no entries found for " + sym.name + ":" + sym + " in reference map");
                    } else  if (!syms.contains(sym)) {
                        error("check: symbol " + sym.name + ":" + sym + " not found in reference map");
                    }
                }
            }
            if (bogusCount > 0) {
                log("Warning: " + bogusCount + " other bogus entries previously reported");
            }

            // Second, check all entries in map are in scope
            for (Map.Entry<Name,Set<Symbol>> me: map.entrySet()) {
                Name name = me.getKey();
                if (scope.findFirst(name) == null) {
                    error("check: no entries found for " + name + " in scope");
                    continue;
                }
            nextSym:
                for (Symbol sym: me.getValue()) {
                    for (Symbol s : scope.getSymbolsByName(name)) {
                        if (sym == s)
                            continue nextSym;
                    }
                    error("check: symbol " + sym + " not found in scope");
                }
            }
        }
    }
}
