/*
 * Copyright (c) 2008, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.hotspot.igv.graph;

import java.util.List;

/**
 *
 * @author Thomas Wuerthinger
 */
public class OrSelector implements Selector {

    private Selector selector1;
    private Selector selector2;

    /** Creates a new instance of OrSelector */
    public OrSelector(Selector s1, Selector s2) {
        this.selector1 = s1;
        this.selector2 = s2;
    }

    @Override
    public List<Figure> selected(Diagram d) {

        List<Figure> l1 = selector1.selected(d);
        List<Figure> l2 = selector2.selected(d);

        for (Figure f : l2) {
            if (!l1.contains(f)) {
                l1.add(f);
            }
        }

        return l1;
    }
}
