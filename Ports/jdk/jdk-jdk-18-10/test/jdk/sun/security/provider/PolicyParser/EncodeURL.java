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

/*
 * @test
 * @bug 4797850
 * @modules java.base/sun.security.provider
 * @summary Security policy file does not grok hash mark in pathnames
 */

import java.io.*;
import java.util.*;
import sun.security.provider.*;

public class EncodeURL {

    // java.ext.dirs input and encoding
    private static final String extInput = "foo bar";
    private static final String extAnswer = "foo%20bar";
    private static final String policy0 =
        "grant codebase \"${java.ext.dirs}\" { permission java.security.AllPermission; };";

    // keystore inputs and encodings
    private static final String prop1 = "http://foobar";
    private static final String answer1 = "http://foobar/foo";
    private static final String policy1 =
        "keystore \"${prop1}/foo\"; grant { permission java.security.AllPermission; };";

    private static final String prop2 = "foo#bar";
    private static final String answer2 = "http://foo%23bar/foo";
    private static final String policy2 =
        "keystore \"http://${prop2}/foo\"; grant { permission java.security.AllPermission; };";

    private static final String prop3 = "goofy:foo#bar";
    private static final String answer3 = "http://goofy:foo%23bar/foo";
    private static final String policy3 =
        "keystore \"http://${prop3}/foo\"; grant { permission java.security.AllPermission; };";

    public static void main(String[] args) throws Exception {

        // make sure 'file' URLs created from java.ext.dirs
        // are encoded

        System.setProperty("java.ext.dirs", extInput);
        PolicyParser pp = new PolicyParser(true);
        pp.read(new StringReader(policy0));
        Enumeration e = pp.grantElements();
        while (e.hasMoreElements()) {
            PolicyParser.GrantEntry ge =
                                (PolicyParser.GrantEntry)e.nextElement();
            if (ge.codeBase.indexOf("foo") >= 0 &&
                ge.codeBase.indexOf(extAnswer) < 0) {
                throw new SecurityException("test 0 failed: " +
                        "expected " + extAnswer +
                        " inside " + ge.codeBase);
            }
        }

        // make sure keystore URL is properly encoded (or not)

        System.setProperty("prop1", prop1);
        pp = new PolicyParser(true);
        pp.read(new StringReader(policy1));
        if (!pp.getKeyStoreUrl().equals(answer1)) {
            throw new SecurityException("test 1 failed: " +
                "expected " + answer1 +
                ", and got " + pp.getKeyStoreUrl());
        }

        System.setProperty("prop2", prop2);
        pp = new PolicyParser(true);
        pp.read(new StringReader(policy2));
        if (!pp.getKeyStoreUrl().equals(answer2)) {
            throw new SecurityException("test 2 failed: " +
                "expected " + answer2 +
                ", and got " + pp.getKeyStoreUrl());
        }

        System.setProperty("prop3", prop3);
        pp = new PolicyParser(true);
        pp.read(new StringReader(policy3));
        if (!pp.getKeyStoreUrl().equals(answer3)) {
            throw new SecurityException("test 3 failed: " +
                "expected " + answer3 +
                ", and got " + pp.getKeyStoreUrl());
        }

        System.out.println("test passed");
    }
}
