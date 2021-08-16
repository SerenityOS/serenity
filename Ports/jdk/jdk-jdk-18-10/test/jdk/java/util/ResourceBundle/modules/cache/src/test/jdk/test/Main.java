/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class Main {
    static final String MAIN_BUNDLES_RESOURCE = "jdk.test.resources.MyResources";
    public static void main(String[] args) {
        if (args.length == 1 && args[0].equals("cache")) {
            // first load resource bundle in the cache
            jdk.test.util.Bundles.getBundle();

            // fail to load resource bundle that is present in the cache
            try {
                Module mainbundles = jdk.test.util.Bundles.class.getModule();
                ResourceBundle rb = ResourceBundle.getBundle(MAIN_BUNDLES_RESOURCE, mainbundles);
                throw new RuntimeException("ERROR: test module loads " + rb);
            } catch (MissingResourceException e) {
                System.out.println("Expected: " + e.getMessage());
            }
        } else {
            // fail to load resource bundle; NON_EXISTENT_BUNDLE in the cache
            try {
                Module mainbundles = jdk.test.util.Bundles.class.getModule();
                ResourceBundle rb = ResourceBundle.getBundle(MAIN_BUNDLES_RESOURCE, mainbundles);
                throw new RuntimeException("ERROR: test module loads " + rb);
            } catch (MissingResourceException e) {
                System.out.println("Expected: " + e.getMessage());
            }

            // successfully load the resource bundle
            jdk.test.util.Bundles.getBundle();
        }
    }
}
