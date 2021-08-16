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
 * @bug 7021614 8078320 8132096
 * @summary extend com.sun.source API to support parsing javadoc comments
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.tree
 *          jdk.compiler/com.sun.tools.javac.util
 * @build DocCommentTester
 * @run main DocCommentTester FirstSentenceTest.java
 * @run main DocCommentTester -useBreakIterator FirstSentenceTest.java
 */

class FirstSentenceTest {
    /** */
    void empty() { }
/*
DocComment[DOC_COMMENT, pos:-1
  firstSentence: empty
  body: empty
  block tags: empty
]
*/
/*
BREAK_ITERATOR
DocComment[DOC_COMMENT, pos:-1
  firstSentence: empty
  body: empty
  block tags: empty
]
*/

    /** abc def ghi */
    void no_terminator() { }
/*
DocComment[DOC_COMMENT, pos:0
  firstSentence: 1
    Text[TEXT, pos:0, abc_def_ghi]
  body: empty
  block tags: empty
]
*/
/*
BREAK_ITERATOR
DocComment[DOC_COMMENT, pos:0
  firstSentence: 1
    Text[TEXT, pos:0, abc_def_ghi]
  body: empty
  block tags: empty
]
*/

    /**
     * abc def ghi.
     */
    void no_body() { }
/*
DocComment[DOC_COMMENT, pos:1
  firstSentence: 1
    Text[TEXT, pos:1, abc_def_ghi.]
  body: empty
  block tags: empty
]
*/
/*
BREAK_ITERATOR
DocComment[DOC_COMMENT, pos:1
  firstSentence: 1
    Text[TEXT, pos:1, abc_def_ghi.]
  body: empty
  block tags: empty
]
*/
    /**
     * abc def ghi. jkl mno pqr.
     */
    void dot_space() { }
/*
DocComment[DOC_COMMENT, pos:1
  firstSentence: 1
    Text[TEXT, pos:1, abc_def_ghi.]
  body: 1
    Text[TEXT, pos:14, jkl_mno_pqr.]
  block tags: empty
]
*/
/*
BREAK_ITERATOR
DocComment[DOC_COMMENT, pos:1
  firstSentence: 1
    Text[TEXT, pos:1, abc_def_ghi._jkl_mno_pqr.]
  body: empty
  block tags: empty
]
*/
    /**
     * abc def ghi.
     * jkl mno pqr
     */
    void dot_newline() { }
/*
DocComment[DOC_COMMENT, pos:1
  firstSentence: 1
    Text[TEXT, pos:1, abc_def_ghi.]
  body: 1
    Text[TEXT, pos:15, jkl_mno_pqr]
  block tags: empty
]
*/
/*
BREAK_ITERATOR
DocComment[DOC_COMMENT, pos:1
  firstSentence: 1
    Text[TEXT, pos:1, abc_def_ghi.]
  body: 1
    Text[TEXT, pos:15, jkl_mno_pqr]
  block tags: empty
]
*/
    /**
     * abc def ghi
     * <p>jkl mno pqr
     */
    void dot_p() { }
/*
DocComment[DOC_COMMENT, pos:1
  firstSentence: 1
    Text[TEXT, pos:1, abc_def_ghi]
  body: 2
    StartElement[START_ELEMENT, pos:14
      name:p
      attributes: empty
    ]
    Text[TEXT, pos:17, jkl_mno_pqr]
  block tags: empty
]
*/
/*
BREAK_ITERATOR
DocComment[DOC_COMMENT, pos:1
  firstSentence: 1
    Text[TEXT, pos:1, abc_def_ghi]
  body: 2
    StartElement[START_ELEMENT, pos:14
      name:p
      attributes: empty
    ]
    Text[TEXT, pos:17, jkl_mno_pqr]
  block tags: empty
]
*/
    /**
     *
     * <p>abc def ghi.
     * jdl mno pqf
     */
    void newline_p() { }
/*
DocComment[DOC_COMMENT, pos:2
  firstSentence: 2
    StartElement[START_ELEMENT, pos:2
      name:p
      attributes: empty
    ]
    Text[TEXT, pos:5, abc_def_ghi.]
  body: 1
    Text[TEXT, pos:19, jdl_mno_pqf]
  block tags: empty
]
*/
/*
BREAK_ITERATOR
DocComment[DOC_COMMENT, pos:2
  firstSentence: 2
    StartElement[START_ELEMENT, pos:2
      name:p
      attributes: empty
    ]
    Text[TEXT, pos:5, abc_def_ghi.]
  body: 1
    Text[TEXT, pos:19, jdl_mno_pqf]
  block tags: empty
]
*/
    /**
     * abc def ghi
     * </p>jkl mno pqr
     */
    void dot_end_p() { }
/*
DocComment[DOC_COMMENT, pos:1
  firstSentence: 1
    Text[TEXT, pos:1, abc_def_ghi]
  body: 2
    EndElement[END_ELEMENT, pos:14, p]
    Text[TEXT, pos:18, jkl_mno_pqr]
  block tags: empty
]
*/
/*
BREAK_ITERATOR
DocComment[DOC_COMMENT, pos:1
  firstSentence: 1
    Text[TEXT, pos:1, abc_def_ghi]
  body: 2
    EndElement[END_ELEMENT, pos:14, p]
    Text[TEXT, pos:18, jkl_mno_pqr]
  block tags: empty
]
*/
    /**
     * abc &lt; ghi. jkl mno pqr.
     */
    void entity() { }
/*
DocComment[DOC_COMMENT, pos:1
  firstSentence: 3
    Text[TEXT, pos:1, abc_]
    Entity[ENTITY, pos:5, lt]
    Text[TEXT, pos:9, _ghi.]
  body: 1
    Text[TEXT, pos:15, jkl_mno_pqr.]
  block tags: empty
]
*/
/*
BREAK_ITERATOR
DocComment[DOC_COMMENT, pos:1
  firstSentence: 3
    Text[TEXT, pos:1, abc_]
    Entity[ENTITY, pos:5, lt]
    Text[TEXT, pos:9, _ghi._jkl_mno_pqr.]
  body: empty
  block tags: empty
]
*/
    /**
     * abc {@code code} ghi. jkl mno pqr.
     */
    void inline_tag() { }
/*
DocComment[DOC_COMMENT, pos:1
  firstSentence: 3
    Text[TEXT, pos:1, abc_]
    Literal[CODE, pos:5, code]
    Text[TEXT, pos:17, _ghi.]
  body: 1
    Text[TEXT, pos:23, jkl_mno_pqr.]
  block tags: empty
]
*/
/*
BREAK_ITERATOR
DocComment[DOC_COMMENT, pos:1
  firstSentence: 3
    Text[TEXT, pos:1, abc_]
    Literal[CODE, pos:5, code]
    Text[TEXT, pos:17, _ghi._jkl_mno_pqr.]
  body: empty
  block tags: empty
]
*/
    /**
     * abc def ghi
     * @author jjg
     */
    void block_tag() { }
/*
DocComment[DOC_COMMENT, pos:1
  firstSentence: 1
    Text[TEXT, pos:1, abc_def_ghi]
  body: empty
  block tags: 1
    Author[AUTHOR, pos:14
      name: 1
        Text[TEXT, pos:22, jjg]
    ]
]
*/
/*
BREAK_ITERATOR
DocComment[DOC_COMMENT, pos:1
  firstSentence: 1
    Text[TEXT, pos:1, abc_def_ghi]
  body: empty
  block tags: 1
    Author[AUTHOR, pos:14
      name: 1
        Text[TEXT, pos:22, jjg]
    ]
]
*/
    /**
     * @author jjg
     */
    void just_tag() { }
/*
DocComment[DOC_COMMENT, pos:1
  firstSentence: empty
  body: empty
  block tags: 1
    Author[AUTHOR, pos:1
      name: 1
        Text[TEXT, pos:9, jjg]
    ]
]
*/
/*
BREAK_ITERATOR
DocComment[DOC_COMMENT, pos:1
  firstSentence: empty
  body: empty
  block tags: 1
    Author[AUTHOR, pos:1
      name: 1
        Text[TEXT, pos:9, jjg]
    ]
]
*/
    /**
     * <p> abc def.
     * ghi jkl
     */
    void p_at_zero() { }
/*
DocComment[DOC_COMMENT, pos:1
  firstSentence: 2
    StartElement[START_ELEMENT, pos:1
      name:p
      attributes: empty
    ]
    Text[TEXT, pos:4, _abc_def.]
  body: 1
    Text[TEXT, pos:15, ghi_jkl]
  block tags: empty
]
*/
/*
BREAK_ITERATOR
DocComment[DOC_COMMENT, pos:1
  firstSentence: 2
    StartElement[START_ELEMENT, pos:1
      name:p
      attributes: empty
    ]
    Text[TEXT, pos:4, _abc_def.]
  body: 1
    Text[TEXT, pos:15, ghi_jkl]
  block tags: empty
]
*/

    /**
     * abc <p> def. ghi jkl
     */
    void p_at_nonzero() { }
/*
DocComment[DOC_COMMENT, pos:1
  firstSentence: 1
    Text[TEXT, pos:1, abc]
  body: 2
    StartElement[START_ELEMENT, pos:5
      name:p
      attributes: empty
    ]
    Text[TEXT, pos:8, _def._ghi_jkl]
  block tags: empty
]
*/
/*
BREAK_ITERATOR
DocComment[DOC_COMMENT, pos:1
  firstSentence: 1
    Text[TEXT, pos:1, abc]
  body: 2
    StartElement[START_ELEMENT, pos:5
      name:p
      attributes: empty
    ]
    Text[TEXT, pos:8, _def._ghi_jkl]
  block tags: empty
]
*/
}

