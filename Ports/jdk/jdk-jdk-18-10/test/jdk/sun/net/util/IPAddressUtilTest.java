/*
 * Copyright (c) 2004, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8087190
 * @summary Exercise the sun.net.util.IPAddressUtil class
 * @modules java.base/sun.net.util
 */

import sun.net.util.*;

/*
 * Unit test for sun.net.util.IPAddressUtil
 */
public class IPAddressUtilTest {
    final static Boolean good = Boolean.TRUE;
    final static Boolean bad = Boolean.FALSE;

    static Object [][] assertions = {
        {"224.0.1.0", good},
        {"238.255.255.255", good},
        {"239.255.255.255", good},
        {"238.255.255.2550", bad},
        {"256.255.255.255", bad},
        {"238.255.2550.255", bad},
        {"238.2550.255.255", bad},
        {"2380.255.255.255", bad},
        {".1.1.1", bad},
        {"1..1.1", bad},
        {"1.1.1.", bad},
        {"...", bad},
        {"10::10", good},
        {"10::10.1", bad},
        {"10::10.1.2", bad},
        {"10::10.1.2.3", good},
        {"10::10.1.2.3.4", bad},
        {"10::10.", bad},
        {"10::.10.", bad},
        {"10::.10", bad}
    };

    public static void main(String[] args) {
        for (int i=0; i<assertions.length; i++) {
            String addr = (String) assertions[i][0];
            boolean expected = ((Boolean) assertions[i][1]).booleanValue();

            boolean result = true;

            if (!IPAddressUtil.isIPv4LiteralAddress(addr) &&
                !IPAddressUtil.isIPv6LiteralAddress(addr)) {
                result = false;
            }

            if (result != expected) {
                throw new RuntimeException ("wrong result for " + addr);
            }
        }
    }
}
