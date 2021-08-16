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

/**
 * @test
 * @bug 8262744
 * @summary BigDecimal does not always display formatting correctly because
 * rounding is done after formatting check. Fix moves rounding to before the
 * range-based formatting check for %g formatting flag.
 * @run testng BigDecimalRounding
 */

import org.testng.annotations.Test;

import java.math.BigDecimal;

import static org.testng.Assert.*;

@Test
public class BigDecimalRounding {

    public static void testBigDecimalRounding() {
        var res1 = String.format("%g", 0.00009999999999999995);
        var res2 = String.format("%g", 0.00009999999f);
        var res3 = String.format("%g", new BigDecimal(0.0001));
        var res4 = String.format("%g", new BigDecimal("0.00009999999999999999995"));

        assertEquals(res1, res2);
        assertEquals(res2, res3);
        assertEquals(res3, res4);

        var res5 = String.format("%.9g", 999999.999999432168754e+3);
        var res6 = String.format("%.9g", 999999999.999432168754f);
        var res7 = String.format("%.9g", new BigDecimal("999999.999999432168754e+3")); // !!
        var res8 = String.format("%.9g", new BigDecimal("1000000000")); // !!

        assertEquals(res5, res6);
        assertEquals(res6, res7);
        assertEquals(res7, res8);

    }
}
