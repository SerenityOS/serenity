/*
 * Copyright (c) 2008, 2021, Oracle and/or its affiliates. All rights reserved.
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
 *
 */
package com.sun.hotspot.igv.data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 *
 * @author Thomas Wuerthinger
 */
public class InputNode extends Properties.Entity {

    private int id;
    private List<InputGraph> subgraphs;

    public static final Comparator<InputNode> COMPARATOR = new Comparator<InputNode>() {
        @Override
        public int compare(InputNode o1, InputNode o2) {
            return o1.getId() - o2.getId();
        }
    };

    public static Comparator<InputNode> getPropertyComparator(final String propertyName) {
        return new Comparator<InputNode>() {

            @Override
            public int compare(InputNode o1, InputNode o2) {

                int i1 = 0;
                try {
                    i1 = Integer.parseInt(o1.getProperties().get(propertyName));
                } catch(NumberFormatException e) {
                }

                int i2 = 0;
                try {
                    i2 = Integer.parseInt(o2.getProperties().get(propertyName));
                } catch(NumberFormatException e) {
                }

                return i1 - i2;
            }
        };
    }

    public InputNode(InputNode n) {
        super(n);
        setId(n.id);
    }

    public InputNode(int id) {
        setId(id);
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void addSubgraph(InputGraph graph) {
        if (subgraphs == null) {
            subgraphs = new ArrayList<>();
        }
        subgraphs.add(graph);
    }

    public List<InputGraph> getSubgraphs() {
        return subgraphs;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof InputNode)) {
            return false;
        }
        InputNode n = (InputNode) o;
        return n.id == id;
    }

    @Override
    public int hashCode() {
        return id * 13;
    }

    @Override
    public String toString() {
        return "Node " + id + " " + getProperties().toString();
    }
}
