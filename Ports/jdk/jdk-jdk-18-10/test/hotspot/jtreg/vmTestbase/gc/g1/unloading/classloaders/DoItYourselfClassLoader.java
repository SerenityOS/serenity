/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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
package gc.g1.unloading.classloaders;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.CodeSigner;
import java.security.CodeSource;

/**
 *
 * This is just a classloader that doesn't follow delegation pattern.
 *
 */
public class DoItYourselfClassLoader extends FinalizableClassloader {

    private static long counter = 0;

    /**
     * We force different classes to have different protection domains
     */
    public Class<?> defineClass(String name, byte[] bytes) {
        URL url;
        try {
            url = new URL("http://random.url.com/" + (counter++));
        } catch (MalformedURLException e) {
            throw new RuntimeException("This is impossible, but there is mistake in simple call to URL constructor", e);
        }
        return defineClass(name, bytes, 0, bytes.length, new CodeSource(url, new CodeSigner[] {}));
    }

}
