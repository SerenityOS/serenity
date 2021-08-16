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

import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;

public abstract class GetAttrsBase extends DNSTestBase {
    private String key;
    private String[] mandatoryAttrs;

    public GetAttrsBase() {
        // set default test data
        setKey("host1");
        setMandatoryAttrs("A", "MX", "HINFO", "TXT", "29");
    }

    private static final String[] OPTIONAL_ATTRIBUTES = {};

    public void initContext() throws Exception {
        setContext(new InitialDirContext(env()));
    }

    public abstract Attributes getAttributes() throws Exception;

    public void verifyAttributes(Attributes retAttrs) {
        if (retAttrs != null) {
            DNSTestUtils.verifySchema(retAttrs, mandatoryAttrs,
                    OPTIONAL_ATTRIBUTES);
        } else {
            throw new RuntimeException("Attributes to be verified is null");
        }
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String[] getMandatoryAttrs() {
        return mandatoryAttrs;
    }

    public void setMandatoryAttrs(String... mandatoryAttrs) {
        this.mandatoryAttrs = mandatoryAttrs;
    }
}
