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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class TestcaseData {
    /**
     * Temporary node description used during parsing
     */
    private static class InternalParsedNode {
        public String id;
        public final ArrayList<Integer> connectedTo = new ArrayList<>();
        public final ArrayList<Integer> connectedFrom = new ArrayList<>();
        public final List<ObjectGraph.ReferenceType> referencesTypes = new ArrayList<>();
        public boolean isHumongous;
    }

    /**
     * Immutable node description.
     * Contains:
     * Node id
     * Humongous flag
     * List of external references' types
     * List of nodes connected to
     */
    public static class FinalParsedNode {
        public final String id;
        public final boolean isHumongous;
        private final List<ObjectGraph.ReferenceType> referencesTypes;
        private final ArrayList<Integer> connectedTo;


        public FinalParsedNode(InternalParsedNode internalParsedNode) {
            referencesTypes = internalParsedNode.referencesTypes;
            connectedTo = internalParsedNode.connectedTo;
            id = internalParsedNode.id;
            isHumongous = internalParsedNode.isHumongous;
        }

        public List<ObjectGraph.ReferenceType> getReferencesTypes() {
            return Collections.unmodifiableList(referencesTypes);
        }

        public List<Integer> getConnectedTo() {
            return Collections.unmodifiableList(connectedTo);
        }
    }

    /**
     * @param testcaseDesc testcase in the following notation:
     *                     H - humongous node
     *                     S - non-humongous node
     *                     s - external soft reference
     *                     w - external weak reference
     *                     Hs->Sw - 1st node is humongous, externally soft referenced and strong references to non-humongous node 2 which is
     *                     externally weak referenced
     *                     H->1 - humongous node connects to the first node of chain
     * @return list of nodes description in FinalParsedNode structure
     */
    public static List<FinalParsedNode> parse(String testcaseDesc) {
        String[] nodes = testcaseDesc.split("-");
        List<InternalParsedNode> internalParsedNodeList = new ArrayList<>();

        for (int i = 0; i < nodes.length; ++i) {
            String node = nodes[i];
            InternalParsedNode nd;
            if (node.contains("1")) {
                nd = internalParsedNodeList.get(0);

            } else {
                nd = new InternalParsedNode();
                internalParsedNodeList.add(nd);
                nd.id = String.valueOf(i);
            }

            if (node.startsWith(">")) {
                nd.connectedFrom.add(i - 1);
            }
            if (node.endsWith("<")) {
                nd.connectedFrom.add(i + 1);
            }
            if (node.contains("w")) {
                nd.referencesTypes.add(ObjectGraph.ReferenceType.WEAK);
            }

            if (node.contains("s")) {
                nd.referencesTypes.add(ObjectGraph.ReferenceType.SOFT);
            }
            if (node.contains("H")) {
                nd.isHumongous = true;
            }

            if (node.contains("S")) {
                nd.isHumongous = false;
            }
        }

        // we have connectedFrom but we need to get connectedTo
        for (int i = 0; i < internalParsedNodeList.size(); ++i) {
            for (Integer reference : internalParsedNodeList.get(i).connectedFrom) {
                internalParsedNodeList.get(reference).connectedTo.add(i);
            }
        }

        List<FinalParsedNode> finalParsedNodes = internalParsedNodeList.stream().map(FinalParsedNode::new)
                .collect(Collectors.toList());

        return finalParsedNodes;
    }

    /**
     * @return List of pregenerated testing cases
     */
    public static List<String> getPregeneratedTestcases() {
        return Arrays.asList(
                "Hw",
                "Sw",
                "Sw->Hw",
                "Hw->Sw",
                "Sw<->Hw",
                "Sw<->Sw",
                "Hw->Sw->Sw",
                "Hw->Sw->Sw",
                "Sw->Hw->Sw",
                "Hw->Sw->Sw->1",
                "Sw->Hw->Sw->1",
                "Sw->Hw->Hw->1",
                "Sw<->Hw<->Hw->1",
                "Sw<->Hw<->Sw->1",
                "Sw->Hw<->Sw",
                "Hs",
                "Ss",
                "Ss->Hs",
                "Hs->Ss",
                "Ss<->Hs",
                "Ss<->Ss",
                "Hs->Ss->Ss",
                "Hs->Ss->Ss",
                "Ss->Hs->Ss",
                "Hs->Ss->Ss->1",
                "Ss->Hs->Ss->1",
                "Ss->Hs->Hs->1",
                "Ss<->Hs<->Hs->1",
                "Ss<->Hs<->Ss->1",
                "Ss->Hs<->Ss",
                "Ss->Hw",
                "Sw->Hs",
                "Hs->Sw",
                "Hw->Ss",
                "Ss<->Hw",
                "Sw<->Hs",
                "Ss<->Sw",
                "Sw<->Ss",
                "Hs->Sw->Sw",
                "Hw->Ss->Sw",
                "Hw->Sw->Ss",
                "Ss->Hw->Sw",
                "Sw->Hs->Sw",
                "Sw->Hw->Ss",
                "Hs->Sw->Sw->1",
                "Hw->Ss->Sw->1",
                "Hw->Sw->Ss->1",
                "Ss->Hw->Sw->1",
                "Ss->Hs->Sw->1",
                "Sw->Hw->Ss->1",
                "Ss->Hw->Hw->1",
                "Sw->Hs->Hw->1",
                "Sw->Hw->Hs->1",
                "Ss<->Hw<->Hw->1",
                "Sw<->Hs<->Hw->1",
                "Sw<->Hw<->Hs->1",
                "Ss<->Hw<->Sw->1",
                "Sw<->Hs<->Sw->1",
                "Sw<->Hw<->Ss->1",
                "Ss->Hw<->Sw",
                "Sw->Hs<->Sw",
                "Sw->Hw<->Ss"
        );
    }
}
