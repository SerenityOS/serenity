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

package com.sun.source.tree;

import java.util.List;

/**
 * A tree node for a {@code try} statement.
 *
 * For example:
 * <pre>
 *   try
 *       <em>block</em>
 *   <em>catches</em>
 *   finally
 *       <em>finallyBlock</em>
 * </pre>
 *
 * @jls 14.20 The try statement
 *
 * @author Peter von der Ah&eacute;
 * @author Jonathan Gibbons
 * @since 1.6
 */
public interface TryTree extends StatementTree {
    /**
     * Returns the block of the {@code try} statement.
     * @return the block
     */
    BlockTree getBlock();

    /**
     * Returns any catch blocks provided in the {@code try} statement.
     * The result will be an empty list if there are no
     * catch blocks.
     * @return the catch blocks
     */
    List<? extends CatchTree> getCatches();

    /**
     * Returns the finally block provided in the {@code try} statement,
     * or {@code null} if there is none.
     * @return the finally block
     */
    BlockTree getFinallyBlock();


    /**
     * Returns any resource declarations provided in the {@code try} statement.
     * The result will be an empty list if there are no
     * resource declarations.
     * @return the resource declarations
     */
    List<? extends Tree> getResources();
}
