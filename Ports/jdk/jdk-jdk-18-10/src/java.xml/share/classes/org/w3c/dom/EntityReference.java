/*
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

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file and, per its terms, should not be removed:
 *
 * Copyright (c) 2004 World Wide Web Consortium,
 *
 * (Massachusetts Institute of Technology, European Research Consortium for
 * Informatics and Mathematics, Keio University). All Rights Reserved. This
 * work is distributed under the W3C(r) Software License [1] in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * [1] http://www.w3.org/Consortium/Legal/2002/copyright-software-20021231
 */

package org.w3c.dom;

/**
 * <code>EntityReference</code> nodes may be used to represent an entity
 * reference in the tree. Note that character references and references to
 * predefined entities are considered to be expanded by the HTML or XML
 * processor so that characters are represented by their Unicode equivalent
 * rather than by an entity reference. Moreover, the XML processor may
 * completely expand references to entities while building the
 * <code>Document</code>, instead of providing <code>EntityReference</code>
 * nodes. If it does provide such nodes, then for an
 * <code>EntityReference</code> node that represents a reference to a known
 * entity an <code>Entity</code> exists, and the subtree of the
 * <code>EntityReference</code> node is a copy of the <code>Entity</code>
 * node subtree. However, the latter may not be true when an entity contains
 * an unbound namespace prefix. In such a case, because the namespace prefix
 * resolution depends on where the entity reference is, the descendants of
 * the <code>EntityReference</code> node may be bound to different namespace
 * URIs. When an <code>EntityReference</code> node represents a reference to
 * an unknown entity, the node has no children and its replacement value,
 * when used by <code>Attr.value</code> for example, is empty.
 * <p>As for <code>Entity</code> nodes, <code>EntityReference</code> nodes and
 * all their descendants are readonly.
 * <p ><b>Note:</b> <code>EntityReference</code> nodes may cause element
 * content and attribute value normalization problems when, such as in XML
 * 1.0 and XML Schema, the normalization is performed after entity reference
 * are expanded.
 * <p>See also the <a href='http://www.w3.org/TR/2004/REC-DOM-Level-3-Core-20040407'>Document Object Model (DOM) Level 3 Core Specification</a>.
 */
public interface EntityReference extends Node {
}
