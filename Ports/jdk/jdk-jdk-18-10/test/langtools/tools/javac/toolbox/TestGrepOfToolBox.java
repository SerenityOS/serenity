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
 * @bug 8241187
 * @summary ToolBox::grep should allow for negative filtering
 * @library /tools/lib
 * @build toolbox.ToolBox
 * @run main TestGrepOfToolBox
 */

import java.util.Arrays;
import java.util.List;

import toolbox.ToolBox;

public class TestGrepOfToolBox {
    public static void main(String[] args) {
        ToolBox tb = new ToolBox();
        List<String> input = Arrays.asList("apple", "banana", "cat", "dog", "end", "ending");

        String regex1 = ".*ana.*";
        List<String> expected1 = Arrays.asList("apple", "cat", "dog", "end", "ending");
        List<String> output1 = tb.grep(regex1, input, false);
        tb.checkEqual(expected1, output1);

        String regex2 = ".*nd.*";
        List<String> expected2 = Arrays.asList("apple", "banana", "cat", "dog");
        List<String> output2 = tb.grep(regex2, input, false);
        tb.checkEqual(expected2, output2);

        String regex3 = "apple";
        List<String> expected3 = Arrays.asList("banana", "cat", "dog", "end", "ending");
        List<String> output3 = tb.grep(regex3, input, false);
        tb.checkEqual(expected3, output3);

        String regex4 = ".*ana.*";
        List<String> expected4 = Arrays.asList("banana");
        List<String> output4 = tb.grep(regex4, input, true);
        tb.checkEqual(expected4, output4);

        String regex5 = ".*nd.*";
        List<String> expected5 = Arrays.asList("end", "ending");
        List<String> output5 = tb.grep(regex5, input, true);
        tb.checkEqual(expected5, output5);

        String regex6 = "apple";
        List<String> expected6 = Arrays.asList("apple");
        List<String> output6 = tb.grep(regex6, input, true);
        tb.checkEqual(expected6, output6);
    }
}
