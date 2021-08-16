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

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Access a jrt:/ resource in an observable module that is not in the boot
 * layer and hence not known to the built-in class loaders.
 */

public class OtherResources {
    public static void main(String[] args) throws IOException {

        // check that java.desktop is not in the set of readable modules
        try {
            Class.forName("java.awt.Component");
            throw new RuntimeException("Need to run with --limit-modules java.base");
        } catch (ClassNotFoundException expected) { }

        // access resource in the java.desktop module
        URL url = new URL("jrt:/java.desktop/java/awt/Component.class");
        URLConnection uc = url.openConnection();
        System.out.println(uc.getInputStream());
    }
}
