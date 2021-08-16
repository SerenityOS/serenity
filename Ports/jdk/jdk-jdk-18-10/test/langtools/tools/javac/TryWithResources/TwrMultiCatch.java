/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6911256 6964740 7013420
 * @author Joseph D. Darcy
 * @summary Test that TWR and multi-catch play well together
 * @compile TwrMultiCatch.java
 * @run main TwrMultiCatch
 */

import java.io.IOException;
public class TwrMultiCatch implements AutoCloseable {
    private final Class<? extends Exception> exceptionClass;

    private TwrMultiCatch(Class<? extends Exception> exceptionClass) {
        this.exceptionClass = exceptionClass;
    }

    public static void main(String... args) {
        test(new TwrMultiCatch(CustomCloseException1.class),
             CustomCloseException1.class);

        test(new TwrMultiCatch(CustomCloseException2.class),
             CustomCloseException2.class);
    }

    private static void test(TwrMultiCatch twrMultiCatch,
                     Class<? extends Exception> expected) {
        try(TwrMultiCatch tmc = twrMultiCatch) {
            System.out.println(tmc.toString());
        } catch (CustomCloseException1 |
                 CustomCloseException2 exception) {
            if (!exception.getClass().equals(expected) ) {
                throw new RuntimeException("Unexpected catch!");
            }
        }
    }

    public void close() throws CustomCloseException1, CustomCloseException2 {
        Throwable t;
        try {
             t = exceptionClass.newInstance();
        } catch(ReflectiveOperationException rfe) {
            throw new RuntimeException(rfe);
        }

        try {
            throw t;
        } catch (CustomCloseException1 |
                 CustomCloseException2 exception) {
            throw exception;
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }
}

class CustomCloseException1 extends Exception {}
class CustomCloseException2 extends Exception {}
