/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7017664 7036906
 * @summary Basher for CompoundScopes
 * @modules jdk.compiler/com.sun.tools.javac.code
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.util
 */

import java.util.Random;
import java.util.Map;
import java.util.HashMap;
import java.util.function.Predicate;
import com.sun.tools.javac.util.*;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.code.Scope.*;
import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.file.JavacFileManager;

public class CompoundScopeTest {
    public static void main(String... args) throws Exception {
        new CompoundScopeTest().run(args);
    }

    static final int MAX_SYMBOLS_COUNT = 20;
    static final int PASSES = 10;

    void run(String... args) throws Exception {
        int count = PASSES;

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
     * Write a message to stderr.
     */
    void log(String msg) {
        System.err.println(msg);
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

    /** Class to encapsulate a test run. */
    class Test {

        List<Symbol> elems = List.nil();
        Map<Name, List<Symbol>> shadowedMap = new HashMap<Name, List<Symbol>>();

        /** Run the test. */
        void run() throws Exception {
            log ("starting test");
            setup();
            Scope[] scopes = { createScope(rgen.nextInt(MAX_SYMBOLS_COUNT)),
                               createScope(rgen.nextInt(MAX_SYMBOLS_COUNT)),
                               createScope(rgen.nextInt(MAX_SYMBOLS_COUNT)) };
            boolean[][] scopeNesting = { {false, true, false, true},
                                   {false, true, true, true},
                                   {false, false, true, true} };
            /**
             * We want to generate (and check) the following compound scopes:
             * C1 = C(S1, S2, S3)
             * C2 = C((S1, S2), S3)
             * C3 = C(S1, (S2, S3))
             * C3 = C(C(S1, S2, S3))
             */
            for (int i = 0 ; i < 4 ; i ++) {
                CompoundScope root = new CompoundScope(symtab.noSymbol);
                CompoundScope sub = new CompoundScope(symtab.noSymbol);
                boolean subAdded = false;
                for (int sc = 0 ; sc < 3 ; sc ++) {
                    if (scopeNesting[sc][i]) {
                        sub.prependSubScope(scopes[sc]);
                        if (!subAdded) {
                            root.prependSubScope(sub);
                            subAdded = true;
                        }
                    } else {
                        root.prependSubScope(scopes[sc]);
                    }
                }
                log("testing scope: " + root);
                checkElems(root, null);
                checkElems(root, new OddFilter());
                checkShadowed(root, null);
                checkShadowed(root, new OddFilter());
            }
        }

        class OddFilter implements Predicate<Symbol> {
            @Override
            public boolean test(Symbol s) {
                Name numPart = s.name.subName(1, s.name.length());
                return Integer.parseInt(numPart.toString()) % 2 != 0;
            }
        }

        /**
         * Create a scope containing a given number of synthetic symbols
         */
        Scope createScope(int nelems) {
            WriteableScope s = WriteableScope.create(symtab.noSymbol);
            for (int i = 0 ; i < nelems ; i++) {
                Symbol sym = new TypeVariableSymbol(0, names.fromString("s" + i), null, null);
                s.enter(sym);
                elems = elems.prepend(sym);
                List<Symbol> shadowed = shadowedMap.get(sym.name);
                if (shadowed == null) {
                    shadowed = List.nil();
                }
                shadowedMap.put(sym.name, shadowed.prepend(sym));
            }
            return s;
        }

        /**
         * Setup compiler context
         */
        void setup() {
            log ("setup");
            context = new Context();
            JavacFileManager.preRegister(context); // required by ClassReader which is required by Symtab
            names = Names.instance(context);       // Name.Table impls tied to an instance of Names
            symtab = Symtab.instance(context);
        }

        /**
         * Check that CompoundScope.getElements() correctly visits all symbols
         * in all subscopes (in the correct order)
         */
        void checkElems(CompoundScope cs, Predicate<Symbol> sf) {
            int count = 0;
            ListBuffer<Symbol> found = new ListBuffer<>();
            List<Symbol> allSymbols = sf == null ?
                    elems :
                    filter(elems, sf);
            int expectedCount = allSymbols.length();
            for (Symbol s : sf == null ? cs.getSymbols() : cs.getSymbols(sf)) {
                checkSameSymbols(s, allSymbols.head);
                allSymbols = allSymbols.tail;
                found.append(s);
                count++;
            }
            if (count != expectedCount) {
                error("CompoundScope.getElements() did not returned enough symbols");
            }
        }

        /**
         * Check that CompoundScope.getElements() correctly visits all symbols
         * with a given name in all subscopes (in the correct order)
         */
        void checkShadowed(CompoundScope cs, Predicate<Symbol> sf) {
            for (Map.Entry<Name, List<Symbol>> shadowedEntry : shadowedMap.entrySet()) {
                int count = 0;
                List<Symbol> shadowed = sf == null ?
                    shadowedEntry.getValue() :
                    filter(shadowedEntry.getValue(), sf);
                int expectedCount = shadowed.length();
                Name name = shadowedEntry.getKey();
                for (Symbol s : sf == null ? cs.getSymbolsByName(name) : cs.getSymbolsByName(name, sf)) {
                    checkSameSymbols(s, shadowed.head);
                    shadowed = shadowed.tail;
                    count++;
                }
                if (count != expectedCount) {
                    error("CompoundScope.lookup() did not returned enough symbols for name " + name);
                }
            }
        }

        List<Symbol> filter(List<Symbol> elems, Predicate<Symbol> sf) {
            ListBuffer<Symbol> res = new ListBuffer<>();
            for (Symbol s : elems) {
                if (sf.test(s)) {
                    res.append(s);
                }
            }
            return res.toList();
        }

        void checkSameSymbols(Symbol found, Symbol req) {
            if (found != req) {
                error("Symbol mismatch - found    : " + found + ":" + found.hashCode() + "\n" +
                      "                  required : " + req + ":" + req.hashCode());
            }
        }

        Context context;
        Symtab symtab;
        Names names;
    }
}
