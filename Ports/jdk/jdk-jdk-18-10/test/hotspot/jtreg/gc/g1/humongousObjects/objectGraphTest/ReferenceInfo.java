/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package gc.g1.humongousObjects.objectGraphTest;

import java.lang.ref.Reference;

/**
 * Immutable structure that holds the following information about graph's node
 * reference - weak/soft reference to graph's node
 * graphId and nodeId - graph's and node's ids - we need this for error handling
 * softlyReachable - is node effectively referenced by external soft reference. It could be when external
 * soft reference or when this node is reachable from node that externally referenced by soft reference
 * effectiveHumongous - if node behaves effectively humongous.  It could be when node is humongous
 * or when this node is reachable from humongous node.
 *
 * @param <T> - actual type of node
 */
public class ReferenceInfo<T> {
    public final Reference<T> reference;
    public final String graphId;
    public final String nodeId;
    public final boolean softlyReachable;
    public final boolean effectiveHumongous;

    public ReferenceInfo(Reference<T> reference, String graphId, String nodeId, boolean softlyReachable,
                         boolean effectiveHumongous) {
        this.reference = reference;
        this.graphId = graphId;
        this.nodeId = nodeId;
        this.softlyReachable = softlyReachable;
        this.effectiveHumongous = effectiveHumongous;
    }

    @Override
    public String toString() {
        return String.format("Node %s is effectively %shumongous and effectively %ssoft referenced\n"
                        + "\tReference type is %s and it points to %s", nodeId,
                (effectiveHumongous ? "" : "non-"), (softlyReachable ? "" : "non-"),
                reference.getClass().getSimpleName(), reference.get());
    }
}
