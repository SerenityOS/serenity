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
import java.util.Hashtable;

/**
 * Abstract test base for most of Env Tests, this class extends DNSTestBase.
 *
 * @see DNSTestBase
 * @see TestBase
 */
abstract class EnvTestBase extends DNSTestBase {
    private static final String[] MANDATORY_ATTRIBUTES = { "A", "MX", "HINFO",
            "TXT", "29" };
    private static final String[] OPTIONAL_ATTRIBUTES = {};

    private String key;
    private String fqdnUrl;
    private String foreignFqdnUrl;

    public EnvTestBase() {
        // set default test data
        setKey("host1");
    }

    /**
     * Setup test before real test run, it overrides the method of TestBase.
     */
    @Override public void setupTest() {
        super.setupTest();
        String fqdn = DNSTestUtils.buildFqdn(key, env(), true);

        String foreignLeaf = (String) env().get("FOREIGN_LEAF");
        String foreignFqdn = DNSTestUtils.buildFqdn(foreignLeaf, env(), false);

        fqdnUrl = DNSTestUtils.getRootUrl(env()) + "/" + fqdn;
        foreignFqdnUrl = DNSTestUtils.getRootUrl(env()) + "/" + foreignFqdn;
    }

    /**
     * Overload method of addToEnvAndVerifyOldValIsNull, use context() as
     * context.
     *
     * @param propName given property name
     * @param propVal  given property value
     * @throws NamingException if a naming exception is encountered
     */
    public void addToEnvAndVerifyOldValIsNull(String propName, Object propVal)
            throws NamingException {
        addToEnvAndVerifyOldValIsNull(context(), propName, propVal);
    }

    /**
     * Add given property name/value to the environment of given context and
     * verify the previous old property value is null which means the property
     * was not in the environment before.
     *
     * @param context  given context
     * @param propName given property name
     * @param propVal  given property value
     * @throws NamingException if a naming exception is encountered
     */
    public void addToEnvAndVerifyOldValIsNull(Context context, String propName,
            Object propVal) throws NamingException {
        Object oldValue = context.addToEnvironment(propName, propVal);
        DNSTestUtils.debug("Old Value for " + propName + " : " + oldValue);
        if (oldValue != null) {
            throw new RuntimeException(
                    "Failed: old value expected to be null for " + propName
                            + " but actual is : " + oldValue);
        }
    }

    /**
     * Verify the value of specified property in given environment matched with
     * given expected value. If property not exist and given expected value is
     * null, we think verify passed. RuntimeException will be thrown if verify
     * failed.
     *
     * @param env         given environment
     * @param propName    given property name to verify
     * @param expectedVal expected property value
     */
    public void verifyEnvProperty(Hashtable<?, ?> env, String propName,
            Object expectedVal) {
        boolean equals = true;
        Object actualVal = env.get(propName);
        if (actualVal != null && expectedVal != null) {
            if (!expectedVal.equals(actualVal)) {
                equals = false;
            }
        } else {
            if (actualVal != null || expectedVal != null) {
                equals = false;
            }
        }

        if (!equals) {
            throw new RuntimeException(
                    "Failed: value not match for " + propName + " expected: "
                            + expectedVal + " actual: " + actualVal);
        }
    }

    /**
     * Retrieve attributes by given name and attributes ids and verify
     * attributes contains the mandatory attributes and the right
     * objectclass attribute, will throw RuntimeException if verify failed.
     *
     * @param name    given name
     * @param attrIds given attribute ids
     * @throws NamingException if a naming exception is encountered
     */
    public void retrieveAndVerifyData(String name, String[] attrIds)
            throws NamingException {
        Attributes retAttrs = context().getAttributes(name, attrIds);
        DNSTestUtils.verifySchema(retAttrs, MANDATORY_ATTRIBUTES,
                OPTIONAL_ATTRIBUTES);
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getFqdnUrl() {
        return fqdnUrl;
    }

    public String getForeignFqdnUrl() {
        return foreignFqdnUrl;
    }
}
