/*
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
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

package sun.tools.jconsole.inspector;

/**
 * <p>This class represents the user object of the nodes in the MBean tree.</p>
 *
 * <p>It encapsulates the node's info, i.e. the type of the node, the label to
 * be used when displaying the node in the MBean tree, the node's tool tip text
 * and arbitrary data which varies depending on the type of the node: an XMBean
 * reference for MBEAN, ATTRIBUTES, OPERATIONS and NOTIFICATIONS nodes; the
 * corresponding MBeanInfo for ATTRIBUTE, OPERATION and NOTIFICATION nodes;
 * it is not used for NONMBEAN nodes.</p>
 */
public class XNodeInfo {

    public static enum Type {
        MBEAN, NONMBEAN,
        ATTRIBUTES, OPERATIONS, NOTIFICATIONS,
        ATTRIBUTE, OPERATION, NOTIFICATION
    };

    public XNodeInfo(Type type, Object data, String label, String tooltip) {
        this.type = type;
        this.data = data;
        this.label = label;
        this.tooltip = tooltip;
    }

    public Type getType() {
        return type;
    }

    public Object getData() {
        return data;
    }

    public String getLabel() {
        return label;
    }

    public String getToolTipText() {
        return tooltip;
    }

    public String toString() {
        return label;
    }

    private Type type;
    private Object data;
    private String label;
    private String tooltip;
}
