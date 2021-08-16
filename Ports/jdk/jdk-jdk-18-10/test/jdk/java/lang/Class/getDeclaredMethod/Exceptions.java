/*
 * Copyright (c) 2002, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4319910
 * @summary Verify that exceptions are thrown as expected.
 */

public class Exceptions {
    void m0() {}
    public void m1() {}
    private void m2() {}
    protected void m4() {}

    private static final String [] npe = {null};
    private static final String [] nsme = {"m6"};
    private static final String [] pass = {"m0", "m1", "m2", "m4"};

    private void test(String s, Class ex) {
        Throwable t = null;
        try {
            getClass().getDeclaredMethod(s, new Class[] {});
        } catch (Throwable x) {
            if (ex.isAssignableFrom(x.getClass()))
                t = x;
        }
        if ((t == null) && (ex != null))
            throw new RuntimeException("expected " + ex.getName()
                                       + " for " + s);
        else
            System.out.println(s + " OK");
    }

    public static void main(String [] args) {
        Exceptions e = new Exceptions();
        for (int i = 0; i < npe.length; i++)
            e.test(npe[i], NullPointerException.class);
        for (int i = 0; i < nsme.length; i++)
            e.test(nsme[i], NoSuchMethodException.class);
        for (int i = 0; i < pass.length; i++)
            e.test(pass[i], null);
    }
}
