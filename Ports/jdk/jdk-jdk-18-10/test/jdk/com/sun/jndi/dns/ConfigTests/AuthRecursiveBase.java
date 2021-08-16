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

import javax.naming.NamingException;
import javax.naming.directory.Attributes;

/**
 * Abstract test base for Config related tests, this class extends DNSTestBase.
 *
 * @see DNSTestBase
 * @see TestBase
 */
abstract class AuthRecursiveBase extends DNSTestBase {

    private static final String KEY = "host1";
    private static final String[] MANDATORY_ATTRIBUTES = { "A", "MX", "HINFO",
            "TXT", "29" };
    private static final String[] OPTIONAL_ATTRIBUTES = {};

    private String fqdnUrl;
    private String foreignFqdnUrl;

    /**
     * Setup test before real test run, it overrides the method of TestBase.
     */
    @Override
    public void setupTest() {
        super.setupTest();
        String fqdn = DNSTestUtils.buildFqdn(KEY, env(), true);

        String foreignLeaf = (String) env().get("FOREIGN_LEAF");
        String foreignFqdn = DNSTestUtils.buildFqdn(foreignLeaf, env(), false);

        fqdnUrl = DNSTestUtils.getRootUrl(env()) + "/" + fqdn;
        foreignFqdnUrl = DNSTestUtils.getRootUrl(env()) + "/" + foreignFqdn;
    }

    /**
     * Overload method of retrieveAndVerifyData, it will retrieve all of the
     * attributes associated with given named object and do verification.
     *
     * @param name given named object
     * @throws NamingException if a naming exception is encountered
     */
    public void retrieveAndVerifyData(String name) throws NamingException {
        Attributes retAttrs = context().getAttributes(name);
        DNSTestUtils.verifySchema(retAttrs, MANDATORY_ATTRIBUTES,
                OPTIONAL_ATTRIBUTES);
    }

    /**
     * Retrieves selected attributes associated with a named object and do
     * verification.
     *
     * @param name    given named object
     * @param attrIds given ids of the attributes to retrieve
     * @throws NamingException if a naming exception is encountered
     */
    public void retrieveAndVerifyData(String name, String[] attrIds)
            throws NamingException {
        Attributes retAttrs = context().getAttributes(name, attrIds);
        DNSTestUtils.verifySchema(retAttrs, MANDATORY_ATTRIBUTES,
                OPTIONAL_ATTRIBUTES);
    }

    /**
     * Return parsed flag from given test name.
     *
     * @return parsed flag from given test name
     */
    public String parseFlagFromTestName() {
        String name = (String) env().get("testname");
        if (name == null || name.isEmpty()) {
            throw new RuntimeException("test name expecting not null/empty");
        }

        if (name.endsWith("Default")) {
            return "default";
        } else if (name.endsWith("True")) {
            return "true";
        } else if (name.endsWith("False")) {
            return "false";
        } else {
            throw new RuntimeException("Invalid test name " + name);
        }
    }

    public String getFqdnUrl() {
        return fqdnUrl;
    }

    public String getForeignFqdnUrl() {
        return foreignFqdnUrl;
    }
}
