/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7021614  8241780
 * @summary extend com.sun.source API to support parsing javadoc comments
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.tree
 *          jdk.compiler/com.sun.tools.javac.util
 * @build DocCommentTester
 * @run main DocCommentTester LiteralTest.java
 */

class LiteralTest {
    /** {@literal if (a < b) { }} */
    void minimal() { }
/*
DocComment[DOC_COMMENT, pos:0
  firstSentence: 1
    Literal[LITERAL, pos:0, if_(a_<_b)_{_}]
  body: empty
  block tags: empty
]
*/

    /** [{@literal if (a < b) { }}] */
    void in_brackets() { }
/*
DocComment[DOC_COMMENT, pos:0
  firstSentence: 3
    Text[TEXT, pos:0, []
    Literal[LITERAL, pos:1, if_(a_<_b)_{_}]
    Text[TEXT, pos:26, ]]
  body: empty
  block tags: empty
]
*/

    /** [ {@literal if (a < b) { }} ] */
    void in_brackets_with_whitespace() { }
/*
DocComment[DOC_COMMENT, pos:0
  firstSentence: 3
    Text[TEXT, pos:0, [_]
    Literal[LITERAL, pos:2, if_(a_<_b)_{_}]
    Text[TEXT, pos:27, _]]
  body: empty
  block tags: empty
]
*/

    /**
     * {@literal {@literal nested} }
     */
    void nested() { }
/*
DocComment[DOC_COMMENT, pos:1
  firstSentence: 1
    Literal[LITERAL, pos:1, {@literal_nested}_]
  body: empty
  block tags: empty
]
*/

    /**
     * {@literal if (a < b) {
     *        }
     * }
     */
    void embedded_newline() { }
/*
DocComment[DOC_COMMENT, pos:1
  firstSentence: 1
    Literal[LITERAL, pos:1, if_(a_<_b)_{|________}|_]
  body: empty
  block tags: empty
]
*/

    /**
     * {@literal
     * @tag
     * }
     */
    void embedded_at() { }
/*
DocComment[DOC_COMMENT, pos:1
  firstSentence: 1
    Literal[LITERAL, pos:1, |_@tag|_]
  body: empty
  block tags: empty
]
*/


    /** {@literal if (a < b) { } */
    void unterminated_1() { }
/*
DocComment[DOC_COMMENT, pos:0
  firstSentence: 1
    Erroneous[ERRONEOUS, pos:0
      code: compiler.err.dc.unterminated.inline.tag
      body: {@literal_if_(a_<_b)_{_}
    ]
  body: empty
  block tags: empty
]
*/

    /**
     * {@literal if (a < b) { }
     * @author jjg */
    void unterminated_2() { }
/*
DocComment[DOC_COMMENT, pos:1
  firstSentence: 1
    Erroneous[ERRONEOUS, pos:1
      code: compiler.err.dc.unterminated.inline.tag
      body: {@literal_if_(a_...<_b)_{_}|_@author_jjg
    ]
  body: empty
  block tags: empty
]
*/

}

