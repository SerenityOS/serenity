/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @summary White-box test for Validator.ENTRYNAME_COMPARATOR ( currently just
 *          checks module descriptors ).
 */
package sun.tools.jar;

import java.util.List;
import static java.util.stream.Collectors.toList;
import static sun.tools.jar.Main.ENTRYNAME_COMPARATOR;

import org.testng.Assert;
import org.testng.annotations.Test;

public class ValidatorComparatorTest {

    @Test
    public void testModuleInfo() throws Throwable {
        List<String> list =
                List.of("module-info.class",
                        "META-INF/versions/9/module-info.class",
                        "META-INF/versions/10/module-info.class");
        List<String> sorted = list.stream()
                .sorted(ENTRYNAME_COMPARATOR)
                .collect(toList());
        List<String> expected = list;
        Assert.assertEquals(sorted, expected);


        list =  List.of("META-INF/versions/10/module-info.class",
                        "META-INF/versions/9/module-info.class",
                        "module-info.class");
        sorted = list.stream().sorted(ENTRYNAME_COMPARATOR).collect(toList());
        expected =
                List.of("module-info.class",
                        "META-INF/versions/9/module-info.class",
                        "META-INF/versions/10/module-info.class");
        Assert.assertEquals(sorted, expected);


        list =  List.of("META-INF/versions/1001/module-info.class",
                        "META-INF/versions/1000/module-info.class",
                        "META-INF/versions/999/module-info.class",
                        "META-INF/versions/101/module-info.class",
                        "META-INF/versions/100/module-info.class",
                        "META-INF/versions/99/module-info.class",
                        "META-INF/versions/31/module-info.class",
                        "META-INF/versions/30/module-info.class",
                        "META-INF/versions/29/module-info.class",
                        "META-INF/versions/21/module-info.class",
                        "META-INF/versions/20/module-info.class",
                        "META-INF/versions/13/module-info.class",
                        "META-INF/versions/12/module-info.class",
                        "META-INF/versions/11/module-info.class",
                        "META-INF/versions/10/module-info.class",
                        "META-INF/versions/9/module-info.class",
                        "module-info.class");
        sorted = list.stream().sorted(ENTRYNAME_COMPARATOR).collect(toList());
        expected =
                List.of("module-info.class",
                        "META-INF/versions/9/module-info.class",
                        "META-INF/versions/10/module-info.class",
                        "META-INF/versions/11/module-info.class",
                        "META-INF/versions/12/module-info.class",
                        "META-INF/versions/13/module-info.class",
                        "META-INF/versions/20/module-info.class",
                        "META-INF/versions/21/module-info.class",
                        "META-INF/versions/29/module-info.class",
                        "META-INF/versions/30/module-info.class",
                        "META-INF/versions/31/module-info.class",
                        "META-INF/versions/99/module-info.class",
                        "META-INF/versions/100/module-info.class",
                        "META-INF/versions/101/module-info.class",
                        "META-INF/versions/999/module-info.class",
                        "META-INF/versions/1000/module-info.class",
                        "META-INF/versions/1001/module-info.class");
        Assert.assertEquals(sorted, expected);
    }
}
