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
package test.rowset.spi;

import com.sun.rowset.providers.RIOptimisticProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.Context;
import javax.sql.rowset.spi.SyncFactory;
import javax.sql.rowset.spi.SyncFactoryException;
import javax.sql.rowset.spi.SyncProvider;
import static org.testng.Assert.*;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import util.PropertyStubProvider;
import util.StubSyncProvider;
import util.StubContext;

//com.sun.jndi.ldap.LdapCtxFactory
public class SyncFactoryTests {
    private static String origFactory;

    private final String stubProvider = "util.StubSyncProvider";
    private final String propertyStubProvider = "util.PropertyStubProvider";
    private final Logger alogger = Logger.getLogger(this.getClass().getName());
    // Initial providers including those set via a property
    List<String> initialProviders;
    // All providers including those specifically registered
    List<String> allProviders;
    private  Context ctx= null;

    public SyncFactoryTests() {

        // Add a provider via a property
        System.setProperty("rowset.provider.classname", propertyStubProvider);
        initialProviders = Arrays.asList(
                "com.sun.rowset.providers.RIOptimisticProvider",
                "com.sun.rowset.providers.RIXMLProvider",
                propertyStubProvider);
        allProviders = new ArrayList<>();
        allProviders.addAll(initialProviders);
        allProviders.add(stubProvider);
        ctx = new StubContext();
    }

    @BeforeMethod
    public void setUpMethod() throws Exception {
        // Make sure the provider provider that is registered is removed
        // before each run
        SyncFactory.unregisterProvider(stubProvider);
    }

    /*
     * Validate a non-null factory is returned
     */
    @Test
    public void test() throws SyncFactoryException {
        SyncFactory syncFactory = SyncFactory.getSyncFactory();
        assertTrue(syncFactory != null);
    }

    /*
     * Check that the correct SyncProvider is returned for the specified
     * providerID for the provider registered via a property
     */
    @Test
    public void test00() throws SyncFactoryException {
        SyncProvider p = SyncFactory.getInstance(propertyStubProvider);
        assertTrue(p instanceof PropertyStubProvider);
    }

    /*
     * Check that the correct SyncProvider is returned for the specified
     * providerID
     */
    @Test
    public void test01() throws SyncFactoryException {
        SyncFactory.registerProvider(stubProvider);
        SyncProvider p = SyncFactory.getInstance(stubProvider);
        assertTrue(p instanceof StubSyncProvider);
    }

    /*
     * Check that the Default SyncProvider is returned if an empty String is
     * passed or if an invalid providerID is specified
     */
    @Test
    public void test02() throws SyncFactoryException {
        SyncProvider p = SyncFactory.getInstance("");
        assertTrue(p instanceof RIOptimisticProvider);
        // Attempt to get an invalid provider and get the default provider
        p = SyncFactory.getInstance("util.InvalidSyncProvider");
        assertTrue(p instanceof RIOptimisticProvider);
    }

    /*
     * Validate that a SyncFactoryException is thrown if the ProviderID is null
     */
    @Test(expectedExceptions = SyncFactoryException.class)
    public void test03() throws SyncFactoryException {
        SyncProvider p = SyncFactory.getInstance(null);
    }

    /*
     * Validate that a SyncFactoryException is thrown if the Logger is null
     */
    @Test(expectedExceptions = SyncFactoryException.class,enabled=true)
    public void test04() throws SyncFactoryException {
        Logger l = SyncFactory.getLogger();
    }

    /*
     * Validate that the correct logger is returned by getLogger
     */
    @Test
    public void test05() throws SyncFactoryException {
        SyncFactory.setLogger(alogger);
        Logger l = SyncFactory.getLogger();
        assertTrue(l.equals(alogger));
    }

    /*
     * Validate that the correct logger is returned by getLogger
     */
    @Test
    public void test06() throws SyncFactoryException {
        SyncFactory.setLogger(alogger, Level.INFO);
        Logger l = SyncFactory.getLogger();
        assertTrue(l.equals(alogger));
    }

    /*
     *  Validate that a driver that is registered is returned by
     * getRegisteredProviders and  if it is unregistered, that it is
     * not returned by getRegisteredProviders
     */
    @Test
    public void test07() throws SyncFactoryException {

        // Validate that only the default providers and any specified via
        // a System property are available
        validateProviders(initialProviders);

        // Register a provider and make sure it is avaiable
        SyncFactory.registerProvider(stubProvider);
        validateProviders(allProviders);

        // Check that if a provider is unregistered, it does not show as
        // registered
        SyncFactory.unregisterProvider(stubProvider);
        validateProviders(initialProviders);
    }

    /*
     * Validate that setJNDIContext throws a SyncFactoryException if the
     * context is null
     */
    @Test(expectedExceptions = SyncFactoryException.class, enabled=true)
    public void test08() throws Exception {
        SyncFactory.setJNDIContext(null);
    }

    /*
     * Validate that setJNDIContext succeeds
     */
    @Test(enabled=true)
    public void test09() throws Exception {
        SyncFactory.setJNDIContext(ctx);
    }

    /*
     * Utility method to validate the expected providers are regsitered
     */
    private void validateProviders(List<String> expectedProviders)
            throws SyncFactoryException {
        List<String> results = new ArrayList<>();
        Enumeration<SyncProvider> providers = SyncFactory.getRegisteredProviders();

        while (providers.hasMoreElements()) {
            SyncProvider p = providers.nextElement();
            results.add(p.getProviderID());
        }
        assertTrue(expectedProviders.containsAll(results)
                && results.size() == expectedProviders.size());
    }

    /*
     * Utility method to dump out SyncProvider info for a registered provider
     */
    private void showImpl(SyncProvider impl) {
        System.out.println("Provider implementation:"
                + "\nVendor: " + impl.getVendor()
                + "\nVersion: " + impl.getVersion()
                + "\nProviderID: " + impl.getProviderID());
    }
}
