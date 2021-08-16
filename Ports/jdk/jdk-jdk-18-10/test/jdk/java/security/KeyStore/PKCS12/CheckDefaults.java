/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
import java.security.KeyStore;
import java.security.Security;
import static java.lang.System.out;

/**
 * @test
 * @bug 8048830
 * @summary Set up keystore.type secure property and check that
 * KeyStore.getDefaultType() value is related to property value. Expect a full
 * match the value 'keystore.type' and the value of the
 * KeyStore.getDefaultType()
 * @run  main/othervm CheckDefaults
 */
public class CheckDefaults {
    private static final String DEFAULT_KEY_STORE_TYPE = "pkcs12";
    private static final String[] KEY_STORE_TYPES = {"jks", "pkcs12", "jceks",
        "Unregistered_type_of_KS"};

    private void runTest(String[] args) {
        if (!KeyStore.getDefaultType().
                equalsIgnoreCase(DEFAULT_KEY_STORE_TYPE)) {
            throw new RuntimeException(String.format("Default keystore type "
                    + "Expected '%s' . Actual: '%s' ", DEFAULT_KEY_STORE_TYPE,
                    KeyStore.getDefaultType()));
        }
        for (String ksDefaultType : KEY_STORE_TYPES) {
            Security.setProperty("keystore.type", ksDefaultType);
            if (!KeyStore.getDefaultType().equals(ksDefaultType)) {
                throw new RuntimeException(String.format(
                        "Keystore default type value: '%s' cannot be set up via"
                        + " keystore.type "
                        + "security property, Actual: '%s'",
                        ksDefaultType, KeyStore.getDefaultType()));
            }
        }
        out.println("Test Passed");
    }

    public static void main(String[] args) {
        CheckDefaults checkDefaultsTest = new CheckDefaults();
        checkDefaultsTest.runTest(args);
    }
}
