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

/**
 * Abstract test base for Factory related Tests, this class extends DNSTestBase.
 *
 * This test base will also been referenced outside of current open test folder,
 * please double check all usages when modify this.
 *
 * @see DNSTestBase
 * @see TestBase
 */
public abstract class LookupFactoryBase extends DNSTestBase {
    // pre defined attribute value for 'A'
    public static final String ATTRIBUTE_VALUE = "1.2.4.1";

    public static final String DNS_LOOKUP_ATTR = "com.sun.jndi.dns.lookup.attr";

    private String key;

    public LookupFactoryBase() {
        // set default test data
        setKey("host1");
    }

    /**
     * Overload method of verifyLookupObject, specify class type TestDnsObject.
     *
     * @param obj given lookup returned object
     */
    public void verifyLookupObject(Object obj) {
        verifyLookupObject(obj, TestDnsObject.class);
    }

    /**
     * Verify given lookup returned object whether match class type, will throw
     * RuntimeException if verify failed.
     *
     * @param obj       given lookup returned object
     * @param classType given class type
     * @param <T>       given type
     */
    public <T> void verifyLookupObject(Object obj, Class<T> classType) {
        DNSTestUtils.debug("Object is: " + obj);
        if (!classType.isInstance(obj)) {
            throw new RuntimeException(
                    "Expected " + classType + ", but found " + obj.getClass());
        }
    }

    /**
     * Verify given lookup returned object match class type TestDnsObject, then
     * verify the object toString() value match with given value, will throw
     * RuntimeException if any verification step failed.
     *
     * @param obj   given lookup returned object
     * @param value given expected value
     */
    public void verifyLookupObjectAndValue(Object obj, String value) {
        verifyLookupObject(obj);
        if (!obj.toString().equals(value)) {
            throw new RuntimeException(
                    "Expected " + value + ", but found " + obj.toString());
        }
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
