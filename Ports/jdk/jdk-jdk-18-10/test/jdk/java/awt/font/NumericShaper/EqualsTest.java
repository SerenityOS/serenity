/*
 * Copyright (c) 2009, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6842557
 * @summary confirm that an instance which is created with new Enum ranges is
 * equivalent to another instance which is created with equivalent traditional
 * ranges or the same Enum ranges.
 */

import java.awt.font.NumericShaper;
import java.util.EnumSet;
import static java.awt.font.NumericShaper.*;

public class EqualsTest {
    public static void main(String[] args) {
        NumericShaper ns1 = getContextualShaper(ARABIC | TAMIL, TAMIL);
        NumericShaper ns2 = getContextualShaper(
                                EnumSet.of(Range.ARABIC, Range.TAMIL),
                                Range.TAMIL);
        NumericShaper ns3 = getContextualShaper(
                                EnumSet.of(Range.ARABIC, Range.TAMIL),
                                Range.TAMIL);
        NumericShaper ns4 = getContextualShaper(
                                EnumSet.of(Range.ARABIC, Range.TAMIL),
                                Range.ARABIC);

        if (!ns1.equals(ns2)) {
            throw new RuntimeException("ns1 != ns2: ns1=" + ns1 + ", ns2=" + ns2);
        }
        if (!ns2.equals(ns1)) {
            throw new RuntimeException("ns2 != ns1: ns1=" + ns1 + ", ns2=" + ns2);
        }
        if (!ns2.equals(ns3)) {
            throw new RuntimeException("ns2 != ns3: ns2=" + ns2 + ", ns3=" + ns3);
        }
        if (ns1.equals(ns4)) {
            throw new RuntimeException("ns1 == ns4: ns1=" + ns1 + ", ns4=" + ns4);
        }
        if (ns2.equals(ns4)) {
            throw new RuntimeException("ns2 == ns4: ns2=" + ns2 + ", ns4=" + ns4);
        }
    }
}
