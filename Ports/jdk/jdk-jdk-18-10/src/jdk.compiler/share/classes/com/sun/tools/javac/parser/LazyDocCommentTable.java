/*
 * Copyright (c) 1999, 2020, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.parser;

import java.util.HashMap;
import java.util.Map;

import com.sun.tools.javac.parser.Tokens.Comment;
import com.sun.tools.javac.tree.DCTree.DCDocComment;
import com.sun.tools.javac.tree.DocCommentTable;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.DiagnosticSource;

/**
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class LazyDocCommentTable implements DocCommentTable {
    private static class Entry {
        final Comment comment;
        DCDocComment tree;

        Entry(Comment c) {
            comment = c;
        }
    }

    private final ParserFactory fac;
    private final DiagnosticSource diagSource;
    private final Map<JCTree, Entry> table;

    LazyDocCommentTable(ParserFactory fac) {
        this.fac = fac;
        diagSource = fac.log.currentSource();
        table = new HashMap<>();
    }

    @Override
    public boolean hasComment(JCTree tree) {
        return table.containsKey(tree);
    }

    @Override
    public Comment getComment(JCTree tree) {
        Entry e = table.get(tree);
        return (e == null) ? null : e.comment;
    }

    @Override
    public String getCommentText(JCTree tree) {
        Comment c = getComment(tree);
        return (c == null) ? null : c.getText();
    }

    @Override
    public DCDocComment getCommentTree(JCTree tree) {
        Entry e = table.get(tree);
        if (e == null)
            return null;
        if (e.tree == null)
            e.tree = new DocCommentParser(fac, diagSource, e.comment).parse();
        return e.tree;
    }

    @Override
    public void putComment(JCTree tree, Comment c) {
        table.put(tree, new Entry(c));
    }

}
