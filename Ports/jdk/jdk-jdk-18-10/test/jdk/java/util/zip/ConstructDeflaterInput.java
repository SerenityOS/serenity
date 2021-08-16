/*
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4679743 8148624
 * @summary Test parts of DeflaterInputStream code that don't really do I/O.
 */

import java.io.*;
import java.util.zip.*;

public class ConstructDeflaterInput {

    static class MyDeflater extends Deflater {
        volatile boolean ended = false;
        public void end() {
            ended = true;
            super.end();
        }
    }

    public static void realMain(String[] args) throws Throwable {
        final MyDeflater def = new MyDeflater();
        ByteArrayInputStream bais = new ByteArrayInputStream(
            "hello, world".getBytes());
        DeflaterInputStream dis = null;
        byte[] b = new byte[512];

        // Check construction
        //
        try {
            dis = new DeflaterInputStream(null);
            fail();
        } catch (NullPointerException ex) {
            pass();
        }

        try {
            dis = new DeflaterInputStream(bais, null);
            fail();
        } catch (NullPointerException ex) {
            pass();
        }

        try {
            dis = new DeflaterInputStream(bais, def, 0);
            fail();
        } catch (IllegalArgumentException ex) {
            pass();
        }

        // Check sanity checks in read methods
        //
        dis = new DeflaterInputStream(bais, def);

        try {
            dis.read(null, 5, 2);
            fail();
        } catch (NullPointerException ex) {
            pass();
        }

        try {
            dis.read(b, -1, 0);
            fail();
        } catch (IndexOutOfBoundsException ex) {
            pass();
        }

        try {
            dis.read(b, 0, -1);
            fail();
        } catch (IndexOutOfBoundsException ex) {
            pass();
        }

        try {
            dis.read(b, 0, 600);
            fail();
        } catch (IndexOutOfBoundsException ex) {
            pass();
        }

        int len = 0;
        try {
            len = dis.read(b, 0, 0);
            check(len == 0);
        } catch (IndexOutOfBoundsException ex) {
            fail("Read of length 0 should return 0, but returned " + len);
        }

        try {
            dis.skip(-1);
            fail();
        } catch (IllegalArgumentException ex) {
            pass();
        }

        // Check unsupported operations
        //
        check(!dis.markSupported());
        check(dis.available() == 1);
        check(!def.ended);
        try {
            dis.reset();
            fail();
        } catch (IOException ex) {
            pass();
        }

        // Check close
        //
        dis.close();
        check(!def.ended);

        try {
            dis.available();
            fail();
        } catch (IOException ex) {
            pass();
        }

        try {
            int x = dis.read();
            fail();
        } catch (IOException ex) {
            pass();
        }

        try {
            dis.skip(1);
            fail();
        } catch (IOException ex) {
            pass();
        }
        java.lang.ref.Reference.reachabilityFence(def);
    }

    //--------------------- Infrastructure ---------------------------
    static volatile int passed = 0, failed = 0;
    static void pass() {passed++;}
    static void fail() {failed++; Thread.dumpStack();}
    static void fail(String msg) {System.out.println(msg); fail();}
    static void unexpected(Throwable t) {failed++; t.printStackTrace();}
    static void check(boolean cond) {if (cond) pass(); else fail();}
    static void equal(Object x, Object y) {
        if (x == null ? y == null : x.equals(y)) pass();
        else fail(x + " not equal to " + y);}
    public static void main(String[] args) throws Throwable {
        try {realMain(args);} catch (Throwable t) {unexpected(t);}
        System.out.println("\nPassed = " + passed + " failed = " + failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
}
