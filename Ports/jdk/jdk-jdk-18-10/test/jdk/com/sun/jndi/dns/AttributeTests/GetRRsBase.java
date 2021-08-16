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

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;

public abstract class GetRRsBase extends DNSTestBase {
    public static final int ROOT_LIMIT = 5; // point at which to use root ctx
    private String[] keys;
    private String[][] numAttrs;
    private String[][] attrs;

    public GetRRsBase() {
        // set default test data
        setKeys(new String[] { "host1", "", "ipv6", "sun", "_ftp._Tcp",
                "1.4.2.1.in-addr.arpa", });
        setNumAttrs(new String[][] { { "IN 1", "IN 15", "IN 13", "IN 16" },
                { "IN 2", "IN 6" }, { "IN 28" }, { "IN 5" }, { "IN 33" },
                { "IN 12" }, });
        setAttrs(
                new String[][] { { "A", "MX", "HINFO", "TXT" }, { "NS", "SOA" },
                        { "AAAA" }, { "CNAME" }, { "SRV" }, { "PTR" }, });
    }

    public void initContext() throws Exception {
        setContext(new InitialDirContext(env()));
    }

    public void switchToRootUrl() throws NamingException {
        DNSTestUtils.cleanup(context());
        env().put(Context.PROVIDER_URL, DNSTestUtils.getRootUrl(env()));
        setContext(new InitialDirContext(env()));
    }

    public void verifyAttributes(Attributes attrs, String[] mandatory) {
        DNSTestUtils.verifySchema(attrs, mandatory, new String[] {});
    }

    public Attributes getAttributes(String name, String[] attrIds)
            throws Exception {
        return context().getAttributes(name, attrIds);
    }

    public void setKeys(String[] keys) {
        this.keys = keys;
    }

    public String[] getKeys() {
        return keys;
    }

    public void setNumAttrs(String[][] numAttrs) {
        this.numAttrs = numAttrs;
    }

    public String[][] getNumAttrs() {
        return numAttrs;
    }

    public void setAttrs(String[][] attrs) {
        this.attrs = attrs;
    }

    public String[][] getAttrs() {
        return attrs;
    }
}
