/*
 * Copyright (c) 1998, 2015, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.hotspot.igv.data;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Wuerthinger
 */
public class Util {

    public static void assertGraphDocumentNotEquals(GraphDocument a, GraphDocument b) {
        try {
            assertGraphDocumentEquals(a, b);
        } catch(AssertionError e) {
            return;
        }

        fail("Graphs documents are equal!");
    }

    public static void assertGraphDocumentEquals(GraphDocument a, GraphDocument b) {

        if (a.getElements().size() != b.getElements().size()) {
            fail();
        }

        int z = 0;
        for (FolderElement e : b.getElements()) {

            if (e instanceof Group) {
                Group g = (Group) e;
                Group thisG = (Group) a.getElements().get(z);
                assertGroupEquals(thisG, g);
            z++;
            }
        }
    }

    public static void assertGroupNotEquals(Group a, Group b) {
        try {
            assertGroupEquals(a, b);
        } catch(AssertionError e) {
            return;
        }

        fail("Groups are equal!");
    }

    public static void assertGroupEquals(Group a, Group b) {

        if (a.getGraphsCount() != b.getGraphsCount()) {
            fail();
        }

        int z = 0;
        for (InputGraph graph : a.getGraphs()) {
            InputGraph otherGraph = b.getGraphs().get(z);
            assertGraphEquals(graph, otherGraph);
            z++;
        }

        if (a.getMethod() == null || b.getMethod() == null) {
            if (a.getMethod() != b.getMethod()) {
                fail();
            }
        } else {
            if (!a.getMethod().equals(b.getMethod())) {
                fail();
            }
        }
    }

    public static void assertGraphNotEquals(InputGraph a, InputGraph b) {
        try {
            assertGraphEquals(a, b);
        } catch(AssertionError e) {
            return;
        }

        fail("Graphs are equal!");
    }

    public static void assertGraphEquals(InputGraph a, InputGraph b) {

        if(!a.getNodesAsSet().equals(b.getNodesAsSet())) {
            fail();
        }

        if (!a.getEdges().equals(b.getEdges())) {
            fail();
        }

        if (a.getBlocks().equals(b.getBlocks())) {
            fail();
        }

        for (InputNode n : a.getNodes()) {
            assertEquals(a.getBlock(n), b.getBlock(n));
        }
    }
}
