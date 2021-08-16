/*
 * Copyright (c) 2005, 2014, Oracle and/or its affiliates. All rights reserved.
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
 * $Id: NodeSetData.java,v 1.5 2005/05/10 15:47:42 mullan Exp $
 */
package javax.xml.crypto;

import java.util.Iterator;

/**
 * An abstract representation of a <code>Data</code> type containing a
 * node-set. The type (class) and ordering of the nodes contained in the set
 * are not defined by this class; instead that behavior should be
 * defined by <code>NodeSetData</code> subclasses.
 *
 * @author Sean Mullan
 * @author JSR 105 Expert Group
 * @param <T> the type of nodes maintained by this set
 * @since 1.6
 */
public interface NodeSetData<T> extends Data, Iterable<T> {

    /**
     * Returns a read-only iterator over the nodes contained in this
     * <code>NodeSetData</code> in
     * <a href="http://www.w3.org/TR/1999/REC-xpath-19991116#dt-document-order">
     * document order</a>. Attempts to modify the returned iterator
     * via the <code>remove</code> method throw
     * <code>UnsupportedOperationException</code>.
     *
     * @return an <code>Iterator</code> over the nodes in this
     *    <code>NodeSetData</code> in document order
     */
    Iterator<T> iterator();
}
