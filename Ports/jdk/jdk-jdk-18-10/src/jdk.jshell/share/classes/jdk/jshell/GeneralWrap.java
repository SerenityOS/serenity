/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jshell;

/**
 * Common interface for all wrappings of snippet source to Java source.
 *
 * Snippet index is index into the source of the snippet.  Note: If the snippet is a sub-range of
 * the source, the index is not the index in the snippet.
 *
 * Wrap index is index into the wrapped snippet.
 */
interface GeneralWrap {

    String wrapped();

    int snippetIndexToWrapIndex(int sni);

    int wrapIndexToSnippetIndex(int wi);

    default int wrapIndexToSnippetIndex(long wi) {
        return wrapIndexToSnippetIndex((int) wi);
    }

    int firstSnippetIndex();

    int lastSnippetIndex();

    int snippetLineToWrapLine(int snline);

    int wrapLineToSnippetLine(int wline);

    int firstSnippetLine();

    int lastSnippetLine();

    default String debugPos(long lpos) {
        int pos = (int) lpos;
        int len = wrapped().length();
        return wrapped().substring(Math.max(0, pos - 10), Math.max(0, Math.min(len, pos)))
                + "###"
                + wrapped().substring(Math.max(0, Math.min(len, pos)), Math.max(0, Math.min(len, pos + 10)));
    }
}
