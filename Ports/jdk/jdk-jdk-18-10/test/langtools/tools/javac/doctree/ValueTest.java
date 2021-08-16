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
 * @bug 7021614
 * @summary extend com.sun.source API to support parsing javadoc comments
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.tree
 *          jdk.compiler/com.sun.tools.javac.util
 * @build DocCommentTester
 * @run main DocCommentTester ValueTest.java
 */

class ValueTest {
    /**
     * abc {@value}
     */
    int no_ref() { }
/*
DocComment[DOC_COMMENT, pos:1
  firstSentence: 2
    Text[TEXT, pos:1, abc_]
    Value[VALUE, pos:5
      reference: null
    ]
  body: empty
  block tags: empty
]
*/

    /**
     * abc {@value java.awt.Color#RED}
     */
    int typical() { }
/*
DocComment[DOC_COMMENT, pos:1
  firstSentence: 2
    Text[TEXT, pos:1, abc_]
    Value[VALUE, pos:5
      reference:
        Reference[REFERENCE, pos:13, java.awt.Color#RED]
    ]
  body: empty
  block tags: empty
]
*/

    /**
     * abc {@value java.awt.Color#RED }
     */
    int trailing_ws() { }
/*
DocComment[DOC_COMMENT, pos:1
  firstSentence: 2
    Text[TEXT, pos:1, abc_]
    Value[VALUE, pos:5
      reference:
        Reference[REFERENCE, pos:13, java.awt.Color#RED]
    ]
  body: empty
  block tags: empty
]
*/

    /**
     * abc {@value java.awt.Color#RED junk}
     */
    int trailing_junk() { }
/*
DocComment[DOC_COMMENT, pos:1
  firstSentence: 3
    Text[TEXT, pos:1, abc_]
    Erroneous[ERRONEOUS, pos:5
      code: compiler.err.dc.unexpected.content
      body: {@value_java.awt.Color#RED_j
    ]
    Text[TEXT, pos:33, unk}]
  body: empty
  block tags: empty
]
*/

}


