/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

public class IsCompatibleWith {
    private static boolean fail = false;
    private static Package p = p.A.class.getPackage();

    public static void main(String [] args) {
        IsCompatibleWith s = new IsCompatibleWith();

        s.ex("");
        s.ex("x.31");
        s.ex("5....");
        s.ex("10.3a");
        s.ex("2.-2");
        s.ex("-1.0");
        s.ex(".01");

        s.t("0");
        s.t("1.4");
        s.t("1.4.0");
        s.f("1.415");
        s.t("1.3.1.0.0.0.0.7");
        s.t("1.2.02");
        s.f("1.4.35");
        s.f("33.0");

        s.f(new Integer(Integer.MAX_VALUE).toString());
        s.ex("2147483648");  // Integer.MAX_VALUE + 1

        if (fail == true)
            throw new RuntimeException("Test FAILED");
        else
            System.out.println("Test PASSED");
    }

    // NumberFormatException expected
    private void ex(String s) {
        try {
            p.isCompatibleWith(s);
        } catch (NumberFormatException e) {
            System.out.println("PASS: \"" + s + "\"");
            e.printStackTrace(System.out);
            return;
        }
        fail = true;
        System.err.println("FAIL: Exception missing: \"" + s + "\"");
    }

    // "true" or "false" expected
    private void t(String s) { test(s, true);  }
    private void f(String s) { test(s, false); }

    private void test(String s, boolean expect) {
        try {
            if (p.isCompatibleWith(s) != expect) {
                System.err.println("FAIL: \"" + s + "\", expected: " + expect);
                fail = true;
                return;
            }
        } catch (Exception e) {
            System.err.println("FAIL: \"" + s + "\"");
            e.printStackTrace();
            fail = true;
            return;
        }
        System.out.println("PASS: \"" + s + "\"");
    }
}
