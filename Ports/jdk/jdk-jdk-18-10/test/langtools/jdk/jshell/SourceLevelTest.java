/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8259820
 * @summary Check JShell can handle -source 8
 * @modules jdk.jshell
 * @run testng SourceLevelTest
 */

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class SourceLevelTest extends ReplToolTesting {

    @DataProvider(name="sourceLevels")
    public Object[][] sourceLevels() {
        return new Object[][] {
            new Object[] {"8"},
            new Object[] {"11"}
        };
    }

    @Test(dataProvider="sourceLevels")
    public void testSourceLevel(String sourceLevel) {
        test(new String[] {"-C", "-source", "-C", sourceLevel},
                (a) -> assertCommand(a, "1 + 1", "$1 ==> 2"),
                (a) -> assertCommand(a, "1 + 2", "$2 ==> 3")
        );
    }

}
