/*
 * Copyright (c) 2004, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package build.tools.charsetmapping;

import java.io.*;
import java.util.*;


/**
 * Reads a map in the form of a sequence of key/value-expression pairs from the
 * standard input, attempts to construct a hash map that fits within the given
 * table-size and chain-depth constraints, and, if successful, writes source
 * code to the standard output for a subclass of sun.util.PreHashedMap that
 * implements the map.
 *
 * @see sun.util.PreHashedMap
 *
 * @author Mark Reinhold
 */

public class Hasher {

    private PrintStream err = System.err;

    boolean verbose = false;
    List<String> keys = new ArrayList<>();      // Key strings
    List<String> values = new ArrayList<>();    // Value expressions
    String pkg = null;                          // Package prefix for generated class
    String cln = null;                          // Name of generated class
    String vtype = null;                        // Value type
    int maxBits = 11;                           // lg table size
    int maxDepth = 3;                           // Max chain depth
    boolean inner = false;                      // Generating an inner class?
    boolean empty = false;                      // Generating an empty table?

    Object[] ht;                                // Hash table itself
    int nb;                                     // Number of bits (lg table size)
    int md;                                     // Maximum chain depth
    int mask;                                   // Hash-code mask
    int shift;                                  // Hash-code shift size

    int hash(String w) {
        return (w.hashCode() >> shift) & mask;
    }

    // Build a hash table of size 2^nb, shifting the hash code by s bits
    //
    void build(int nb, int s) {

        this.nb = nb;
        this.shift = s;
        int n = 1 << nb;
        this.mask = n - 1;
        ht = new Object[n];
        int nw = keys.size();

        for (int i = 0; i < nw; i++) {
            String w = keys.get(i);
            String v = values.get(i);
            int h = hash(w);
            if (ht[h] == null)
                ht[h] = new Object[] { w, v };
            else
                ht[h] = new Object[] { w, v, ht[h] };
        }

        this.md = 0;
        for (int i = 0; i < n; i++) {
            int d = 1;
            for (Object[] a = (Object[])ht[i];
                 a != null && a.length > 2;
                 a = (Object[])a[2], d++);
            this.md = Math.max(md, d);
        }

    }

    Hasher build() {
        // Iterate through acceptable table sizes
        for (int nb = 2; nb < maxBits; nb++) {
            // Iterate through possible shift sizes
            for (int s = 0; s < (32 - nb); s++) {
                build(nb, s);
                if (verbose)
                    err.println("nb=" + nb + " s=" + s + " md=" + md);
                if (md <= maxDepth) {
                    // Success
                     if (verbose) {
                        if (cln != null)
                            err.print(cln + ": ");
                        err.println("Table size " + (1 << nb) + " (" + nb + " bits)"
                                    + ", shift " + shift
                                    + ", max chain depth " + md);
                    }
                    return this;
                }
            }
        }
        throw new RuntimeException("Cannot find a suitable size"
                                   + " within given constraints");
    }

    // Look for the given key in the hash table
    //
    String get(String k) {
        int h = hash(k);
        Object[] a = (Object[])ht[h];
        for (;;) {
            if (a[0].equals(k))
                return (String)a[1];
            if (a.length < 3)
                return null;
            a = (Object[])a[2];
        }
    }

    // Test that all input keys can be found in the table
    //
    Hasher test() {
        if (verbose)
            err.println();
        for (int i = 0, n = keys.size(); i < n; i++) {
            String w = keys.get(i);
            String v = get(w);
            if (verbose)
                err.println(hash(w) + "\t" + w);
            if (!v.equals(values.get(i)))
                throw new Error("Incorrect value: " + w + " --> "
                                + v + ", should be " + values.get(i));
        }
        return this;
    }

    String ind = "";                    // Indent prefix

    // Generate code for a single table entry
    //
    void genEntry(Object[] a, int depth, PrintStream out) {
        Object v = empty ? null : a[1];
        out.print("new Object[] { \"" + a[0] + "\", " + v);
        if (a.length < 3) {
            out.print(" }");
            return;
        }
        out.println(",");
        out.print(ind + "                     ");
        for (int i = 0; i < depth; i++)
            out.print("    ");
        genEntry((Object[])a[2], depth + 1, out);
        out.print(" }");
    }

    // Generate a PreHashedMap subclass from the computed hash table
    //
    Hasher generate(PrintStream out) throws IOException {
        if (cln == null)
            return this;

        if (inner)
            ind = "    ";

        if (!inner && pkg != null) {
            out.println();
            out.println("package " + pkg + ";");
            out.println();
        }

        if (inner) {
            out.println(ind + "private static final class " + cln);
        } else {
            out.println();
            out.println("public final class " + cln);
        }
        out.println(ind + "    extends sun.util.PreHashedMap<" + vtype +">");
        out.println(ind + "{");

        out.println();
        out.println(ind + "    private static final int ROWS = "
                    + ht.length + ";");
        out.println(ind + "    private static final int SIZE = "
                    + keys.size() + ";");
        out.println(ind + "    private static final int SHIFT = "
                    + shift + ";");
        out.println(ind + "    private static final int MASK = 0x"
                    + Integer.toHexString(mask) + ";");
        out.println();

        out.println(ind + "    " + (inner ? "private " : "public ")
                    + cln + "() {");
        out.println(ind + "        super(ROWS, SIZE, SHIFT, MASK);");
        out.println(ind + "    }");
        out.println();

        out.println(ind + "    protected void init(Object[] ht) {");
        for (int i = 0; i < ht.length; i++) {
            if (ht[i] == null)
                continue;
            Object[] a = (Object[])ht[i];
            out.print(ind + "        ht[" + i + "] = ");
            genEntry(a, 0, out);
            out.println(";");
        }
        out.println(ind + "    }");
        out.println();

        out.println(ind + "}");
        if (inner)
            out.println();

        return this;
    }

    private Hasher(List<String> keys, List<String> values,
                   String pkg, String cln, String vtype,
                   int maxBits, int maxDepth,
                   boolean inner, boolean empty,
                   boolean verbose) {
        this.keys = keys;
        this.values = values;
        this.pkg = pkg;
        this.cln = cln;
        this.vtype = vtype;
        this.maxBits = maxBits;
        this.maxDepth = maxDepth;
        this.inner = inner;
        this.empty = empty;
        this.verbose = verbose;
    }

    public static void genClass(PrintStream out,
                                List<String> keys, List<String> values,
                                String pkg, String cln, String vtype,
                                int maxBits, int maxDepth,
                                boolean inner, boolean empty, boolean verbose)
        throws IOException {
        new Hasher(keys, values, pkg, cln, vtype,
                   maxBits, maxDepth, inner, empty, verbose)
            .build()
            .test()
            .generate(out);
    }
}
