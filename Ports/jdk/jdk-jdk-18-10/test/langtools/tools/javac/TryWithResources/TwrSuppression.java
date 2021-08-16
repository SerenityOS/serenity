/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6971877
 * @author Joseph D. Darcy
 * @summary Verify a primary exception suppresses all throwables
 */

public class TwrSuppression implements AutoCloseable {
    public static void main(String... args) throws Throwable {
        try {
            try (TwrSuppression r1 = new TwrSuppression(false);
                 TwrSuppression r2 = new TwrSuppression(true)) {
                throw new RuntimeException();
            }
        } catch(RuntimeException e) {
            Throwable[] suppressedExceptions = e.getSuppressed();
            int length = suppressedExceptions.length;
            if (length != 2)
                throw new RuntimeException("Unexpected length " + length);

            if (suppressedExceptions[0].getClass() != Error.class ||
                suppressedExceptions[1].getClass() != Exception.class) {
                System.err.println("Unexpected suppressed types!");
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }

    private boolean throwError;

    private TwrSuppression(boolean throwError) {
        this.throwError = throwError;
    }

    @Override
    public void close() throws Exception {
        if (throwError) {
            throw new Error();
        } else {
            throw new Exception();
        }
    }
}
