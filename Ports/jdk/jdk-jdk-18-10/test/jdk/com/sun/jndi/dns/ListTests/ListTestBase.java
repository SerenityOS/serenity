/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Hashtable;

/**
 * Abstract test base for List related Tests, this class extends DNSTestBase.
 *
 * This test base will also been referenced outside of current open test folder,
 * please double check all usages when modify this.
 *
 * @see DNSTestBase
 * @see TestBase
 */
public abstract class ListTestBase extends DNSTestBase {
    private String key;
    private String[] children;

    public ListTestBase() {
        // set default test data
        setKey("subdomain");
        setChildren("host1", "host2", "host3", "host4", "host5", "host6",
                "host7", "host8", "host9");
    }

    /**
     * Verify given entries, will throw RuntimeException if any child missing.
     *
     * @param entries given entries
     */
    public void verifyEntries(Hashtable<?, ?> entries) {
        if (entries.size() != children.length) {
            throw new RuntimeException(
                    "Expected " + children.length + " entries but found "
                            + entries.size());
        } else {
            for (String child : children) {
                if (entries.get(child.toLowerCase()) == null) {
                    throw new RuntimeException("Missing " + child);
                }
            }
        }
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setChildren(String... children) {
        this.children = children;
    }
}
