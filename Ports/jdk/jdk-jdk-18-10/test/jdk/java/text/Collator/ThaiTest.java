/*
 * Copyright (c) 1997, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @library /java/text/testlib
 * @summary test Thai Collation
 * @modules jdk.localedata
 */
/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
 *
 * This software is the proprietary information of Oracle.
 * Use is subject to license terms.
 *
 */

import java.util.Locale;
import java.text.Collator;
import java.text.RuleBasedCollator;

public class ThaiTest extends CollatorTest {

    public static void main(String[] args) throws Exception {
        new ThaiTest().run(args);
    }

    /*
     * Data for TestPrimary()
     */
    private static final String[] primarySourceData = {
        "\u0e01\u0e01",
        "\u0e07\u0e42\u0e01\u0e49",
        "\u0e10\u0e34\u0e19",
        "\u0e16\u0e32\u0e21\u0e23\u0e23\u0e04\u0e40\u0e17\u0e28\u0e19\u0e32",
        "\u0e23\u0e21\u0e18\u0e23\u0e23\u0e21\u0e4c\u0e1b\u0e23\u0e30\u0e01\u0e31\u0e19\u0e20\u0e31\u0e22",
        "\u0e23\u0e23\u0e21\u0e2a\u0e34\u0e17\u0e18\u0e34\u0e4c\u0e40\u0e04\u0e23\u0e37\u0e48\u0e2d\u0e07\u0e2b\u0e21\u0e32\u0e22\u0e41\u0e25\u0e30\u0e22\u0e35\u0e48\u0e2b\u0e49\u0e2d\u0e01\u0e32\u0e23\u0e04\u0e49\u0e32\u0e02\u0e32\u0e22",
        "\u0e23\u0e23\u0e25\u0e38\u0e19\u0e34\u0e15\u0e34\u0e20\u0e32\u0e27\u0e30",
        "\u0e01\u0e25\u0e37\u0e2d\u0e41\u0e01\u0e07",
        "\u0e21\u0e17\u0e34\u0e25\u0e41\u0e2d\u0e25\u0e01\u0e2d\u0e2e\u0e2d\u0e25\u0e4c",
        "\u0e40\u0e2d\u0e35\u0e48\u0e22\u0e21\u0e2d\u0e48\u0e2d\u0e07",
        "\u0e2a\u0e14\u0e32\u0e1b\u0e31\u0e15\u0e15\u0e34\u0e1c\u0e25",
        "\u0e2d\u0e19\u0e01\u0e23\u0e23\u0e21\u0e2a\u0e34\u0e17\u0e18\u0e34\u0e4c",
        "\u0e21\u0e49\u0e40\u0e17\u0e49\u0e32\u0e22\u0e32\u0e22\u0e21\u0e48\u0e2d\u0e21",
        "\u0e2a\u0e49\u0e25\u0e30\u0e21\u0e32\u0e19",
        "\u0e2a\u0e49\u0e28\u0e36\u0e01",
        "\u0e2a\u0e49\u0e2d\u0e31\u0e48\u0e27",
        "\u0e2a\u0e49\u0e40\u0e14\u0e37\u0e2d\u0e19",
        "\u0e2a\u0e49\u0e40\u0e25\u0e37\u0e48\u0e2d\u0e19",
        "\u0e2a\u0e49\u0e41\u0e02\u0e27\u0e19",
        "\u0e2a\u0e49\u0e41\u0e2b\u0e49\u0e07",
        "\u0e2a\u0e49\u0e44\u0e01\u0e48",
        "\u0e2b",
        "\u0e2b\u0e0b\u0e2d\u0e07",
        "\u0e2b\u0e19",
        "\u0e2b\u0e1b\u0e25\u0e32\u0e23\u0e49\u0e32",
        "\u0e2b\u0e21",
        "\u0e2b\u0e21\u0e17\u0e2d\u0e07",
        "\u0e2b\u0e21\u0e2a\u0e31\u0e1a\u0e1b\u0e30\u0e23\u0e14",
        "\u0e2b\u0e21\u0e49",
        "\u0e2b\u0e23\u0e13\u0e22\u0e4c",
        "\u0e2b\u0e25",
        "\u0e2b\u0e25\u0e19\u0e49\u0e33",
        "\u0e2b\u0e25\u0e48",
        "\u0e2b\u0e25\u0e48\u0e16\u0e19\u0e19",
        "\u0e2b\u0e25\u0e48\u0e17\u0e27\u0e35\u0e1b",
        "\u0e2b\u0e25\u0e48\u0e17\u0e32\u0e07",
        "\u0e2b\u0e25\u0e48\u0e23\u0e27\u0e1a",
        "\u0e2b\u0e49",
        "\u0e2d",
        "\u0e2d\u0e49",
        "\u0e2e\u0e42\u0e25",
        "\u0e2e\u0e44\u0e1f",
        "\u0e2e\u0e49"
    };

    private static final String[] primaryTargetData = {
        "\u0e01\u0e01",
        "\u0e07\u0e42\u0e01\u0e49",
        "\u0e10\u0e34\u0e19",
        "\u0e16\u0e32\u0e21\u0e23\u0e23\u0e04\u0e40\u0e17\u0e28\u0e19\u0e32",
        "\u0e23\u0e21\u0e18\u0e23\u0e23\u0e21\u0e4c\u0e1b\u0e23\u0e30\u0e01\u0e31\u0e19\u0e20\u0e31\u0e22",
        "\u0e23\u0e23\u0e21\u0e2a\u0e34\u0e17\u0e18\u0e34\u0e4c\u0e40\u0e04\u0e23\u0e37\u0e48\u0e2d\u0e07\u0e2b\u0e21\u0e32\u0e22\u0e41\u0e25\u0e30\u0e22\u0e35\u0e48\u0e2b\u0e49\u0e2d\u0e01\u0e32\u0e23\u0e04\u0e49\u0e32\u0e02\u0e32\u0e22",
        "\u0e23\u0e23\u0e25\u0e38\u0e19\u0e34\u0e15\u0e34\u0e20\u0e32\u0e27\u0e30",
        "\u0e01\u0e25\u0e37\u0e2d\u0e41\u0e01\u0e07",
        "\u0e21\u0e17\u0e34\u0e25\u0e41\u0e2d\u0e25\u0e01\u0e2d\u0e2e\u0e2d\u0e25\u0e4c",
        "\u0e40\u0e2d\u0e35\u0e48\u0e22\u0e21\u0e2d\u0e48\u0e2d\u0e07",
        "\u0e2a\u0e14\u0e32\u0e1b\u0e31\u0e15\u0e15\u0e34\u0e1c\u0e25",
        "\u0e2d\u0e19\u0e01\u0e23\u0e23\u0e21\u0e2a\u0e34\u0e17\u0e18\u0e34\u0e4c",
        "\u0e21\u0e49\u0e40\u0e17\u0e49\u0e32\u0e22\u0e32\u0e22\u0e21\u0e48\u0e2d\u0e21",
        "\u0e2a\u0e49\u0e25\u0e30\u0e21\u0e32\u0e19",
        "\u0e2a\u0e49\u0e28\u0e36\u0e01",
        "\u0e2a\u0e49\u0e2d\u0e31\u0e48\u0e27",
        "\u0e2a\u0e49\u0e40\u0e14\u0e37\u0e2d\u0e19",
        "\u0e2a\u0e49\u0e40\u0e25\u0e37\u0e48\u0e2d\u0e19",
        "\u0e2a\u0e49\u0e41\u0e02\u0e27\u0e19",
        "\u0e2a\u0e49\u0e41\u0e2b\u0e49\u0e07",
        "\u0e2a\u0e49\u0e44\u0e01\u0e48",
        "\u0e2b",
        "\u0e2b\u0e0b\u0e2d\u0e07",
        "\u0e2b\u0e19",
        "\u0e2b\u0e1b\u0e25\u0e32\u0e23\u0e49\u0e32",
        "\u0e2b\u0e21",
        "\u0e2b\u0e21\u0e17\u0e2d\u0e07",
        "\u0e2b\u0e21\u0e2a\u0e31\u0e1a\u0e1b\u0e30\u0e23\u0e14",
        "\u0e2b\u0e21\u0e49",
        "\u0e2b\u0e23\u0e13\u0e22\u0e4c",
        "\u0e2b\u0e25",
        "\u0e2b\u0e25\u0e19\u0e49\u0e33",
        "\u0e2b\u0e25\u0e48",
        "\u0e2b\u0e25\u0e48\u0e16\u0e19\u0e19",
        "\u0e2b\u0e25\u0e48\u0e17\u0e27\u0e35\u0e1b",
        "\u0e2b\u0e25\u0e48\u0e17\u0e32\u0e07",
        "\u0e2b\u0e25\u0e48\u0e23\u0e27\u0e1a",
        "\u0e2b\u0e49",
        "\u0e2d",
        "\u0e2d\u0e49",
        "\u0e2e\u0e42\u0e25",
        "\u0e2e\u0e44\u0e1f",
        "\u0e2e\u0e49"
    };

    private static final int[] primaryResults = {
         0,  0,  0,  0,  0,  0,  0,  0,  0,  0,
         0,  0,  0,  0,  0,  0,  0,  0,  0,  0,
         0,  0,  0,  0,  0,  0,  0,  0,  0,  0,
         0,  0,  0,  0,  0,  0,  0,  0,  0,  0,
         0,  0,  0
    };

    public void TestPrimary() {
        doTest(myCollation, Collator.PRIMARY,
               primarySourceData, primaryTargetData, primaryResults);
    }

    private final Collator myCollation = Collator.getInstance(new Locale("th"));
}
