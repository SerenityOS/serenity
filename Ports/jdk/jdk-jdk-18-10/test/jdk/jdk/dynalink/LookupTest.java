/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import jdk.dynalink.linker.support.Lookup;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @test
 * @run testng LookupTest
 */
public class LookupTest {
    private static final MethodHandles.Lookup MY_LOOKUP = MethodHandles.lookup();

    private static MethodHandles.Lookup getLookup(final boolean publicLookup) {
        return publicLookup? MethodHandles.publicLookup() : MY_LOOKUP;
    }

    // test constructors, methods used for lookup
    @SuppressWarnings("unused")
    public LookupTest() {}

    @SuppressWarnings("unused")
    private LookupTest(final int unused) {}

    @SuppressWarnings("unused")
    private void privateFunc() {}

    @SuppressWarnings("unused")
    protected void protectedFunc() {}

    @SuppressWarnings("unused")
    private static void privateStaticFunc() {}

    @SuppressWarnings("unused")
    private final int myIntField = 0;

    @SuppressWarnings("unused")
    @DataProvider
    private static Object[][] flags() {
        return new Object[][]{
            {Boolean.FALSE},
            {Boolean.TRUE}
        };
    }

    @Test(dataProvider = "flags")
    public void unreflectTest(final boolean publicLookup) throws NoSuchMethodException {
        final MethodHandle mh = Lookup.unreflect(getLookup(publicLookup), LookupTest.class.getMethod("unreflectTest", Boolean.TYPE));
        Assert.assertNotNull(mh);
    }

    @Test
    public void unreflectTest2() throws NoSuchMethodException {
        final MethodHandle mh = Lookup.PUBLIC.unreflect(LookupTest.class.getMethod("unreflectTest", Boolean.TYPE));
        Assert.assertNotNull(mh);
    }

    @Test(dataProvider = "flags")
    public void unreflectNegativeTest(final boolean publicLookup) throws NoSuchMethodException {
        try {
            final MethodHandle mh = Lookup.unreflect(getLookup(publicLookup),
                LookupTest.class.getDeclaredMethod("privateFunc"));
            if (publicLookup) {
                throw new RuntimeException("should have thrown Error");
            }
            Assert.assertNotNull(mh);
        } catch (final Error err) {
            Assert.assertTrue(publicLookup);
            Assert.assertTrue(err instanceof NoSuchMethodError || err instanceof IllegalAccessError);
        }
    }

    @Test
    public void unreflectNegativeTest2() throws NoSuchMethodException {
        try {
            Lookup.PUBLIC.unreflect(LookupTest.class.getDeclaredMethod("privateFunc"));
            throw new RuntimeException("should have thrown Error");
        } catch (final Error err) {
            Assert.assertTrue(err instanceof NoSuchMethodError || err instanceof IllegalAccessError);
        }
    }

    @Test(dataProvider = "flags")
    public void unreflectConstructorTest(final boolean publicLookup) throws NoSuchMethodException {
        final MethodHandle mh = Lookup.unreflectConstructor(getLookup(publicLookup), LookupTest.class.getConstructor());
        Assert.assertNotNull(mh);
    }

    @Test
    public void unreflectConstructorTest2() throws NoSuchMethodException {
        final MethodHandle mh = Lookup.PUBLIC.unreflectConstructor(LookupTest.class.getConstructor());
        Assert.assertNotNull(mh);
    }

    @Test(dataProvider = "flags")
    public void unreflectConstructorNegativeTest(final boolean publicLookup) throws NoSuchMethodException {
        try {
            final MethodHandle mh = Lookup.unreflectConstructor(getLookup(publicLookup),
                LookupTest.class.getDeclaredConstructor(Integer.TYPE));
            if (publicLookup) {
                throw new RuntimeException("should have thrown Error");
            }
            Assert.assertNotNull(mh);
        } catch (final Error err) {
            Assert.assertTrue(publicLookup);
            Assert.assertTrue(err instanceof NoSuchMethodError || err instanceof IllegalAccessError);
        }
    }

    @Test
    public void unreflectConstructorNegativeTest2() throws NoSuchMethodException {
        try {
            Lookup.PUBLIC.unreflectConstructor(
                LookupTest.class.getDeclaredConstructor(Integer.TYPE));
            throw new RuntimeException("should have thrown Error");
        } catch (final Error err) {
            Assert.assertTrue(err instanceof NoSuchMethodError || err instanceof IllegalAccessError);
        }
    }

    @Test(dataProvider = "flags")
    public void findOwnStaticTest(final boolean publicLookup) {
        try {
            final MethodHandle mh = Lookup.findOwnStatic(getLookup(publicLookup), "getLookup",
                    MethodHandles.Lookup.class, Boolean.TYPE);
            if (publicLookup) {
                throw new RuntimeException("should have thrown Error");
            }
            Assert.assertNotNull(mh);
        } catch (final Error err) {
            Assert.assertTrue(publicLookup);
            Assert.assertTrue(err instanceof NoSuchMethodError || err instanceof IllegalAccessError);
        }
    }

    @Test
    public void findOwnStaticTest2() {
        try {
            Lookup.PUBLIC.findStatic(LookupTest.class, "getLookup",
                    MethodType.methodType(MethodHandles.Lookup.class, Boolean.TYPE));
            throw new RuntimeException("should have thrown Error");
        } catch (final Error err) {
            Assert.assertTrue(err instanceof NoSuchMethodError || err instanceof IllegalAccessError);
        }
    }

    @Test(dataProvider = "flags")
    public void findOwnSepcialTest(final boolean publicLookup) {
        try {
            final MethodHandle mh = Lookup.findOwnSpecial(getLookup(publicLookup), "privateFunc", Void.TYPE);
            if (publicLookup) {
                throw new RuntimeException("should have thrown Error");
            }
            Assert.assertNotNull(mh);
        } catch (final Error err) {
            Assert.assertTrue(publicLookup);
            Assert.assertTrue(err instanceof NoSuchMethodError || err instanceof IllegalAccessError);
        }
    }

    @Test
    public void findOwnSepcialTest2() {
        try {
            Lookup.PUBLIC.findOwnSpecial("privateFunc", Void.TYPE);
            throw new RuntimeException("should have thrown Error");
        } catch (final Error err) {
            Assert.assertTrue(err instanceof NoSuchMethodError || err instanceof IllegalAccessError);
        }
    }

    @Test(dataProvider = "flags")
    public void findGetterTest(final boolean publicLookup) {
        try {
            final MethodHandle mh = new Lookup(getLookup(publicLookup)).findGetter(LookupTest.class, "myIntField", Integer.TYPE);
            if (publicLookup) {
                throw new RuntimeException("should have thrown Error");
            }
            Assert.assertNotNull(mh);
        } catch (final Error err) {
            Assert.assertTrue(publicLookup);
            Assert.assertTrue(err instanceof NoSuchMethodError || err instanceof IllegalAccessError);
        }
    }

    @Test
    public void findGetterTest2() {
        try {
            Lookup.PUBLIC.findGetter(LookupTest.class, "myIntField", Integer.TYPE);
            throw new RuntimeException("should have thrown Error");
        } catch (final Error err) {
            Assert.assertTrue(err instanceof NoSuchMethodError || err instanceof IllegalAccessError);
        }
    }

    @Test(dataProvider = "flags")
    public void findVirtualTest(final boolean publicLookup) {
        try {
            final MethodHandle mh = new Lookup(getLookup(publicLookup)).findVirtual(LookupTest.class, "protectedFunc",
                    MethodType.methodType(Void.TYPE));
            if (publicLookup) {
                throw new RuntimeException("should have thrown Error");
            }
            Assert.assertNotNull(mh);
        } catch (final Error err) {
            Assert.assertTrue(publicLookup);
            Assert.assertTrue(err instanceof NoSuchMethodError || err instanceof IllegalAccessError);
        }
    }

    @Test
    public void findVirtualTest2() {
        try {
            Lookup.PUBLIC.findVirtual(LookupTest.class, "protectedFunc",
                    MethodType.methodType(Void.TYPE));
            throw new RuntimeException("should have thrown Error");
        } catch (final Error err) {
            Assert.assertTrue(err instanceof NoSuchMethodError || err instanceof IllegalAccessError);
        }
    }

    @Test(dataProvider = "flags")
    public void findStaticTest(final boolean publicLookup) {
        try {
            final MethodHandle mh = new Lookup(getLookup(publicLookup)).findStatic(LookupTest.class, "privateStaticFunc",
                    MethodType.methodType(Void.TYPE));
            if (publicLookup) {
                throw new RuntimeException("should have thrown Error");
            }
            Assert.assertNotNull(mh);
        } catch (final Error err) {
            Assert.assertTrue(publicLookup);
            Assert.assertTrue(err instanceof NoSuchMethodError || err instanceof IllegalAccessError);
        }
    }

    @Test
    public void findStaticTest2() {
        try {
            Lookup.PUBLIC.findStatic(LookupTest.class, "privateStaticFunc",
                    MethodType.methodType(Void.TYPE));
            throw new RuntimeException("should have thrown Error");
        } catch (final Error err) {
            Assert.assertTrue(err instanceof NoSuchMethodError || err instanceof IllegalAccessError);
        }
    }
}
