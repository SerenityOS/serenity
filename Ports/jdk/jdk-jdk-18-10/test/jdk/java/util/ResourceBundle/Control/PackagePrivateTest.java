/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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
/*
 * @test
 * @bug 4175293 5102289
 * @summary Test if package private ResourceBundles can be loaded.
 * @build PackagePrivateRB
 * @run main/timeout=300/othervm -esa PackagePrivateTest
 */

import java.io.*;
import java.util.*;
import static java.util.ResourceBundle.Control.*;

public class PackagePrivateTest {
    public static void main(String[] args) {
        ResourceBundle rb;

        // Make sure that the default Control can't load the package
        // private resource bundles.
        try {
            rb = ResourceBundle.getBundle("PackagePrivateRB");
            throw new RuntimeException(
                       "doesn't throw MissingResourceException with the default Control");
        } catch (MissingResourceException e) {
        }

        // Remove the dummy cache entry
        ResourceBundle.clearCache();

        rb = ResourceBundle.getBundle("PackagePrivateRB",
                new ResourceBundle.Control() {
                    @Override
                    public List<String> getFormats(String baseName) {
                        return FORMAT_CLASS;
                    }

                    @Override
                    public ResourceBundle newBundle(String baseName,
                                                    Locale locale,
                                                    String format,
                                                    ClassLoader loader,
                                                    boolean reload)
                        throws IllegalAccessException,
                               InstantiationException, IOException {
                        String bn = toBundleName(baseName, locale);
                        if ("java.class".equals(format)) {
                            try {
                                Class<? extends ResourceBundle> cl =
                                    (Class<? extends ResourceBundle>) loader.loadClass(bn);
                                return  cl.newInstance();
                            } catch (ClassNotFoundException e) {
                                //System.out.println("ClassNotFoundException: " + e.getMessage());
                            }
                            return null;
                        }
                        throw new IllegalArgumentException("unknown format: " + format);
                    }
                });
        String s = rb.getString("type");
        if (!s.equals("class (package1.PackagePrivateRB)")) {
            throw new RuntimeException("wrong type: got " + s + ", expected '" +
                                       "class (package1.PackagePrivateRB)'");
        }
    }
}
