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

/* @test
 * @bug 6529759
 * @summary URL constructor of specific form does not provide exception chaining
 */

import java.net.URL;

public class B6529759
{
    public static void main(String[] args) {
        try {
            new java.net.URL(null, "a:", new a());
        } catch (Exception e) {
            if (e.getCause() == null) {
                e.printStackTrace();
                throw new RuntimeException("Failed: Exception has no cause");
            }
        }
    }

    static class a extends java.net.URLStreamHandler {
        protected java.net.URLConnection openConnection(java.net.URL u)  {
            throw new UnsupportedOperationException();
        }

        protected void parseURL(java.net.URL u, String spec, int start, int limit) {
            throw new RuntimeException();
        }
    }
}
