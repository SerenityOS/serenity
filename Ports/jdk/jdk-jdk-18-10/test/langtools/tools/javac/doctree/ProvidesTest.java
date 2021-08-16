/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8160196
 * @summary Module summary page should display information based on "api" or "detail" mode.
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.tree
 *          jdk.compiler/com.sun.tools.javac.util
 * @build DocCommentTester
 * @run main DocCommentTester ProvidesTest.java
 */

class ProvidesTest {
    /**
     * abc.
     * @provides UsesTest
      */
    void simple_provides() { }
/*
DocComment[DOC_COMMENT, pos:1
  firstSentence: 1
    Text[TEXT, pos:1, abc.]
  body: empty
  block tags: 1
    Provides[PROVIDES, pos:7
      serviceName:
        Reference[REFERENCE, pos:17, UsesTest]
      description: empty
    ]
]
*/

    /**
     * abc.
     * @provides UsesTest Test description for provides.
      */
    void provides_with_description() { }
/*
DocComment[DOC_COMMENT, pos:1
  firstSentence: 1
    Text[TEXT, pos:1, abc.]
  body: empty
  block tags: 1
    Provides[PROVIDES, pos:7
      serviceName:
        Reference[REFERENCE, pos:17, UsesTest]
      description: 1
        Text[TEXT, pos:26, Test_description_for_provides.]
    ]
]
*/
}
