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

/*
 * @test
 * @bug 8078320
 * @summary extend com.sun.source API to support parsing javadoc comments
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.tree
 *          jdk.compiler/com.sun.tools.javac.util
 * @build DocCommentTester
 * @run main DocCommentTester InPreTest.java
 */

class InPreTest {
    /**
     * xyz<pre> pqr </pre> abc{@code  def  }ghi
     */
    public void after_pre() { }
/*
DocComment[DOC_COMMENT, pos:1
  firstSentence: 1
    Text[TEXT, pos:1, xyz]
  body: 6
    StartElement[START_ELEMENT, pos:4
      name:pre
      attributes: empty
    ]
    Text[TEXT, pos:9, _pqr_]
    EndElement[END_ELEMENT, pos:14, pre]
    Text[TEXT, pos:20, _abc]
    Literal[CODE, pos:24, _def__]
    Text[TEXT, pos:38, ghi]
  block tags: empty
]
*/
    /**
     * abc{@code def}ghi
     */
    public void no_pre() { }
/*
DocComment[DOC_COMMENT, pos:1
  firstSentence: 3
    Text[TEXT, pos:1, abc]
    Literal[CODE, pos:4, def]
    Text[TEXT, pos:15, ghi]
  body: empty
  block tags: empty
]
*/
    /**
     * xyz<pre> abc{@code  def  }ghi</pre>
     */
    public void pre_after_text() {}
/*
DocComment[DOC_COMMENT, pos:1
  firstSentence: 1
    Text[TEXT, pos:1, xyz]
  body: 5
    StartElement[START_ELEMENT, pos:4
      name:pre
      attributes: empty
    ]
    Text[TEXT, pos:9, _abc]
    Literal[CODE, pos:13, _def__]
    Text[TEXT, pos:27, ghi]
    EndElement[END_ELEMENT, pos:30, pre]
  block tags: empty
]
*/

    /**
     * abc{@code  def  }ghi
     */
    public void no_pre_extra_whitespace() { }
/*
DocComment[DOC_COMMENT, pos:1
  firstSentence: 3
    Text[TEXT, pos:1, abc]
    Literal[CODE, pos:4, _def__]
    Text[TEXT, pos:18, ghi]
  body: empty
  block tags: empty
]
*/
    /**
     * <pre> abc{@code  def  }ghi</pre>
     */
    public void in_pre() { }
/*
DocComment[DOC_COMMENT, pos:1
  firstSentence: 4
    StartElement[START_ELEMENT, pos:1
      name:pre
      attributes: empty
    ]
    Text[TEXT, pos:6, _abc]
    Literal[CODE, pos:10, _def__]
    Text[TEXT, pos:24, ghi]
  body: 1
    EndElement[END_ELEMENT, pos:27, pre]
  block tags: empty
]
*/
    /**
     * <pre> abc{@code
     * def  }ghi</pre>
     */
    public void in_pre_with_space_nl() { }
/*
DocComment[DOC_COMMENT, pos:1
  firstSentence: 4
    StartElement[START_ELEMENT, pos:1
      name:pre
      attributes: empty
    ]
    Text[TEXT, pos:6, _abc]
    Literal[CODE, pos:10, |_def__]
    Text[TEXT, pos:24, ghi]
  body: 1
    EndElement[END_ELEMENT, pos:27, pre]
  block tags: empty
]
*/

    /**
     * <pre> abc{@code
     *def  }ghi</pre>
     */
    public void in_pre_with_nl() { }
/*
DocComment[DOC_COMMENT, pos:1
  firstSentence: 4
    StartElement[START_ELEMENT, pos:1
      name:pre
      attributes: empty
    ]
    Text[TEXT, pos:6, _abc]
    Literal[CODE, pos:10, |def__]
    Text[TEXT, pos:23, ghi]
  body: 1
    EndElement[END_ELEMENT, pos:26, pre]
  block tags: empty
]
*/
    /**
     * abc {@code
     */
    public void bad_code_no_content() { }
/*
DocComment[DOC_COMMENT, pos:1
  firstSentence: 2
    Text[TEXT, pos:1, abc_]
    Erroneous[ERRONEOUS, pos:5
      code: compiler.err.dc.unterminated.inline.tag
      body: {@code
    ]
  body: empty
  block tags: empty
]
*/
    /**
     * abc {@code abc
     */
    public void bad_code_content() { }
/*
DocComment[DOC_COMMENT, pos:1
  firstSentence: 2
    Text[TEXT, pos:1, abc_]
    Erroneous[ERRONEOUS, pos:5
      code: compiler.err.dc.unterminated.inline.tag
      body: {@code_abc
    ]
  body: empty
  block tags: empty
]
*/
}
