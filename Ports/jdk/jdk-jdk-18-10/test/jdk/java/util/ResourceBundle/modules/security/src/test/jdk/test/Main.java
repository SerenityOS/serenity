/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package jdk.test;

import p1.Bundle;
import java.util.ResourceBundle;

public class Main {
    private static final String TEST_RESOURCE_BUNDLE_NAME
            = "jdk.test.resources.TestResources";
    private static final String M1_RESOURCE_BUNDLE_NAME
            = "p1.resources.MyResources";

    public static void main(String[] args) {
        // local resource
        ResourceBundle.getBundle(TEST_RESOURCE_BUNDLE_NAME, Main.class.getModule());

        // resource in another module
        Module m1 = p1.Bundle.class.getModule();

        // bundles loaded with different cache key
        ResourceBundle rb1 = Bundle.getBundle(M1_RESOURCE_BUNDLE_NAME);
        ResourceBundle rb2 = ResourceBundle.getBundle(M1_RESOURCE_BUNDLE_NAME, m1);
        if (rb1 == rb2) {
            throw new RuntimeException("unexpected resource bundle");
        }

        System.setSecurityManager(new SecurityManager());

        // no permission needed for local resource
        ResourceBundle.getBundle(TEST_RESOURCE_BUNDLE_NAME, Main.class.getModule());

        // resource bundle through m1's exported API
        Bundle.getBundle(M1_RESOURCE_BUNDLE_NAME);

        try {
            // fail to get resource bundle in another module
            ResourceBundle.getBundle(M1_RESOURCE_BUNDLE_NAME, m1);
            throw new RuntimeException("should deny access");
        } catch (SecurityException e) {}
    }

}
