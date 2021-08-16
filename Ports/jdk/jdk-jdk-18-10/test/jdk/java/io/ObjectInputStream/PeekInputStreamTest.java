/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/*
 * @test
 * @bug 8067870
 * @modules java.base/java.io:open
 * @summary verifies java.io.ObjectInputStream.PeekInputStream.skip works
 *          as intended
 */
public class PeekInputStreamTest {

    public static void main(String[] args) throws ReflectiveOperationException,
            IOException {

        InputStream pin = createPeekInputStream(
                new ByteArrayInputStream(new byte[]{1, 2, 3, 4}));
        peek(pin);
        if (pin.skip(1) != 1 || pin.read() != 2)
            throw new AssertionError();

        InputStream pin1 = createPeekInputStream(
                new ByteArrayInputStream(new byte[]{1, 2, 3, 4}));
        if (pin1.skip(1) != 1 || pin1.read() != 2)
            throw new AssertionError();

        InputStream pin2 = createPeekInputStream(
                new ByteArrayInputStream(new byte[]{1, 2, 3, 4}));
        if (pin2.skip(0) != 0 || pin2.read() != 1)
            throw new AssertionError();

        InputStream pin3 = createPeekInputStream(
                new ByteArrayInputStream(new byte[]{1, 2, 3, 4}));
        if (pin3.skip(2) != 2 || pin3.read() != 3)
            throw new AssertionError();

        InputStream pin4 = createPeekInputStream(
                new ByteArrayInputStream(new byte[]{1, 2, 3, 4}));
        if (pin4.skip(3) != 3 || pin4.read() != 4)
            throw new AssertionError();

        InputStream pin5 = createPeekInputStream(
                new ByteArrayInputStream(new byte[]{1, 2, 3, 4}));
        if (pin5.skip(16) != 4 || pin5.read() != -1)
            throw new AssertionError();
    }

    private static InputStream createPeekInputStream(InputStream underlying)
            throws ReflectiveOperationException {
        Class<? extends InputStream> clazz =
                Class.forName("java.io.ObjectInputStream$PeekInputStream")
                        .asSubclass(InputStream.class);

        Constructor<? extends InputStream> ctr =
                clazz.getDeclaredConstructor(InputStream.class);
        ctr.setAccessible(true);
        return ctr.newInstance(underlying);
    }

    private static void peek(InputStream pin)
            throws ReflectiveOperationException {
        Method p = pin.getClass().getDeclaredMethod("peek");
        p.setAccessible(true);
        p.invoke(pin);
    }
}
