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
package com.sun.hotspot.igv.bytecodes;

import com.sun.hotspot.igv.data.InputBytecode;
import com.sun.hotspot.igv.data.InputGraph;
import com.sun.hotspot.igv.data.InputMethod;
import java.awt.Image;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.ImageUtilities;

/**
 *
 * @author Thomas Wuerthinger
 */
public class MethodNode extends AbstractNode {

    private static class MethodNodeChildren extends Children.Keys<InputBytecode> {

        private InputMethod method;
        private InputGraph graph;
        private String bciString;

        public MethodNodeChildren(InputMethod method, InputGraph graph, String bciString) {
            this.method = method;
            this.bciString = bciString;
            this.graph = graph;
        }

        @Override
        protected Node[] createNodes(InputBytecode bc) {
            if (bc.getInlined() == null) {
                return new Node[]{new BytecodeNode(bc, graph, bciString)};
            } else {
                return new Node[]{new BytecodeNode(bc, graph, bciString), new MethodNode(bc.getInlined(), graph, bc.getBci() + " " + bciString)};
            }
        }

        @Override
        public void addNotify() {
            if (method != null) {
                setKeys(method.getBytecodes());
            }
        }

        public void setMethod(InputMethod method, InputGraph graph) {
            this.method = method;
            this.graph = graph;
            addNotify();
        }
    }

    /** Creates a new instance of MethodNode */
    public MethodNode(InputMethod method, InputGraph graph, String bciString) {
        super((method != null && method.getBytecodes().size() == 0) ? Children.LEAF : new MethodNodeChildren(method, graph, bciString));
        if (method != null) {
            this.setDisplayName(method.getName());
        }
    }

    @Override
    public Image getIcon(int i) {
        return ImageUtilities.loadImage("com/sun/hotspot/igv/bytecodes/images/method.png");
    }

    @Override
    public Image getOpenedIcon(int i) {
        return getIcon(i);
    }

    public void update(InputGraph graph, InputMethod method) {
        ((MethodNodeChildren) this.getChildren()).setMethod(method, graph);
        if (method != null) {
            this.setDisplayName(method.getName());
        }

    }
}
