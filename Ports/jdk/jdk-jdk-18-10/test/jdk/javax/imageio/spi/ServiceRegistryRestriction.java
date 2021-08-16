/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8068749
 * @run main ServiceRegistryRestriction
 * @summary Tests ServiceRegistry's restriction on handling
 *          only standard Image I/O service types.
 */

import java.util.*;
import java.util.function.Consumer;
import javax.imageio.spi.*;

public class ServiceRegistryRestriction {
    static class DummyTestSpi {
    }

    ClassLoader cl = ServiceRegistryRestriction.class.getClassLoader();

    <T> void construct(Class<T> clazz) {
        List<Class<?>> list = Arrays.<Class<?>>asList(clazz);
        ServiceRegistry sr = new ServiceRegistry(list.iterator());
    }

    <T> void lookup(Class<T> clazz) {
        Iterator<T> i = ServiceRegistry.lookupProviders(clazz);
    }

    <T> void lookupCL(Class<T> clazz) {
        Iterator<T> i = ServiceRegistry.lookupProviders(clazz, cl);
    }

    <T> void doOneTest(String label, Class<T> clazz, boolean expectFail, Consumer<Class<T>> op) {
        System.out.printf("testing %s with %s...", label, clazz.getName());
        try {
            op.accept(clazz);
            if (expectFail) {
                throw new AssertionError("fail, operation succeeded unexpectedly");
            } else {
                System.out.println("success");
            }
        } catch (IllegalArgumentException iae) {
            if (expectFail) {
                System.out.println("success, got expected IAE");
            } else {
                throw new AssertionError("fail, unexpected exception", iae);
            }
        }
    }

    void doTests(Class<?> clazz, boolean expectFail) {
        doOneTest("constructor", clazz, expectFail, this::construct);
        doOneTest("lookup", clazz, expectFail, this::lookup);
        doOneTest("lookupCL", clazz, expectFail, this::lookupCL);
    }

    void run() {
        doTests(ImageInputStreamSpi.class, false);
        doTests(ImageOutputStreamSpi.class, false);
        doTests(ImageReaderSpi.class, false);
        doTests(ImageTranscoderSpi.class, false);
        doTests(ImageWriterSpi.class, false);
        doTests(DummyTestSpi.class, true);
    }

    public static void main(String[] args) {
        new ServiceRegistryRestriction().run();
    }
}
