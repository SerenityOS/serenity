/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7021614 8076026
 * @summary extend com.sun.source API to support parsing javadoc comments
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.tree
 *          jdk.compiler/com.sun.tools.javac.util
 * @build DocCommentTester
 * @run main DocCommentTester AttrTest.java
 */

class AttrTest {
    /**
     * <a name=unquoted>foo</a>
     */
    void unquoted_attr() { }
/*
DocComment[DOC_COMMENT, pos:1
  firstSentence: 3
    StartElement[START_ELEMENT, pos:1
      name:a
      attributes: 1
        Attribute[ATTRIBUTE, pos:4
          name: name
          vkind: UNQUOTED
          value: 1
            Text[TEXT, pos:9, unquoted]
        ]
    ]
    Text[TEXT, pos:18, foo]
    EndElement[END_ELEMENT, pos:21, a]
  body: empty
  block tags: empty
]
*/

    /**
     * <a name-test=hyphened>foo</a>
     */
    void hyphened_attr() { }
/*
DocComment[DOC_COMMENT, pos:1
  firstSentence: 3
    StartElement[START_ELEMENT, pos:1
      name:a
      attributes: 1
        Attribute[ATTRIBUTE, pos:4
          name: name-test
          vkind: UNQUOTED
          value: 1
            Text[TEXT, pos:14, hyphened]
        ]
    ]
    Text[TEXT, pos:23, foo]
    EndElement[END_ELEMENT, pos:26, a]
  body: empty
  block tags: empty
]
*/

    /**
     * <a name="double_quoted">foo</a>
     */
    void double_quoted_attr() { }
/*
DocComment[DOC_COMMENT, pos:1
  firstSentence: 3
    StartElement[START_ELEMENT, pos:1
      name:a
      attributes: 1
        Attribute[ATTRIBUTE, pos:4
          name: name
          vkind: DOUBLE
          value: 1
            Text[TEXT, pos:10, double_quoted]
        ]
    ]
    Text[TEXT, pos:25, foo]
    EndElement[END_ELEMENT, pos:28, a]
  body: empty
  block tags: empty
]
*/

    /**
     * <a name='single_quoted'>foo</a>
     */
    void single_quoted_attr() { }
/*
DocComment[DOC_COMMENT, pos:1
  firstSentence: 3
    StartElement[START_ELEMENT, pos:1
      name:a
      attributes: 1
        Attribute[ATTRIBUTE, pos:4
          name: name
          vkind: SINGLE
          value: 1
            Text[TEXT, pos:10, single_quoted]
        ]
    ]
    Text[TEXT, pos:25, foo]
    EndElement[END_ELEMENT, pos:28, a]
  body: empty
  block tags: empty
]
*/

    /**
     * <hr size="3">
     */
    void numeric_attr() { }
/*
DocComment[DOC_COMMENT, pos:1
  firstSentence: 1
    StartElement[START_ELEMENT, pos:1
      name:hr
      attributes: 1
        Attribute[ATTRIBUTE, pos:5
          name: size
          vkind: DOUBLE
          value: 1
            Text[TEXT, pos:11, 3]
        ]
    ]
  body: empty
  block tags: empty
]
*/

    /**
     * <a href="{@docRoot}/index.html">
     */
    void docRoot_attr() { }
/*
DocComment[DOC_COMMENT, pos:1
  firstSentence: 1
    StartElement[START_ELEMENT, pos:1
      name:a
      attributes: 1
        Attribute[ATTRIBUTE, pos:4
          name: href
          vkind: DOUBLE
          value: 2
            DocRoot[DOC_ROOT, pos:10]
            Text[TEXT, pos:20, /index.html]
        ]
    ]
  body: empty
  block tags: empty
]
*/

    /**
     * <a name="abc&quot;def">
     */
    void entity_attr() { }
/*
DocComment[DOC_COMMENT, pos:1
  firstSentence: 1
    StartElement[START_ELEMENT, pos:1
      name:a
      attributes: 1
        Attribute[ATTRIBUTE, pos:4
          name: name
          vkind: DOUBLE
          value: 3
            Text[TEXT, pos:10, abc]
            Entity[ENTITY, pos:13, quot]
            Text[TEXT, pos:19, def]
        ]
    ]
  body: empty
  block tags: empty
]
*/

    /**
     * <hr noshade>
     */
    void no_value_attr() { }
/*
DocComment[DOC_COMMENT, pos:1
  firstSentence: 1
    StartElement[START_ELEMENT, pos:1
      name:hr
      attributes: 1
        Attribute[ATTRIBUTE, pos:5
          name: noshade
          vkind: EMPTY
          value: null
        ]
    ]
  body: empty
  block tags: empty
]
*/

    /**
     * abc <hr size='3'/>
     */
    void self_closing_attr_1() { }
/*
DocComment[DOC_COMMENT, pos:1
  firstSentence: 2
    Text[TEXT, pos:1, abc_]
    StartElement[START_ELEMENT, pos:5
      name:hr
      attributes: 1
        Attribute[ATTRIBUTE, pos:9
          name: size
          vkind: SINGLE
          value: 1
            Text[TEXT, pos:15, 3]
        ]
    ]
  body: empty
  block tags: empty
]
*/

    /**
     * abc <hr size=3 />
     */
    void self_closing_attr_2() { }
/*
DocComment[DOC_COMMENT, pos:1
  firstSentence: 2
    Text[TEXT, pos:1, abc_]
    StartElement[START_ELEMENT, pos:5
      name:hr
      attributes: 1
        Attribute[ATTRIBUTE, pos:9
          name: size
          vkind: UNQUOTED
          value: 1
            Text[TEXT, pos:14, 3]
        ]
    ]
  body: empty
  block tags: empty
]
*/

    /**
     * abc <hr size="3
     */
    void unterminated_attr_eoi() { }
/*
DocComment[DOC_COMMENT, pos:1
  firstSentence: 3
    Text[TEXT, pos:1, abc_]
    Erroneous[ERRONEOUS, pos:5
      code: compiler.err.dc.malformed.html
      body: <
    ]
    Text[TEXT, pos:6, hr_size="3]
  body: empty
  block tags: empty
]
*/

    /**
     * abc <hr size="3
     * @author jjg
     */
    void unterminated_attr_block() { }
/*
DocComment[DOC_COMMENT, pos:1
  firstSentence: 3
    Text[TEXT, pos:1, abc_]
    Erroneous[ERRONEOUS, pos:5
      code: compiler.err.dc.malformed.html
      body: <
    ]
    Text[TEXT, pos:6, hr_size="3]
  body: empty
  block tags: 1
    Author[AUTHOR, pos:18
      name: 1
        Text[TEXT, pos:26, jjg]
    ]
]
*/

    /**
     * <a name1="val1" name2='val2' name3=val3 name4>
     */
    void multiple_attr() { }
/*
DocComment[DOC_COMMENT, pos:1
  firstSentence: 1
    StartElement[START_ELEMENT, pos:1
      name:a
      attributes: 4
        Attribute[ATTRIBUTE, pos:4
          name: name1
          vkind: DOUBLE
          value: 1
            Text[TEXT, pos:11, val1]
        ]
        Attribute[ATTRIBUTE, pos:17
          name: name2
          vkind: SINGLE
          value: 1
            Text[TEXT, pos:24, val2]
        ]
        Attribute[ATTRIBUTE, pos:30
          name: name3
          vkind: UNQUOTED
          value: 1
            Text[TEXT, pos:36, val3]
        ]
        Attribute[ATTRIBUTE, pos:41
          name: name4
          vkind: EMPTY
          value: null
        ]
    ]
  body: empty
  block tags: empty
]
*/
}
