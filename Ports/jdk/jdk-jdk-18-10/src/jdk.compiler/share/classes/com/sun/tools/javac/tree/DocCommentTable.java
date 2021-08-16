/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.tree;

import com.sun.source.doctree.ErroneousTree;
import com.sun.tools.javac.parser.Tokens.Comment;
import com.sun.tools.javac.tree.DCTree.DCDocComment;

/**
 * A table giving the doc comment, if any, for any tree node.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own
 *  risk.  This code and its internal interfaces are subject to change
 *  or deletion without notice.</b>
 */
public interface DocCommentTable {
    /**
     * Check if a tree node has a corresponding doc comment.
     */
    boolean hasComment(JCTree tree);

    /**
     * Get the Comment token containing the doc comment, if any, for a tree node.
     */
    Comment getComment(JCTree tree);

    /**
     * Get the plain text of the doc comment, if any, for a tree node.
     */
    String getCommentText(JCTree tree);

    /**
     * Get the parsed form of the doc comment as a DocTree. If any errors
     * are detected during parsing, they will be reported via
     * {@link ErroneousTree ErroneousTree} nodes within the resulting tree.
     */
    DCDocComment getCommentTree(JCTree tree);

    /**
     * Set the Comment to be associated with a tree node.
     */
    void putComment(JCTree tree, Comment c);
}
