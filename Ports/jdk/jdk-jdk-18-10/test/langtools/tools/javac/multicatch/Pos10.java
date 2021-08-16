/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7039822
 * @summary Verify lub of an exception parameter can be an intersection type
 * @author Joseph D. Darcy
 */

public class Pos10 {
    public static void main(String... args) {
        test(0);
        test(1);

        if (record != 0b11)
            throw new RuntimeException("Unexpected exception execution: " +
                                       record);
        if (closeRecord != 0b11)
            throw new RuntimeException("Unexpected close execution: " +
                                       closeRecord);
    }

    private static int record = 0;
    private static int closeRecord = 0;

    private static void test(int i) {
        try {
            thrower(i);
        } catch (SonException | DaughterException e) {
            Class<? extends ParentException> clazz = e.getClass();
            HasFoo m = e;
            e.foo();

            try (AutoCloseable ac = e) {
                e.toString();
            } catch(Exception except) {
                throw new RuntimeException(except);
            }
        }
    }

    private static interface HasFoo {
        void foo();
    }

    private static void thrower(int i) throws SonException, DaughterException {
        if (i == 0)
            throw new SonException();
        else
            throw new DaughterException();
    }

    private static class ParentException extends RuntimeException {}

    private static class SonException
        extends ParentException
        implements HasFoo, AutoCloseable {

        public void foo() {
            record |= 0b01;
        }

        @Override
        public void close() {
            closeRecord |= 0b01;
        }
    }

    private static class DaughterException
        extends ParentException
        implements HasFoo, AutoCloseable {

        public void foo() {
            record |= 0b10;
        }

        @Override
        public  void close() {
            closeRecord |= 0b10;
        }
    }
}
