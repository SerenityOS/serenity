/*
 * Copyright (c) 2005, 2020, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.source.util;

import com.sun.source.tree.*;

/**
 * Provides methods to obtain the position of a Tree within a CompilationUnit.
 * A position is defined as a simple character offset from the start of a
 * CompilationUnit where the first character is at offset 0.
 *
 * @author Peter von der Ah&eacute;
 * @since 1.6
 */
public interface SourcePositions {

    /**
     * Returns the starting position of tree within file.  If tree is not found within
     * file, or if the starting position is not available,
     * returns {@link javax.tools.Diagnostic#NOPOS}.
     * The returned position must be at the start of the yield of this tree, that
     * is for any sub-tree of this tree, the following must hold:
     *
     * <p>
     * {@code tree.getStartPosition() <= subtree.getStartPosition()} or <br>
     * {@code tree.getStartPosition() == NOPOS} or <br>
     * {@code subtree.getStartPosition() == NOPOS}
     * </p>
     *
     * @param file CompilationUnit in which to find tree
     * @param tree tree for which a position is sought
     * @return the start position of tree
     */
     long getStartPosition(CompilationUnitTree file, Tree tree);

    /**
     * Returns the ending position of tree within file.  If tree is not found within
     * file, or if the ending position is not available,
     * returns {@link javax.tools.Diagnostic#NOPOS}.
     * The returned position must be at the end of the yield of this tree,
     * that is for any sub-tree of this tree, the following must hold:
     *
     * <p>
     * {@code tree.getEndPosition() >= subtree.getEndPosition()} or <br>
     * {@code tree.getEndPosition() == NOPOS} or <br>
     * {@code subtree.getEndPosition() == NOPOS}
     * </p>
     *
     * In addition, the following must hold:
     *
     * <p>
     * {@code tree.getStartPosition() <= tree.getEndPosition()} or <br>
     * {@code tree.getStartPosition() == NOPOS} or <br>
     * {@code tree.getEndPosition() == NOPOS}
     * </p>
     *
     * @param file CompilationUnit in which to find tree
     * @param tree tree for which a position is sought
     * @return the end position of tree
     */
     long getEndPosition(CompilationUnitTree file, Tree tree);

}
