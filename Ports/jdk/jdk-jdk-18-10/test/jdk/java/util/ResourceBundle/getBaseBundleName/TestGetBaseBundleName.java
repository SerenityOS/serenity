/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

/**
 * @test
 * @bug 4814565 8027930
 * @summary tests ResourceBundle.getBaseBundleName();
 * @build TestGetBaseBundleName resources.ListBundle resources.ListBundle_fr
 * @run main TestGetBaseBundleName
 * @author danielfuchs
 */
public class TestGetBaseBundleName {

    static final String PROPERTY_BUNDLE_NAME = "resources/PropertyBundle";
    static final String LIST_BUNDLE_NAME = "resources.ListBundle";

    public static String getBaseName(ResourceBundle bundle) {
        return bundle == null ? null : bundle.getBaseBundleName();
    }

    public static void main(String... args) throws Exception {

        Locale defaultLocale = Locale.getDefault();
        System.out.println("Default locale is: " + defaultLocale);
        for (String baseName : new String[] {
                    PROPERTY_BUNDLE_NAME,
                    LIST_BUNDLE_NAME
        }) {
            try {
                Locale.setDefault(Locale.US);
                ResourceBundle bundle = ResourceBundle.getBundle(baseName);
                System.out.println(getBaseName(bundle));
                if (!Locale.ROOT.equals(bundle.getLocale())) {
                    throw new RuntimeException("Unexpected locale: "
                            + bundle.getLocale());
                }
                if (!baseName.equals(getBaseName(bundle))) {
                    throw new RuntimeException("Unexpected base name: "
                            + getBaseName(bundle));
                }

                Locale.setDefault(Locale.FRENCH);
                ResourceBundle bundle_fr = ResourceBundle.getBundle(baseName);
                if (!Locale.FRENCH.equals(bundle_fr.getLocale())) {
                    throw new RuntimeException("Unexpected locale: "
                            + bundle_fr.getLocale());
                }
                if (!baseName.equals(getBaseName(bundle_fr))) {
                    throw new RuntimeException("Unexpected base name: "
                            + getBaseName(bundle_fr));
                }
            } finally {
                Locale.setDefault(defaultLocale);
            }
        }

        final ResourceBundle bundle = new ResourceBundle() {
            @Override
            protected Object handleGetObject(String key) {
                if ("dummy".equals(key)) return "foo";
                throw new MissingResourceException("Missing key",
                        this.getClass().getName(), key);
            }
            @Override
            public Enumeration<String> getKeys() {
                return Collections.enumeration(java.util.Arrays.asList(
                        new String[] {"dummy"}));
            }
        };

        if (getBaseName(bundle) != null) {
            throw new RuntimeException("Expected null baseName, got "
                    + getBaseName(bundle));
        }

        final ResourceBundle bundle2 = new ResourceBundle() {
            @Override
            protected Object handleGetObject(String key) {
                if ("dummy".equals(key)) return "foo";
                throw new MissingResourceException("Missing key",
                        this.getClass().getName(), key);
            }
            @Override
            public Enumeration<String> getKeys() {
                return Collections.enumeration(java.util.Arrays.asList(
                        new String[] {"dummy"}));
            }

            @Override
            public String getBaseBundleName() {
                return this.getClass().getName();
            }


        };

        if (!bundle2.getClass().getName().equals(getBaseName(bundle2))) {
            throw new RuntimeException("Expected "
                    + bundle2.getClass().getName() + ", got "
                    + getBaseName(bundle2));
        }

        ResourceBundle propertyBundle = new PropertyResourceBundle(
                TestGetBaseBundleName.class.getResourceAsStream(
                    PROPERTY_BUNDLE_NAME+".properties"));

        if (getBaseName(propertyBundle) != null) {
            throw new RuntimeException("Expected null baseName, got "
                    + getBaseName(propertyBundle));
        }

        ResourceBundle listBundle = new resources.ListBundle_fr();
        if (getBaseName(listBundle) != null) {
            throw new RuntimeException("Expected null baseName, got "
                    + getBaseName(listBundle));
        }


    }
}
