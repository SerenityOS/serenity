/*
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8198882
 * @summary Tests that we can get an attribute that has a nonstandard name from
 *          a DNS entry.
 * @modules java.base/sun.security.util
 * @library ../lib/
 * @run main GetNonstandard
 */

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import java.util.Arrays;

public class GetNonstandard extends GetAttrsBase {
    // "40 2 0.373 N 105 17 23.528 W 1638m"
    private static final byte[] EXPECTED_VALUE = { (byte) 0,    // version
            (byte) 18,    // size
            (byte) 22,    // horiz pre
            (byte) 19,    // vert pre
            (byte) -120,    // latitude 1
            (byte) -105,    // latitude 2
            (byte) 26,    // longitude 1
            (byte) 53,    // longitude 2
            (byte) 105,    // altitude 1
            (byte) 104,    // altitude 2
            (byte) 65, (byte) 56, (byte) 0, (byte) -101, (byte) 22,
            (byte) 88, };

    public GetNonstandard() {
        // set new test data instead of default value
        setMandatoryAttrs("29");
    }

    public static void main(String[] args) throws Exception {
        new GetNonstandard().run(args);
    }

    @Override public void runTest() throws Exception {
        initContext();
        Attributes retAttrs = getAttributes();
        verifyAttributes(retAttrs);
        verifyLoc(retAttrs);
    }

    /*
     * Tests that we can get an attribute that has a nonstandard name from
     * a DNS entry.
     */
    @Override public Attributes getAttributes() throws Exception {
        return context().getAttributes(getKey(), getMandatoryAttrs());
    }

    private void verifyLoc(Attributes retAttrs) throws NamingException {
        Attribute loc = retAttrs.get(getMandatoryAttrs()[0]);
        byte[] val = (byte[]) loc.get(0);

        String expected = Arrays.toString(EXPECTED_VALUE);
        String actual = Arrays.toString(val);
        DNSTestUtils.debug("Expected: " + expected);
        DNSTestUtils.debug("Actual:   " + actual);

        if (!Arrays.equals(val, EXPECTED_VALUE)) {
            throw new RuntimeException(String.format(
                    "Failed: values not match, expected: %s, actual: %s",
                    expected, actual));
        }
    }
}
