/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import sun.reflect.ReflectionFactory;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.annotations.DataProvider;
import org.testng.TestNG;

/*
 * @test
 * @bug 8137058 8164908 8168980
 * @run testng ReflectionFactoryTest
 * @run testng/othervm/policy=security.policy ReflectionFactoryTest
 * @summary Basic test for the unsupported ReflectionFactory
 * @modules jdk.unsupported
 */

public class ReflectionFactoryTest {

    // Initialized by init()
    static ReflectionFactory factory;

    @DataProvider(name = "ClassConstructors")
    static Object[][] classConstructors() {
        return new Object[][] {
                {Object.class},
                {Foo.class},
                {Bar.class},
        };
    }

    @BeforeClass
    static void init() {
        factory = ReflectionFactory.getReflectionFactory();
    }

    /**
     * Test that the correct Constructor is selected and run.
     * @param type type of object to create
     * @throws NoSuchMethodException - error
     * @throws InstantiationException - error
     * @throws IllegalAccessException - error
     * @throws InvocationTargetException - error
     */
    @Test(dataProvider="ClassConstructors")
    static void testConstructor(Class<?> type)
        throws NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException
    {
        @SuppressWarnings("unchecked")
        Constructor<?> c = factory.newConstructorForSerialization(type);

        Object o = c.newInstance();
        Assert.assertEquals(o.getClass(), type, "Instance is wrong type");
        if (o instanceof Foo) {
            Foo foo = (Foo)o;
            foo.check();
        }
    }

    @DataProvider(name = "NonSerialConstructors")
    static Object[][] constructors() throws NoSuchMethodException {
        return new Object[][] {
                {Foo.class, Object.class.getDeclaredConstructor()},
                {Foo.class, Foo.class.getDeclaredConstructor()},
                {Baz.class, Object.class.getDeclaredConstructor()},
                {Baz.class, Foo.class.getDeclaredConstructor()},
                {Baz.class, Baz.class.getDeclaredConstructor()}
        };
    }

    /**
     * Tests that the given Constructor, in the hierarchy, is run.
     */
    @Test(dataProvider="NonSerialConstructors")
    static void testNonSerializableConstructor(Class<?> cl,
                                               Constructor<?> constructorToCall)
        throws ReflectiveOperationException
    {
        @SuppressWarnings("unchecked")
        Constructor<?> c = factory.newConstructorForSerialization(cl,
                                                                  constructorToCall);

        Object o = c.newInstance();
        Assert.assertEquals(o.getClass(), cl, "Instance is wrong type");

        int expectedFoo = 0;
        int expectedBaz = 0;
        if (constructorToCall.getName().equals("ReflectionFactoryTest$Foo")) {
            expectedFoo = 1;
        } else if (constructorToCall.getName().equals("ReflectionFactoryTest$Baz")) {
            expectedFoo = 1;
            expectedBaz = 4;
        }

        Assert.assertEquals(((Foo)o).foo(), expectedFoo);
        if (o instanceof Baz) {
            Assert.assertEquals(((Baz)o).baz(), expectedBaz);
        }
    }

    static class Foo {
        private int foo;
        public Foo() {
            this.foo = 1;
        }

        public String toString() {
            return "foo: " + foo;
        }

        public void check() {
            int expectedFoo = 1;
            Assert.assertEquals(foo, expectedFoo, "foo() constructor not run");
        }

        public int foo() { return foo; }
    }

    static class Bar extends Foo implements Serializable {
        private int bar;
        public Bar() {
            this.bar = 1;
        }

        public String toString() {
            return super.toString() + ", bar: " + bar;
        }

        public void check() {
            super.check();
            int expectedBar = 0;
            Assert.assertEquals(bar, expectedBar, "bar() constructor not run");
        }
    }

    static class Baz extends Foo {
        private final int baz;
        public Baz() { this.baz = 4; }
        public int baz() { return baz; }
    }

    /**
     * Test newConstructorForExternalization returns the constructor and it can be called.
     * @throws NoSuchMethodException - error
     * @throws InstantiationException - error
     * @throws IllegalAccessException - error
     * @throws InvocationTargetException - error
     */
    @Test
    static void newConstructorForExternalization()
            throws NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException {
        Constructor<?> cons = factory.newConstructorForExternalization(Ext.class);
        Ext ext = (Ext)cons.newInstance();
        Assert.assertEquals(ext.ext, 1, "Constructor not run");
    }

    static class Ext implements Externalizable {
        private static final long serialVersionUID = 1L;

        int ext;

        public Ext() {
            ext = 1;
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {}

        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {}
    }

    @Test
    static void testReadWriteObjectForSerialization() throws Throwable {
        MethodHandle readObjectMethod = factory.readObjectForSerialization(Ser.class);
        Assert.assertNotNull(readObjectMethod, "readObjectMethod not found");

        MethodHandle readObjectNoDataMethod = factory.readObjectNoDataForSerialization(Ser.class);
        Assert.assertNotNull(readObjectNoDataMethod, "readObjectNoDataMethod not found");

        MethodHandle writeObjectMethod = factory.writeObjectForSerialization(Ser.class);
        Assert.assertNotNull(writeObjectMethod, "writeObjectMethod not found");

        MethodHandle readResolveMethod = factory.readResolveForSerialization(Ser.class);
        Assert.assertNotNull(readResolveMethod, "readResolveMethod not found");

        MethodHandle writeReplaceMethod = factory.writeReplaceForSerialization(Ser.class);
        Assert.assertNotNull(writeReplaceMethod, "writeReplaceMethod not found");

        byte[] data = null;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            Ser ser = new Ser();

            writeReplaceMethod.invoke(ser);
            Assert.assertTrue(ser.writeReplaceCalled, "writeReplace not called");
            Assert.assertFalse(ser.writeObjectCalled, "writeObject should not have been called");

            writeObjectMethod.invoke(ser, oos);
            Assert.assertTrue(ser.writeReplaceCalled, "writeReplace should have been called");
            Assert.assertTrue(ser.writeObjectCalled, "writeObject not called");
            oos.flush();
            data = baos.toByteArray();
        }

        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             ObjectInputStream ois = new ObjectInputStream(bais)) {
            Ser ser2 = new Ser();

            readObjectMethod.invoke(ser2, ois);
            Assert.assertTrue(ser2.readObjectCalled, "readObject not called");
            Assert.assertFalse(ser2.readObjectNoDataCalled, "readObjectNoData should not be called");
            Assert.assertFalse(ser2.readResolveCalled, "readResolve should not be called");

            readObjectNoDataMethod.invoke(ser2, ois);
            Assert.assertTrue(ser2.readObjectCalled, "readObject should have been called");
            Assert.assertTrue(ser2.readObjectNoDataCalled, "readObjectNoData not called");
            Assert.assertFalse(ser2.readResolveCalled, "readResolve should not be called");

            readResolveMethod.invoke(ser2);
            Assert.assertTrue(ser2.readObjectCalled, "readObject should have been called");
            Assert.assertTrue(ser2.readObjectNoDataCalled, "readObjectNoData not called");
            Assert.assertTrue(ser2.readResolveCalled, "readResolve not called");
        }
    }

    @Test
    static void hasStaticInitializer() {
        boolean actual = factory.hasStaticInitializerForSerialization(Ser.class);
        Assert.assertTrue(actual, "hasStaticInitializerForSerialization is wrong");
    }

    static class Ser implements Serializable {
        private static final long serialVersionUID = 2L;
        static {
            // Define a static class initialization method
        }

        boolean readObjectCalled = false;
        boolean readObjectNoDataCalled = false;
        boolean writeObjectCalled = false;
        boolean readResolveCalled = false;
        boolean writeReplaceCalled = false;

        public Ser() {}

        private void readObject(ObjectInputStream ois) throws IOException {
            Assert.assertFalse(writeObjectCalled, "readObject called too many times");
            readObjectCalled = ois.readBoolean();
        }

        private void readObjectNoData(ObjectInputStream ois) throws IOException {
            Assert.assertFalse(readObjectNoDataCalled, "readObjectNoData called too many times");
            readObjectNoDataCalled = true;
        }

        private void writeObject(ObjectOutputStream oos) throws IOException {
            Assert.assertFalse(writeObjectCalled, "writeObject called too many times");
            writeObjectCalled = true;
            oos.writeBoolean(writeObjectCalled);
        }

        private Object writeReplace() {
            Assert.assertFalse(writeReplaceCalled, "writeReplace called too many times");
            writeReplaceCalled = true;
            return this;
        }

        private Object readResolve() {
            Assert.assertFalse(readResolveCalled, "readResolve called too many times");
            readResolveCalled = true;
            return this;
        }
    }

    /**
     * Test the constructor of OptionalDataExceptions.
     */
    @Test
    static void newOptionalDataException() {
        OptionalDataException ode = factory.newOptionalDataExceptionForSerialization(true);
        Assert.assertTrue(ode.eof, "eof wrong");
        ode = factory.newOptionalDataExceptionForSerialization(false);
        Assert.assertFalse(ode.eof, "eof wrong");

    }



    // Main can be used to run the tests from the command line with only testng.jar.
    @SuppressWarnings("raw_types")
    @Test(enabled = false)
    public static void main(String[] args) {
        Class<?>[] testclass = {ReflectionFactoryTest.class};
        TestNG testng = new TestNG();
        testng.setTestClasses(testclass);
        testng.run();
    }
}
