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
 * @bug 7021614 8031212
 * @summary extend com.sun.source API to support parsing javadoc comments
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.tree
 *          jdk.compiler/com.sun.tools.javac.util
 * @build DocCommentTester
 * @run main DocCommentTester SeeTest.java
 */

class SeeTest {
    /**
     * abc.
     * @see "String"
     */
    void quoted_text() { }
/*
DocComment[DOC_COMMENT, pos:1
  firstSentence: 1
    Text[TEXT, pos:1, abc.]
  body: empty
  block tags: 1
    See[SEE, pos:7
      reference: 1
        Text[TEXT, pos:12, "String"]
    ]
]
*/

    /**
     * abc.
     * @see <a href="url">url</a>
     */
    void url() { }
/*
DocComment[DOC_COMMENT, pos:1
  firstSentence: 1
    Text[TEXT, pos:1, abc.]
  body: empty
  block tags: 1
    See[SEE, pos:7
      reference: 3
        StartElement[START_ELEMENT, pos:12
          name:a
          attributes: 1
            Attribute[ATTRIBUTE, pos:15
              name: href
              vkind: DOUBLE
              value: 1
                Text[TEXT, pos:21, url]
            ]
        ]
        Text[TEXT, pos:26, url]
        EndElement[END_ELEMENT, pos:29, a]
    ]
]
*/

    /**
     * abc.
     * @see String text
     */
    void string() { }
/*
DocComment[DOC_COMMENT, pos:1
  firstSentence: 1
    Text[TEXT, pos:1, abc.]
  body: empty
  block tags: 1
    See[SEE, pos:7
      reference: 2
        Reference[REFERENCE, pos:12, String]
        Text[TEXT, pos:19, text]
    ]
]
*/

    /**
     * abc.
     * @see java.lang.String text
     */
    void j_l_string() { }
/*
DocComment[DOC_COMMENT, pos:1
  firstSentence: 1
    Text[TEXT, pos:1, abc.]
  body: empty
  block tags: 1
    See[SEE, pos:7
      reference: 2
        Reference[REFERENCE, pos:12, java.lang.String]
        Text[TEXT, pos:29, text]
    ]
]
*/

    /**
     * abc.
     * @see java.lang.String#length text
     */
    void j_l_string_length() { }
/*
DocComment[DOC_COMMENT, pos:1
  firstSentence: 1
    Text[TEXT, pos:1, abc.]
  body: empty
  block tags: 1
    See[SEE, pos:7
      reference: 2
        Reference[REFERENCE, pos:12, java.lang.String#length]
        Text[TEXT, pos:36, text]
    ]
]
*/

    /**
     * abc.
     * @see java.lang.String#matches(String regex) text
     */
    void j_l_string_matches() { }
/*
DocComment[DOC_COMMENT, pos:1
  firstSentence: 1
    Text[TEXT, pos:1, abc.]
  body: empty
  block tags: 1
    See[SEE, pos:7
      reference: 2
        Reference[REFERENCE, pos:12, java.lang.String...#matches(String_regex)]
        Text[TEXT, pos:51, text]
    ]
]
*/

    /**
     * abc.
     * @see 123 text
     */
    void bad_numeric() { }
/*
DocComment[DOC_COMMENT, pos:1
  firstSentence: 1
    Text[TEXT, pos:1, abc.]
  body: empty
  block tags: 1
    Erroneous[ERRONEOUS, pos:7
      code: compiler.err.dc.unexpected.content
      body: @see_123_text
    ]
]
*/

}
