/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

package org.openjdk.tests.shapegen;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Robert Field
 */
public class TTShape {

    private final TTNode root;
    private final TTNode[] nodes;

    TTShape(TTNode root) {
        this.root = root;
        Set<TTNode> subs = new HashSet<>();
        root.collectAllSubtypes(subs);
        nodes = subs.toArray(new TTNode[subs.size()]);
    }

    private List<ClassCase> toCases(boolean includeClasses) {
        List<ClassCase> ccs = new ArrayList<>();
        root.start(includeClasses);
        int i;
        outer:
        while (true) {
            if (root.isValid()) {
                ClassCase cc = root.genCase();
                //System.out.println(cc);
                ccs.add(cc);
            }

            i = 0;
            do {
                if (i >= nodes.length) {
                    break outer;
                }
            } while(!nodes[i++].next());
        }
        return ccs;
    }

   public static List<ClassCase> allCases(boolean includeClasses) {
        List<ClassCase> ccs = new ArrayList<>();
        for (TTShape shape : SHAPES) {
            ccs.addAll(shape.toCases(includeClasses));
        }
        return ccs;
    }

    public static TTShape parse(String s) {
        return new TTShape(new TTParser(s).parse());
    }

    public static final TTShape[] SHAPES = new TTShape[] {
        parse("a"),
        parse("a(b)"),
        parse("A(bb)"),
        parse("A(B(d)c(d))"),
        parse("A(b(c))"),
        parse("A(B(cd)d)"),
        parse("A(B(c)c)"),
        parse("A(B(Ce)d(e))"),
        parse("A(B(C)d(e))"),
        parse("A(Bc(d))"),
        parse("A(B(d)dc)"),
        parse("A(B(dc)dc)"),
        parse("A(B(c(d))d)"),
        parse("A(B(C(d))d)"),
        parse("A(B(C(e)d(e))e)"),
        parse("A(B(c(d))c)"),
        parse("A(B(dc(d))c)"),
        parse("A(B(C(d))d)"),
    };

}
