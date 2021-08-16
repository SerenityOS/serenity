/*
 * Copyright (c) 2002, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @summary unicode escapes delimiting and inside of comments
 *
 * @compile UnicodeCommentDelimiter.java
 */

class UnicodeCommentDelimiter {
    public static void main(String[] args) {
        // no error on the following line because although \u005c
        // represents a backslash, that cannot be considered to begin
        // a unicode escape sequence.
        // \u005c000a xyzzy plugh;

        // no error on the following line because there are an even
        // number of backslashes before the u, meaning it is not a
        // unicode escape sequence.
        // \\u000a xyzzy plugh;

        // However, unicode escaped characters can delimit comments.
        \u002f\u002f xyzzy plugh;

        // \u000a class plugh{}
        plugh xyzzy;
    }
}
