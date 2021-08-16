/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.source.doctree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The top-level representation of a documentation comment.
 *
 * <pre>
 *    first-sentence body block-tags
 * </pre>
 *
 * @since 1.8
 */
public interface DocCommentTree extends DocTree {
    /**
     * Returns the first sentence of a documentation comment.
     * @return the first sentence of a documentation comment
     */
    List<? extends DocTree> getFirstSentence();

    /**
     * Returns the entire body of a documentation comment, appearing
     * before any block tags, including the first sentence.
     * @return body of a documentation comment first sentence inclusive
     *
     * @since 9
     */
    default List<? extends DocTree> getFullBody() {
        ArrayList<DocTree> bodyList = new ArrayList<>();
        bodyList.addAll(getFirstSentence());
        bodyList.addAll(getBody());
        return bodyList;
    }

    /**
     * Returns the body of a documentation comment,
     * appearing after the first sentence, and before any block tags.
     * @return the body of a documentation comment
     */
    List<? extends DocTree> getBody();

    /**
     * Returns the block tags for a documentation comment.
     * @return the block tags of a documentation comment
     */
    List<? extends DocTree> getBlockTags();

    /**
     * Returns a list of trees containing the content (if any) preceding
     * the content of the documentation comment.
     * When the {@code DocCommentTree} has been read from a documentation
     * comment in a Java source file, the list will be empty.
     * When the {@code DocCommentTree} has been read from an HTML file, this
     * represents the content from the beginning of the file up to and
     * including the {@code <body>} tag.
     *
     * @implSpec This implementation returns an empty list.
     *
     * @return the list of trees
     * @since 10
     */
    default List<? extends DocTree> getPreamble() {
        return Collections.emptyList();
    }

    /**
     * Returns a list of trees containing the content (if any) following the
     * content of the documentation comment.
     * When the {@code DocCommentTree} has been read from a documentation
     * comment in a Java source file, the list will be empty.
     * When {@code DocCommentTree} has been read from an HTML file, this
     * represents the content from the {@code </body>} tag to the end of file.
     *
     * @implSpec This implementation returns an empty list.
     *
     * @return the list of trees
     * @since 10
     */
    default List<? extends DocTree> getPostamble() {
        return Collections.emptyList();
    }
}
