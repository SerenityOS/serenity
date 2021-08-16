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
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputFilter;
import java.io.ObjectInputFilter.Config;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.io.SerializablePermission;
import java.security.AccessControlException;
import java.security.Permission;
import java.util.function.BinaryOperator;

/* @test
 * @build SerialFilterFactoryTest
 * @run testng/othervm SerialFilterFactoryTest
 * @run testng/othervm -Djdk.serialFilterFactory=SerialFilterFactoryTest$PropertyFilterFactory
 *                     -Djava.util.logging.config.file=${test.src}/logging.properties SerialFilterFactoryTest
 * @run testng/othervm -Djdk.serialFilterFactory=SerialFilterFactoryTest$NotMyFilterFactory
 *                     -Djava.util.logging.config.file=${test.src}/logging.properties SerialFilterFactoryTest
 * @run testng/othervm/policy=security.policy
 *        -Djava.security.properties=${test.src}/java.security-extra-factory
 *        -Djava.security.debug=properties SerialFilterFactoryTest
 * @run testng/othervm/policy=security.policy SerialFilterFactoryTest
 * @run testng/othervm/policy=security.policy.without.globalFilter SerialFilterFactoryTest
 *
 * @summary Test Context-specific Deserialization Filters
 */
@Test
public class SerialFilterFactoryTest {

    // A stream with just the header, enough to create a OIS
    private static final byte[] simpleStream = simpleStream();
    private static final Validator v1 = new Validator("v1");
    private static final Validator v2 = new Validator("v2");
    private static final BinaryOperator<ObjectInputFilter> jdkSerialFilterFactory
            = Config.getSerialFilterFactory();
    private static final MyFilterFactory contextFilterFactory = new MyFilterFactory("DynFF");
    private static final String jdkSerialFilterFactoryProp = System.getProperty("jdk.serialFilterFactory");

    /**
     * Return a byte array with a simple stream containing an Dummy object.
     * @return  a byte with a simple serialization object
     */
    private static byte[] simpleStream() {
        ByteArrayOutputStream boas = new ByteArrayOutputStream();
        try (ObjectOutputStream ois = new ObjectOutputStream(boas)) {
            ois.writeObject(new Dummy("Here"));
            return boas.toByteArray();
        } catch (IOException ioe) {
            Assert.fail("unexpected IOE", ioe);
        }
        throw new RuntimeException("should not reach here");
    }

    /**
     * Initialize the filter factory, supplying one if not already set.
     * Does not/can not replace any MyFilterFactory.
     *
     * @param dynFilterFactory a filter factory to use if not already set
     * @return the filter factory in effect
     */
    private static MyFilterFactory setupFilterFactory(MyFilterFactory dynFilterFactory) {
        if ((Config.getSerialFilterFactory() instanceof MyFilterFactory ff))
            return ff;
        Config.setSerialFilterFactory(dynFilterFactory);
        return dynFilterFactory;
    }

    // If the configured filter has not been set, set it
    // It can only be set once for the process, so avoid setting it again
    private static ObjectInputFilter setupFilter(ObjectInputFilter serialFilter) {

        var configFilter = Config.getSerialFilter();
        if (configFilter == serialFilter || configFilter instanceof Validator)
            return configFilter;        // if already set or a type we can use, no change

        if (configFilter == null) {
            Config.setSerialFilter(serialFilter);
            return serialFilter;        // none set already, set it
        }

        return configFilter;
    }

    private static boolean isValidFilterFactory() {
        return !(ObjectInputFilter.Config.getSerialFilterFactory() instanceof NotMyFilterFactory);
    }

    /**
     * Returns true if serialFilter actions are ok, either no SM or SM has serialFilter Permission
     */
    @SuppressWarnings("removal")
    private static boolean hasFilterPerm() {
        boolean hasSerialPerm = true;
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            try {
                Permission p = new SerializablePermission("serialFilter");
                sm.checkPermission(p);
                hasSerialPerm = true;
            } catch (AccessControlException ace2) {
                hasSerialPerm = false;      // SM and serialFilter not allowed
            }
        }
        return hasSerialPerm;
    }

    @DataProvider(name="FilterCases")
    static Object[][] filterCases() {
        if (isValidFilterFactory()) {
            return new Object[][]{
                    {contextFilterFactory, null, null},  // no overrides
                    {contextFilterFactory, v1, null},    // context filter
                    {contextFilterFactory, v1, v2},      // per stream filter
            };
        } else {
            // There are zero cases to run with an unknown filter factory. (NotMyFilterFactory)
            return new Object[0][0];
        }
    }

    // Setting the filter factory to null is not allowed.
    @Test(expectedExceptions=NullPointerException.class)
    void testNull() {
        Config.setSerialFilterFactory(null);
    }

    /**
     * Setting and resetting the filter factory is not allowed.
     * The filter factory may have been on the command line (depending on which @run this is).
     * If the jdk.SerialFilterFactory is the built-in filter factory, set it once.
     * Try to set it again, the second should throw.
     */
    @Test
    @SuppressWarnings("removal")
    void testSecondSetShouldThrow() {
        if (System.getSecurityManager() != null) {
            // Skip test when running with SM
            return;
        }
        var currFF = Config.getSerialFilterFactory();
        if (currFF.getClass().getClassLoader() == null) {
            try {
                // Not already set, set it
                Config.setSerialFilterFactory(contextFilterFactory);
                currFF = contextFilterFactory;
            } catch (IllegalStateException ise) {
                Assert.fail("First setSerialFilterFactory should not throw");
            }
        }
        // Setting it again will throw
        Assert.expectThrows(IllegalStateException.class,
                () -> Config.setSerialFilterFactory(new MyFilterFactory("f11")));
        var resetFF = Config.getSerialFilterFactory();
        Assert.assertEquals(resetFF, currFF, "Setting again should not change filter factory");
    }

    /**
     * Test that the filter factory is set when expected and is called when expected.
     * This test only covers the cases when a filter factory is supplied
     * either via a command line property or via the API.
     * The cases where the builtin filter factory applies are tested in SerialFilterTest.
     *
     * @param dynFilterFactory a FilterFactory to set
     * @param dynFilter a serial filter to be used for the configured filter
     * @param streamFilter a serial filter to be used for the stream filter
     * @throws IOException if an I/O error occurs (should not occur)
     * @throws ClassNotFoundException for class not found (should not occur)
     */
    @Test(dataProvider="FilterCases")
    @SuppressWarnings("removal")
    void testCase(MyFilterFactory dynFilterFactory, Validator dynFilter, Validator streamFilter)
                throws IOException, ClassNotFoundException {

        // Set the Filter Factory and System-wide filter
        ObjectInputFilter configFilter;
        MyFilterFactory factory;
        try {
            configFilter = setupFilter(dynFilter);
            factory = setupFilterFactory(dynFilterFactory);
            Assert.assertTrue(hasFilterPerm(),
                    "setSerialFilterFactory and setFilterFactory succeeded without serialFilter permission");
        } catch (AccessControlException ace) {
            Assert.assertFalse(hasFilterPerm(),
                    "setSerialFilterFactory failed even with serialFilter permission");
            return;         // test complete
        }
        factory.reset();

        InputStream is = new ByteArrayInputStream(simpleStream);
        ObjectInputStream ois = new ObjectInputStream(is);

        Assert.assertNull(factory.current(), "initially current should be null");
        Assert.assertEquals(factory.next(), configFilter, "initially next should be the configured filter");
        var currFilter = ois.getObjectInputFilter();
        if (currFilter != null && currFilter.getClass().getClassLoader() == null) {
            // Builtin loader;  defaults to configured filter
            Assert.assertEquals(currFilter, configFilter, "getObjectInputFilter should be configured filter");
        } else {
            Assert.assertEquals(currFilter, configFilter, "getObjectInputFilter should be null");
        }
        if (streamFilter != null) {
            ois.setObjectInputFilter(streamFilter);
            // MyFilterFactory is called when the stream filter is changed; verify values passed it
            Assert.assertEquals(factory.current(), currFilter, "when setObjectInputFilter, current should be current filter");
            Assert.assertEquals(factory.next(), streamFilter, "next should be stream specific filter");

            // Check the OIS filter after the factory has updated it.
            currFilter = ois.getObjectInputFilter();
            Assert.assertEquals(currFilter, streamFilter, "getObjectInputFilter should be set");

            // Verify that it can not be set again
            Assert.assertThrows(IllegalStateException.class, () -> ois.setObjectInputFilter(streamFilter));
        }
        if (currFilter instanceof Validator validator) {
            validator.reset();
            Object o = ois.readObject();       // Invoke only for the side effect of calling the Filter
            Assert.assertEquals(validator.count, 1, "Wrong number of calls to the stream filter");
        } else {
            Object o = ois.readObject();       // Invoke only for the side effect of calling the filter
        }
    }

    // Test that if the property jdk-serialFilterFactory is set, then initial factory has the same classname
    @Test
    void testPropertyFilterFactory() {
        if (jdkSerialFilterFactoryProp != null) {
            Assert.assertEquals(jdkSerialFilterFactory.getClass().getName(), jdkSerialFilterFactoryProp,
                    "jdk.serialFilterFactory property classname mismatch");
        }
    }

    // Test that setting the filter factory after any deserialization (any testCase)
    // throws IllegalStateException with the specific message
    @Test(dependsOnMethods="testCase")
    void testSetFactoryAfterDeserialization() {
        if (hasFilterPerm()) {
            // Only test if is allowed by SM.
            BinaryOperator<ObjectInputFilter> factory = Config.getSerialFilterFactory();
            IllegalStateException ise = Assert.expectThrows(IllegalStateException.class, () -> Config.setSerialFilterFactory(factory));
            Assert.assertTrue(ise.getMessage().startsWith("Cannot replace filter factory: "));
        }
    }


    // Test that OIS.setObjectInputFilter does not allow a null filter to replace
    // a non-null filter.  And does allow a null filter to replace a null filter
    @Test
    void testDisableFailFilter() throws IOException {
        if (hasFilterPerm()) {
            // Only test if is allowed by SM.
            ObjectInputFilter curr = null;
            try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(simpleStream))) {
                curr = ois.getObjectInputFilter();
                // Try to set the filter to null
                ois.setObjectInputFilter(null);
                if (curr != null) {
                    Assert.fail("setting filter to null after a non-null filter should throw");
                }
            } catch (IllegalStateException ise) {
                if (curr == null) {
                    Assert.fail("setting filter to null after a null filter should not throw");
                }
            }
        }
    }

    /**
     * A simple filter factory that retains its arguments.
     */
    private static class MyFilterFactory
            implements BinaryOperator<ObjectInputFilter> {
        private final String name;
        private ObjectInputFilter current;
        private ObjectInputFilter next;

        MyFilterFactory(String name) {
            this.name = name;
            current = new Validator("UnsetCurrent");
            next = new Validator("UnsetNext");
        }

        public ObjectInputFilter apply(ObjectInputFilter curr, ObjectInputFilter next) {
            this.current = curr;
            this.next = next;
            if (curr == null & next == null)
                return Config.getSerialFilter();    // Default to the configured filter
            return next;
        }

        public void  reset() {
            current = new Validator("UnsetCurrent");
            next = new Validator("UnsetNext");
        }

        public ObjectInputFilter current() {
            return current;
        }

        public ObjectInputFilter next() {
            return next;
        }

        public String toString() {
            return name + ":: curr: " + current + ", next: " + next;
        }
    }

    /**
     * A subclass of MyFilterFactory with a name, used when testing setting the factory using
     * -Djdk.setFilterFactory.
     */
    public static class PropertyFilterFactory extends MyFilterFactory {
        public PropertyFilterFactory() {
            super("UNNAMED");
        }
    }

    /**
     * A filter factory that is not compatible with MyFilterFactory test.
     * Used for testing incorrect initialization.
     */
    public static class NotMyFilterFactory
            implements BinaryOperator<ObjectInputFilter> {

        public NotMyFilterFactory() {}

        /**
         * Returns null as the filter to be used for an ObjectInputStream.
         *
         * @param curr the current filter, if any
         * @param next the next filter, if any
         * @return null
         */
        public ObjectInputFilter apply(ObjectInputFilter curr, ObjectInputFilter next) {
            return null;
        }
    }

    /**
     * A filter that accumulates information about the checkInput callbacks
     * that can be checked after readObject completes.
     */
    static class Validator implements ObjectInputFilter {
        private final String name;
        long count;          // Count of calls to checkInput

        Validator(String name) {
            this.name = name;
            count = 0;
        }

        void reset() {
            count = 0;
        }

        @Override
        public Status checkInput(FilterInfo filter) {
            count++;
            return Status.ALLOWED;
        }

        public String toString(){
            return name + ": count: " + count;
        }
    }

    /**
     * A simple class to serialize.
     */
    private static final class Dummy implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        final String s;
        Dummy(String s) {
            this.s = s;
        }
        public String toString() {
            return this.getClass().getName() + "::" + s;
        }
    }
}
