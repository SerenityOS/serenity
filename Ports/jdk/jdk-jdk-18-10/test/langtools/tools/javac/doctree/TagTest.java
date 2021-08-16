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
 * @bug 7021614 8078320
 * @summary extend com.sun.source API to support parsing javadoc comments
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.tree
 *          jdk.compiler/com.sun.tools.javac.util
 * @build DocCommentTester
 * @run main DocCommentTester TagTest.java
 */

class TagTest {
    /**
     * @tag:colon abc
     */
    void custom_tag_with_a_colon() {}
/*
DocComment[DOC_COMMENT, pos:1
  firstSentence: empty
  body: empty
  block tags: 1
    UnknownBlockTag[UNKNOWN_BLOCK_TAG, pos:1
      tag:tag:colon
      content: 1
        Text[TEXT, pos:12, abc]
    ]
]
*/

    /**
     * @tag-hyphen abc
     */
    void custom_tag_with_a_hyphen() {}
/*
DocComment[DOC_COMMENT, pos:1
  firstSentence: empty
  body: empty
  block tags: 1
    UnknownBlockTag[UNKNOWN_BLOCK_TAG, pos:1
      tag:tag-hyphen
      content: 1
        Text[TEXT, pos:13, abc]
    ]
]
*/

    /**
     * @author jjg
     */
    void simple_standard_block() { }
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

    /**
     * @ abc
     */
    void no_name_block() { }
/*
DocComment[DOC_COMMENT, pos:1
  firstSentence: empty
  body: empty
  block tags: 1
    Erroneous[ERRONEOUS, pos:1
      code: compiler.err.dc.no.tag.name
      body: @_abc
    ]
]
*/

    /**
     * @abc def ghi
     */
    void unknown_name_block() { }
/*
DocComment[DOC_COMMENT, pos:1
  firstSentence: empty
  body: empty
  block tags: 1
    UnknownBlockTag[UNKNOWN_BLOCK_TAG, pos:1
      tag:abc
      content: 1
        Text[TEXT, pos:6, def_ghi]
    ]
]
*/

    /**
     * {@link String}
     */
    void simple_standard_inline() { }
/*
DocComment[DOC_COMMENT, pos:1
  firstSentence: 1
    Link[LINK, pos:1
      reference:
        Reference[REFERENCE, pos:8, String]
      body: empty
    ]
  body: empty
  block tags: empty
]
*/

    /**
     * {@ abc}
     */
    void no_name_inline() { }
/*
DocComment[DOC_COMMENT, pos:1
  firstSentence: 2
    Erroneous[ERRONEOUS, pos:1
      code: compiler.err.dc.no.tag.name
      body: {@
    ]
    Text[TEXT, pos:3, _abc}]
  body: empty
  block tags: empty
]
*/

    /**
     * {@abc def ghi}
     */
    void unknown_name_inline() { }
/*
DocComment[DOC_COMMENT, pos:1
  firstSentence: 1
    UnknownInlineTag[UNKNOWN_INLINE_TAG, pos:1
      tag:abc
      content: 1
        Text[TEXT, pos:7, def_ghi]
    ]
  body: empty
  block tags: empty
]
*/

    /**
     * {@abc def ghi
     */
    void unterminated_standard_inline() { }
/*
DocComment[DOC_COMMENT, pos:1
  firstSentence: 1
    Erroneous[ERRONEOUS, pos:1
      code: compiler.err.dc.unterminated.inline.tag
      body: {@abc_def_ghi
    ]
  body: empty
  block tags: empty
]
*/

}
