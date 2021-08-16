/*
 * Copyright (c) 2001, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4480308
 * @summary URL is not throwinng exception for wrong URL format.
 *
 */

import java.net.*;

public class ParseAuthority {
    public static void main(String args[]) throws Exception {
        try {
            URL u1 = new URL("http://[fe80::]9999/path1/path2/");
            throw new RuntimeException("URL parser didn't catch" +
                                       " invalid authority field");
        } catch (MalformedURLException me) {
            if (!me.getMessage().startsWith("Invalid authority field")) {
                throw new RuntimeException("URL parser didn't catch" +
                                       " invalid authority field");
            }
        }

        try {
            URL u2 = new URL("http://[www.sun.com]:9999/path1/path2/");
            throw new RuntimeException("URL parser didn't catch" +
                                       " invalid host");
        } catch (MalformedURLException me) {
            if (!me.getMessage().startsWith("Invalid host")) {
                throw new RuntimeException("URL parser didn't catch" +
                                       " invalid host");
            }
        }
    }
}
