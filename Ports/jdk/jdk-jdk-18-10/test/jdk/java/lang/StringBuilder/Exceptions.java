/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6248507
 * @summary Verify that exceptions are thrown as expected.
 */

public class Exceptions {
    private static boolean ok = true;

    private static void fail(Throwable ex, String s, Throwable got) {
        ok = false;
        System.err.println("expected "
                           + ex.getClass().getName() + ": " + ex.getMessage()
                           + " for " + s
                           + " got "
                           + got.getClass().getName() + ": " + got.getMessage()
                           + " - FAILED");
    }

    private static void pass(String s) {
        System.out.println(s + " -- OK");
    }

    private static void tryCatch(String s, Throwable ex, Runnable thunk) {
        Throwable t = null;
        try {
            thunk.run();
        } catch (Throwable x) {
            if (ex.getClass().isAssignableFrom(x.getClass()))
                t = x;
            else
                x.printStackTrace();
        }
        if ((t == null) && (ex != null))
            fail(ex, s, t);

        String msg = (ex == null ? null : ex.getMessage());
        if ((msg != null) && !msg.equals(t.getMessage()))
            fail(ex, s, t);
        else
            pass(s);
    }

    public static void main(String [] args) {
        System.out.println("StringBuilder()");
        tryCatch("  no args", null, new Runnable() {
                public void run() {
                    new StringBuilder();
                }});

        System.out.println("StringBuilder(int length)");
        tryCatch("  1", null, new Runnable() {
                public void run() {
                    new StringBuilder(1);
                }});
        tryCatch("  -1", new NegativeArraySizeException(), new Runnable() {
                public void run() {
                    new StringBuilder(-1);
                }});

        System.out.println("StringBuilder(String str)");
        tryCatch("  null", new NullPointerException(), new Runnable() {
                public void run() {
                    new StringBuilder(null);
                }});
        tryCatch("  foo", null, new Runnable() {
                public void run() {
                    new StringBuilder("foo");
                }});

        System.out.println("StringBuilder.replace(int start, int end, String str)");
        tryCatch("  -1, 2, \" \"",
                 new StringIndexOutOfBoundsException("Range [-1, 2) out of bounds for length 7"),
                 new Runnable() {
                public void run() {
                    StringBuilder sb = new StringBuilder("hilbert");
                    sb.replace(-1, 2, " ");
                }});
        tryCatch("  7, 8, \" \"",
                 new StringIndexOutOfBoundsException("Range [7, 6) out of bounds for length 6"),
                 new Runnable() {
                public void run() {
                    StringBuilder sb = new StringBuilder("banach");
                    sb.replace(7, 8, " ");
                }});
        tryCatch("  2, 1, \" \"",
                 new StringIndexOutOfBoundsException("Range [2, 1) out of bounds for length 7"),
                 new Runnable() {
                public void run() {
                    StringBuilder sb = new StringBuilder("riemann");
                    sb.replace(2, 1, " ");
                }});

        if (!ok) {
            throw new RuntimeException("Some tests FAILED");
        } else {
            System.out.println("All tests PASSED");
        }
    }
}