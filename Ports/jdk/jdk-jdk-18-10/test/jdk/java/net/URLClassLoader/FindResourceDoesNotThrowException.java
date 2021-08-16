/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8262277
 * @summary Test if URLClassLoader throws IllegalArgumentException when getting
 * or finding resources.
 */

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;

public class FindResourceDoesNotThrowException {
    public static void main(String[] args) throws Exception {
        // URL ends in slash, does not start with file: or jar:
        URL url = new URL("https://127.0.0.1/");
        // resource name is not a valid url component
        String resource = "c:/windows";
        try (URLClassLoader urlClassLoader = new URLClassLoader(new URL[]{url})) {
            if (urlClassLoader.findResource(resource) != null) {
                throw new RuntimeException("findResource should return null");
            }
            if (urlClassLoader.getResource(resource) != null) {
                throw new RuntimeException("getResource should return null");
            }
            if (urlClassLoader.findResources(resource).hasMoreElements()) {
                throw new RuntimeException("findResources should return an empty enumeration");
            }
            if (urlClassLoader.getResources(resource).hasMoreElements()) {
                throw new RuntimeException("getResources should return an empty enumeration");
            }
        }
    }
    private FindResourceDoesNotThrowException() {}
}
