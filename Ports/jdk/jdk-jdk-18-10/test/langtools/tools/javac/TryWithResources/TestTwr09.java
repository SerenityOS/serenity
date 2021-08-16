/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     8025113
 * @author  sogoel
 * @summary t-w-r completes abruptly if the initialization of resource completes abruptly
 */

import java.io.FileInputStream;
import java.io.IOException;
import java.io.File;

/*
 * If the initialization of the resource completes abruptly because of a
 * throw of a value V ... and the automatic ->closing of the resource completes normally,
 * then the try-with-resources statement completes abruptly because of the throw of value V.
 */
public class TestTwr09 {

    /**
     * throw from ctor of nested resource
     * Check first resource is not open.
     */
    String test1() {
        String ret = null;
        try (ResCloseable tr = new ResCloseable(new ResCloseable("throw from inner resource ctor",3))) {
            ret = "FAIL";
        } catch (RuntimeException re) {
            ret = re.getMessage();
        }
        return ret;
    }

    /**
     * throw from ctor of 2nd resource.
     * 1st resource, FileInputStream should be automatically closed.
     */
    String test2() {
        String ret = null;
        byte[] buf = new byte[1];
        try (java.io.ByteArrayInputStream tr = new java.io.ByteArrayInputStream(buf);
            ResCloseable str = new ResCloseable("throw from inner resource ctor",3)) {
            ret = "FAIL";
        } catch (final IOException fe) {
            ret = "FAIL test2";
        } catch (RuntimeException re) {
            ret = "PASS test2";
        }
        System.out.println("Ret = " + ret);
        return ret;
    }

    public static void main(String... args) {
        TestTwr09 t = new TestTwr09();
        if (t.test1().compareTo("throw from inner resource ctor") != 0) {
            throw new RuntimeException("FAIL-test1");
        }
        if (t.test2().compareTo("PASS test2") != 0) {
            throw new RuntimeException("FAIL-test2");
        }
    }
}

/** a simple resource the implements AutoCloseable so it can be used
 * in twr's resource specification block.
 */
class ResCloseable implements AutoCloseable {

    ResCloseable testres = null;
    String msg = "default";
    boolean bOpen = false;

    public ResCloseable() {
        bOpen = true;
    }

    public ResCloseable(ResCloseable tr) {
        bOpen = true;
        msg = tr.getMsg();
    }

    public ResCloseable(String s) {
        bOpen = true;
        msg = s;
    }

    public ResCloseable(String msg, int c) {
        bOpen = true;
        if (c == 3) {
            throw new RuntimeException(msg);
        }
    }

    @Override
    public void close() {
        bOpen = false;
    }

    public boolean isOpen() {
        return bOpen;
    }

    public String getMsg() {
        return msg;
    }
}

