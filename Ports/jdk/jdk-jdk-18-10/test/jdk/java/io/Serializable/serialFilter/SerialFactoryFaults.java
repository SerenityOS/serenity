/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.ObjectInputFilter;
import java.io.ObjectInputFilter.Config;
import java.util.function.BinaryOperator;

/* @test
 * @run testng/othervm  -Djdk.serialFilterFactory=ForcedError_NoSuchClass SerialFactoryFaults
 * @run testng/othervm  -Djdk.serialFilterFactory=SerialFactoryFaults$NoPublicConstructor SerialFactoryFaults
 * @run testng/othervm  -Djdk.serialFilterFactory=SerialFactoryFaults$ConstructorThrows SerialFactoryFaults
 * @run testng/othervm  -Djdk.serialFilterFactory=SerialFactoryFaults$FactorySetsFactory SerialFactoryFaults
 * @summary Check cases where the Filter Factory initialization from properties fails
 */

@Test
public class SerialFactoryFaults {

    static {
        // Enable logging
        System.setProperty("java.util.logging.config.file",
                System.getProperty("test.src", ".") + "/logging.properties");
    }

    public void initFaultTest() {
        String factoryName = System.getProperty("jdk.serialFilterFactory");
        ExceptionInInitializerError ex = Assert.expectThrows(ExceptionInInitializerError.class,
                () -> Config.getSerialFilterFactory());
        Throwable cause = ex.getCause();

        if (factoryName.equals("ForcedError_NoSuchClass")) {
            Assert.assertEquals(cause.getClass(),
                    ClassNotFoundException.class, "wrong exception");
        } else if (factoryName.equals("SerialFactoryFaults$NoPublicConstructor")) {
            Assert.assertEquals(cause.getClass(),
                    NoSuchMethodException.class, "wrong exception");
        } else if (factoryName.equals("SerialFactoryFaults$ConstructorThrows")) {
            Assert.assertEquals(cause.getClass(),
                    IllegalStateException.class, "wrong exception");
        } else if (factoryName.equals("SerialFactoryFaults$FactorySetsFactory")) {
            Assert.assertEquals(cause.getClass(),
                    IllegalStateException.class, "wrong exception");
            Assert.assertEquals(cause.getMessage(),
                    "Cannot replace filter factory: initialization incomplete",
                    "wrong message");
        } else {
            Assert.fail("No test for filter factory: " + factoryName);
        }
    }

    /**
     * Test factory that does not have the required public no-arg constructor.
     */
    public static final class NoPublicConstructor
            implements BinaryOperator<ObjectInputFilter> {
        private NoPublicConstructor() {
        }

        public ObjectInputFilter apply(ObjectInputFilter curr, ObjectInputFilter next) {
            throw new RuntimeException("NYI");
        }
    }

    /**
     * Test factory that has a constructor that throws a runtime exception.
     */
    public static final class ConstructorThrows
            implements BinaryOperator<ObjectInputFilter> {
        public ConstructorThrows() {
            throw new IllegalStateException("SerialFactoryFaults$ConstructorThrows");
        }

        public ObjectInputFilter apply(ObjectInputFilter curr, ObjectInputFilter next) {
            throw new RuntimeException("NYI");
        }
    }

    /**
     * Test factory that has a constructor tries to set the filter factory.
     */
    public static final class FactorySetsFactory
            implements BinaryOperator<ObjectInputFilter> {
        public FactorySetsFactory() {
            Config.setSerialFilterFactory(this);
        }

        public ObjectInputFilter apply(ObjectInputFilter curr, ObjectInputFilter next) {
            throw new RuntimeException("NYI");
        }
    }

}
