/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

package java.lang.constant;

import java.lang.invoke.*;
import java.lang.constant.*;
import java.util.*;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * @test
 * @compile ConstantUtilsTest.java
 * @run testng ConstantUtilsTest
 * @summary unit tests for methods of java.lang.constant.ConstantUtils that are not covered by other unit tests
 */
@Test
public class ConstantUtilsTest {
    private static ClassDesc thisClass = ClassDesc.of("MethodHandleDescTest");

    public void testValidateMemberName() {
        try {
            ConstantUtils.validateMemberName(null, false);
            fail("");
        } catch (NullPointerException e) {
            // good
        }

        try {
            ConstantUtils.validateMemberName("", false);
            fail("");
        } catch (IllegalArgumentException e) {
            // good
        }

        List<String> badNames = List.of(".", ";", "[", "/", "<", ">");
        for (String n : badNames) {
            try {
                ConstantUtils.validateMemberName(n, true);
                fail(n);
            } catch (IllegalArgumentException e) {
                // good
            }
        }
    }
}
