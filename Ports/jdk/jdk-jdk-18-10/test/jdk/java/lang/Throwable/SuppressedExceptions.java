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

import java.io.*;
import java.util.*;

/*
 * @test
 * @bug     6911258 6962571 6963622 6991528 7005628 8012044
 * @summary Basic tests of suppressed exceptions
 * @author  Joseph D. Darcy
 */

public class SuppressedExceptions {
    private static String message = "Bad suppressed exception information";

    public static void main(String... args) throws Exception {
        noSelfSuppression();
        basicSupressionTest();
        serializationTest();
        selfReference();
        noModification();
        initCausePlumbing();
    }

    private static void noSelfSuppression() {
        Throwable throwable = new Throwable();
        try {
            throwable.addSuppressed(throwable);
            throw new RuntimeException("IllegalArgumentException for self-suppresion not thrown.");
        } catch (IllegalArgumentException iae) {
            // Expected to be here
            if (iae.getCause() != throwable)
                throw new RuntimeException("Bad cause after self-suppresion.");
        }
    }

    private static void basicSupressionTest() {
        Throwable throwable = new Throwable();
        RuntimeException suppressed = new RuntimeException("A suppressed exception.");
        AssertionError repressed  = new AssertionError("A repressed error.");

        Throwable[] t0 = throwable.getSuppressed();
        if (t0.length != 0) {
            throw new RuntimeException(message);
        }
        throwable.printStackTrace();

        throwable.addSuppressed(suppressed);
        Throwable[] t1 = throwable.getSuppressed();
        if (t1.length != 1 ||
            t1[0] != suppressed) {throw new RuntimeException(message);
        }
        throwable.printStackTrace();

        throwable.addSuppressed(repressed);
        Throwable[] t2 = throwable.getSuppressed();
        if (t2.length != 2 ||
            t2[0] != suppressed ||
            t2[1] != repressed) {
            throw new RuntimeException(message);
        }
        throwable.printStackTrace();
    }

    private static void serializationTest() throws Exception {
        /*
         * Bytes of the serial form of
         *
         * (new Throwable())setStackTrace(new StackTraceElement[0])
         *
         * from JDK 6; suppressedException field will be missing and
         * thus default to null upon deserialization.
         */
        byte[] bytes = {
            (byte)0xac, (byte)0xed, (byte)0x00, (byte)0x05, (byte)0x73, (byte)0x72, (byte)0x00, (byte)0x13,
            (byte)0x6a, (byte)0x61, (byte)0x76, (byte)0x61, (byte)0x2e, (byte)0x6c, (byte)0x61, (byte)0x6e,
            (byte)0x67, (byte)0x2e, (byte)0x54, (byte)0x68, (byte)0x72, (byte)0x6f, (byte)0x77, (byte)0x61,
            (byte)0x62, (byte)0x6c, (byte)0x65, (byte)0xd5, (byte)0xc6, (byte)0x35, (byte)0x27, (byte)0x39,
            (byte)0x77, (byte)0xb8, (byte)0xcb, (byte)0x03, (byte)0x00, (byte)0x03, (byte)0x4c, (byte)0x00,
            (byte)0x05, (byte)0x63, (byte)0x61, (byte)0x75, (byte)0x73, (byte)0x65, (byte)0x74, (byte)0x00,
            (byte)0x15, (byte)0x4c, (byte)0x6a, (byte)0x61, (byte)0x76, (byte)0x61, (byte)0x2f, (byte)0x6c,
            (byte)0x61, (byte)0x6e, (byte)0x67, (byte)0x2f, (byte)0x54, (byte)0x68, (byte)0x72, (byte)0x6f,
            (byte)0x77, (byte)0x61, (byte)0x62, (byte)0x6c, (byte)0x65, (byte)0x3b, (byte)0x4c, (byte)0x00,
            (byte)0x0d, (byte)0x64, (byte)0x65, (byte)0x74, (byte)0x61, (byte)0x69, (byte)0x6c, (byte)0x4d,
            (byte)0x65, (byte)0x73, (byte)0x73, (byte)0x61, (byte)0x67, (byte)0x65, (byte)0x74, (byte)0x00,
            (byte)0x12, (byte)0x4c, (byte)0x6a, (byte)0x61, (byte)0x76, (byte)0x61, (byte)0x2f, (byte)0x6c,
            (byte)0x61, (byte)0x6e, (byte)0x67, (byte)0x2f, (byte)0x53, (byte)0x74, (byte)0x72, (byte)0x69,
            (byte)0x6e, (byte)0x67, (byte)0x3b, (byte)0x5b, (byte)0x00, (byte)0x0a, (byte)0x73, (byte)0x74,
            (byte)0x61, (byte)0x63, (byte)0x6b, (byte)0x54, (byte)0x72, (byte)0x61, (byte)0x63, (byte)0x65,
            (byte)0x74, (byte)0x00, (byte)0x1e, (byte)0x5b, (byte)0x4c, (byte)0x6a, (byte)0x61, (byte)0x76,
            (byte)0x61, (byte)0x2f, (byte)0x6c, (byte)0x61, (byte)0x6e, (byte)0x67, (byte)0x2f, (byte)0x53,
            (byte)0x74, (byte)0x61, (byte)0x63, (byte)0x6b, (byte)0x54, (byte)0x72, (byte)0x61, (byte)0x63,
            (byte)0x65, (byte)0x45, (byte)0x6c, (byte)0x65, (byte)0x6d, (byte)0x65, (byte)0x6e, (byte)0x74,
            (byte)0x3b, (byte)0x78, (byte)0x70, (byte)0x71, (byte)0x00, (byte)0x7e, (byte)0x00, (byte)0x04,
            (byte)0x70, (byte)0x75, (byte)0x72, (byte)0x00, (byte)0x1e, (byte)0x5b, (byte)0x4c, (byte)0x6a,
            (byte)0x61, (byte)0x76, (byte)0x61, (byte)0x2e, (byte)0x6c, (byte)0x61, (byte)0x6e, (byte)0x67,
            (byte)0x2e, (byte)0x53, (byte)0x74, (byte)0x61, (byte)0x63, (byte)0x6b, (byte)0x54, (byte)0x72,
            (byte)0x61, (byte)0x63, (byte)0x65, (byte)0x45, (byte)0x6c, (byte)0x65, (byte)0x6d, (byte)0x65,
            (byte)0x6e, (byte)0x74, (byte)0x3b, (byte)0x02, (byte)0x46, (byte)0x2a, (byte)0x3c, (byte)0x3c,
            (byte)0xfd, (byte)0x22, (byte)0x39, (byte)0x02, (byte)0x00, (byte)0x00, (byte)0x78, (byte)0x70,
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x78, (byte)0xac, (byte)0xed, (byte)0x00,
            (byte)0x05, (byte)0x73, (byte)0x72, (byte)0x00, (byte)0x13, (byte)0x6a, (byte)0x61, (byte)0x76,
            (byte)0x61, (byte)0x2e, (byte)0x6c, (byte)0x61, (byte)0x6e, (byte)0x67, (byte)0x2e, (byte)0x54,
            (byte)0x68, (byte)0x72, (byte)0x6f, (byte)0x77, (byte)0x61, (byte)0x62, (byte)0x6c, (byte)0x65,
            (byte)0xd5, (byte)0xc6, (byte)0x35, (byte)0x27, (byte)0x39, (byte)0x77, (byte)0xb8, (byte)0xcb,
            (byte)0x03, (byte)0x00, (byte)0x03, (byte)0x4c, (byte)0x00, (byte)0x05, (byte)0x63, (byte)0x61,
            (byte)0x75, (byte)0x73, (byte)0x65, (byte)0x74, (byte)0x00, (byte)0x15, (byte)0x4c, (byte)0x6a,
            (byte)0x61, (byte)0x76, (byte)0x61, (byte)0x2f, (byte)0x6c, (byte)0x61, (byte)0x6e, (byte)0x67,
            (byte)0x2f, (byte)0x54, (byte)0x68, (byte)0x72, (byte)0x6f, (byte)0x77, (byte)0x61, (byte)0x62,
            (byte)0x6c, (byte)0x65, (byte)0x3b, (byte)0x4c, (byte)0x00, (byte)0x0d, (byte)0x64, (byte)0x65,
            (byte)0x74, (byte)0x61, (byte)0x69, (byte)0x6c, (byte)0x4d, (byte)0x65, (byte)0x73, (byte)0x73,
            (byte)0x61, (byte)0x67, (byte)0x65, (byte)0x74, (byte)0x00, (byte)0x12, (byte)0x4c, (byte)0x6a,
            (byte)0x61, (byte)0x76, (byte)0x61, (byte)0x2f, (byte)0x6c, (byte)0x6e, (byte)0x67, (byte)0x3b,
            (byte)0x61, (byte)0x6e, (byte)0x67, (byte)0x2f, (byte)0x53, (byte)0x74, (byte)0x72, (byte)0x69,
            (byte)0x5b, (byte)0x00, (byte)0x0a, (byte)0x73, (byte)0x74, (byte)0x61, (byte)0x63, (byte)0x6b,
            (byte)0x54, (byte)0x72, (byte)0x61, (byte)0x63, (byte)0x65, (byte)0x74, (byte)0x00, (byte)0x1e,
            (byte)0x5b, (byte)0x4c, (byte)0x6a, (byte)0x61, (byte)0x76, (byte)0x61, (byte)0x2f, (byte)0x6c,
            (byte)0x61, (byte)0x6e, (byte)0x67, (byte)0x2f, (byte)0x53, (byte)0x74, (byte)0x61, (byte)0x63,
            (byte)0x6b, (byte)0x54, (byte)0x72, (byte)0x61, (byte)0x63, (byte)0x65, (byte)0x45, (byte)0x6c,
            (byte)0x65, (byte)0x6d, (byte)0x65, (byte)0x6e, (byte)0x74, (byte)0x3b, (byte)0x78, (byte)0x70,
            (byte)0x71, (byte)0x00, (byte)0x7e, (byte)0x00, (byte)0x04, (byte)0x70, (byte)0x75, (byte)0x72,
            (byte)0x00, (byte)0x1e, (byte)0x5b, (byte)0x4c, (byte)0x6a, (byte)0x61, (byte)0x76, (byte)0x61,
            (byte)0x2e, (byte)0x6c, (byte)0x61, (byte)0x6e, (byte)0x67, (byte)0x2e, (byte)0x53, (byte)0x74,
            (byte)0x61, (byte)0x63, (byte)0x6b, (byte)0x54, (byte)0x72, (byte)0x61, (byte)0x63, (byte)0x65,
            (byte)0x45, (byte)0x6c, (byte)0x65, (byte)0x6d, (byte)0x65, (byte)0x6e, (byte)0x74, (byte)0x3b,
            (byte)0x02, (byte)0x46, (byte)0x2a, (byte)0x3c, (byte)0x3c, (byte)0xfd, (byte)0x22, (byte)0x39,
            (byte)0x02, (byte)0x00, (byte)0x00, (byte)0x78, (byte)0x70,
        };

        try(ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            ObjectInputStream ois = new ObjectInputStream(bais)) {
            Object o = ois.readObject();
            Throwable throwable = (Throwable) o;

            System.err.println("TESTING SERIALIZED EXCEPTION");

            Throwable[] t0 = throwable.getSuppressed();
            if (t0.length != 0) { // Will fail if t0 is null.
                throw new RuntimeException(message);
            }
            throwable.printStackTrace();
        }
    }

    private static void selfReference() {
        Throwable throwable1 = new RuntimeException();
        Throwable throwable2 = new AssertionError();
        throwable1.initCause(throwable2);
        throwable2.initCause(throwable1);

        throwable1.printStackTrace();

        throwable1.addSuppressed(throwable2);
        throwable2.addSuppressed(throwable1);

        throwable1.printStackTrace();
    }

    private static void noModification() {
        Throwable t = new NoSuppression(false);

        Throwable[] t0 = t.getSuppressed();
        if (t0.length != 0)
            throw new RuntimeException("Bad nonzero length of suppressed exceptions.");

        t.addSuppressed(new ArithmeticException());

        // Make sure a suppressed exception did *not* get added.
        t0 = t.getSuppressed();
        if (t0.length != 0)
            throw new RuntimeException("Bad nonzero length of suppressed exceptions.");

        Throwable suppressed = new ArithmeticException();
        t = new NoSuppression(true); // Suppression enabled
        // Make sure addSuppressed(null) throws an NPE
        try {
            t.addSuppressed(null);
            throw new RuntimeException("NPE not thrown!");
        } catch(NullPointerException e) {
            ; // Expected
        }
        t.addSuppressed(suppressed);
        t0 = t.getSuppressed();
        if (t0.length != 1 || t0[0] != suppressed)
            throw new RuntimeException("Expected suppression did not occur.");
    }

    private static class NoSuppression extends Throwable {
        public NoSuppression(boolean enableSuppression) {
            super("The medium.", null, enableSuppression, true);
        }
    }

    private static void initCausePlumbing() {
        Throwable t1 = new Throwable();
        Throwable t2 = new Throwable("message", t1);
        Throwable t3 = new Throwable();

        try {
            t2.initCause(t3);
            throw new RuntimeException("Shouldn't reach.");
        } catch (IllegalStateException ise) {
            if (ise.getCause() != t2)
                throw new RuntimeException("Unexpected cause in ISE", ise);
            Throwable[] suppressed = ise.getSuppressed();
            if (suppressed.length !=  0)
                throw new RuntimeException("Bad suppression in ISE", ise);
        }

        try {
            t2.initCause(null);
            throw new RuntimeException("Shouldn't reach.");
        } catch (IllegalStateException ise) {
            ; // Expected; don't want an NPE.
        }

        try {
            t3.initCause(t3);
            throw new RuntimeException("Shouldn't reach.");
        } catch (IllegalArgumentException iae) {
            if (iae.getCause() != t3)
                throw new RuntimeException("Unexpected cause in ISE", iae);
        }
    }
}
