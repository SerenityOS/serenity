/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.hotspot.igv.graal.filters;

import com.sun.hotspot.igv.data.Properties;
import com.sun.hotspot.igv.filter.AbstractFilter;
import com.sun.hotspot.igv.graph.Connection;
import com.sun.hotspot.igv.graph.Diagram;
import com.sun.hotspot.igv.graph.Figure;
import com.sun.hotspot.igv.graph.InputSlot;
import com.sun.hotspot.igv.graph.OutputSlot;
import java.util.HashSet;
import java.util.Set;

public class GraalCFGFilter extends AbstractFilter {

    @Override
    public String getName() {
        return "Graal CFG Filter";
    }

    @Override
    public void apply(Diagram d) {
        Set<Connection> connectionsToRemove = new HashSet<>();

        for (Figure f : d.getFigures()) {
            Properties p = f.getProperties();
            int predCount;
            String predCountString = p.get("predecessorCount");
            if (predCountString != null) {
                predCount = Integer.parseInt(predCountString);
            } else if (Boolean.parseBoolean(p.get("hasPredecessor"))) {
                predCount = 1;
            } else {
                predCount = 0;
            }
            for (InputSlot is : f.getInputSlots()) {
                if (is.getPosition() >= predCount && !"EndNode".equals(is.getProperties().get("class"))) {
                    for (Connection c : is.getConnections()) {
                        if (!"EndNode".equals(c.getOutputSlot().getFigure().getProperties().get("class"))) {
                            connectionsToRemove.add(c);
                        }
                    }
                }
            }
        }

        for (Connection c : connectionsToRemove) {
            c.remove();
        }

        Set<Figure> figuresToRemove = new HashSet<>();
        next: for (Figure f : d.getFigures()) {
            for (InputSlot is : f.getInputSlots()) {
                if (!is.getConnections().isEmpty()) {
                    continue next;
                }
            }
            for (OutputSlot os : f.getOutputSlots()) {
                if (!os.getConnections().isEmpty()) {
                    continue next;
                }
            }
            figuresToRemove.add(f);
        }
        d.removeAllFigures(figuresToRemove);
    }
}
