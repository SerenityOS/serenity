/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5076751
 * @summary System properties documentation needed in javadocs
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.tree
 *          jdk.compiler/com.sun.tools.javac.util
 * @build DocCommentTester
 * @run main DocCommentTester SystemPropertyTest.java
 */

class SystemPropertyTest {
    /**
     * abc {@systemProperty xyz.qwe} def
     */
    void simple_term() {}
/*
DocComment[DOC_COMMENT, pos:1
  firstSentence: 3
    Text[TEXT, pos:1, abc_]
    SystemProperty[SYSTEM_PROPERTY, pos:5
      property name: xyz.qwe
    ]
    Text[TEXT, pos:30, _def]
  body: empty
  block tags: empty
]
*/

    /**
     * abc {@systemProperty xyz qwe}
     */
    void bad_with_whitespace() {}
/*
DocComment[DOC_COMMENT, pos:1
  firstSentence: 3
    Text[TEXT, pos:1, abc_]
    Erroneous[ERRONEOUS, pos:5
      code: compiler.err.dc.unexpected.content
      body: {@systemProperty_xyz_q
    ]
    Text[TEXT, pos:27, we}]
  body: empty
  block tags: empty
]
*/

    /**
     * abc {@systemProperty}
     */
    void bad_no_property() {}
/*
DocComment[DOC_COMMENT, pos:1
  firstSentence: 3
    Text[TEXT, pos:1, abc_]
    Erroneous[ERRONEOUS, pos:5
      code: compiler.err.dc.no.content
      body: {@systemProperty
    ]
    Text[TEXT, pos:21, }]
  body: empty
  block tags: empty
]
*/

}
