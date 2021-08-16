/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

package sampleapi.util;

import java.util.HashMap;

import com.sun.tools.javac.parser.Tokens.Comment;
import com.sun.tools.javac.tree.DCTree.DCDocComment;
import com.sun.tools.javac.tree.DocCommentTable;
import com.sun.tools.javac.tree.JCTree;

/*
 * This class is replica of LazyDocCommentTable from com.sun.tools.javac.parser
 * package. It's created due to restrictions of LazyDocCommentTable (cannot be
 * used outside the package) and implements minimal functionality necessary
 * for doc comment generation purposes.
 */
public class PoorDocCommentTable implements DocCommentTable {

    HashMap<JCTree, Comment> table;

    public PoorDocCommentTable() {
        table = new HashMap<>();
    }

    public boolean hasComment(JCTree tree) {
        return table.containsKey(tree);
    }

    public Comment getComment(JCTree tree) {
        return table.get(tree);
    }

    public String getCommentText(JCTree tree) {
        Comment c = getComment(tree);
        return (c == null) ? null : c.getText();
    }

    public DCDocComment getCommentTree(JCTree tree) {
        return null; // no need for generator purposes, Pretty does not call it
    }

    public void putComment(JCTree tree, Comment c) {
        table.put(tree, c);
    }
}
