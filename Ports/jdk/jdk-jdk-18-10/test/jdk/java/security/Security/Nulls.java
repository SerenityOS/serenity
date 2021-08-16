/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 4960208
 * @summary verify behavior passing null to various java.security.Security methods
 * @author Andreas Sterbenz
 */

import java.util.*;

import java.security.*;

public class Nulls {

    public static void main(String[] args) throws Exception {
        try {
            Security.addProvider(null);
            throw new Exception();
        } catch (NullPointerException e) {
            System.out.println("addProvider(null): " + e);
        }
        if (Security.getAlgorithms(null).isEmpty() == false) {
            throw new Exception();
        }
        try {
            Security.getProperty(null);
            throw new Exception();
        } catch (NullPointerException e) {
            System.out.println("getProperty(null): " + e);
        }
        if (Security.getProvider(null) != null) {
            throw new Exception();
        }
        try {
            Security.getProviders((Map)null);
            throw new Exception();
        } catch (NullPointerException e) {
            System.out.println("getProviders((Map)null): " + e);
        }
        try {
            Security.getProviders((String)null);
            throw new Exception();
        } catch (NullPointerException e) {
            System.out.println("getProviders((String)null): " + e);
        }
        try {
            Security.insertProviderAt(null, 1);
            throw new Exception();
        } catch (NullPointerException e) {
            System.out.println("insertProviderAt(null): " + e);
        }
        Security.removeProvider(null);
        try {
            Security.setProperty("foo", null);
            throw new Exception();
        } catch (NullPointerException e) {
            System.out.println("setProperty(\"foo\", null): " + e);
        }
        try {
            Security.setProperty(null, "foo");
            throw new Exception();
        } catch (NullPointerException e) {
            System.out.println("setProperty(null, \"foo\"): " + e);
        }
        try {
            Security.setProperty(null, null);
            throw new Exception();
        } catch (NullPointerException e) {
            System.out.println("setProperty(null, null): " + e);
        }
        if (Security.getAlgorithmProperty(null, null) != null) {
            throw new Exception();
        }
    }

}
