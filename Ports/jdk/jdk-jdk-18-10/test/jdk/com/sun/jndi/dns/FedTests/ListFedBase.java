/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

import javax.naming.Binding;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

/**
 * Abstract test base for Fed list related test, this class extends DNSTestBase.
 *
 * @see DNSTestBase
 * @see TestBase
 */
abstract class ListFedBase extends DNSTestBase {
    private String key;

    public ListFedBase() {
        // set default test data
        setKey("host1");
    }

    /**
     * Verify given NamingEnumeration match expected count.
     *
     * @param enumObj       given NamingEnumeration instance
     * @param expectedCount given expected count
     * @throws NamingException
     */
    public void verifyNamingEnumeration(NamingEnumeration<Binding> enumObj,
            int expectedCount) throws NamingException {
        DNSTestUtils.debug("Enum is: " + enumObj);

        int count = 0;
        Binding res;

        while (enumObj.hasMore()) {
            res = enumObj.next();
            DNSTestUtils.debug(res);
            ++count;
        }

        if (count != expectedCount) {
            throw new RuntimeException(
                    "Expecting " + expectedCount + " entries but got " + count);
        }
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
