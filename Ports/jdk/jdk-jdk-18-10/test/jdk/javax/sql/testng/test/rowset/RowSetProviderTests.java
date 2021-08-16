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
package test.rowset;

import com.sun.rowset.RowSetFactoryImpl;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.SQLException;
import javax.sql.rowset.RowSetFactory;
import javax.sql.rowset.RowSetProvider;
import static org.testng.Assert.*;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import util.BaseTest;
import util.StubRowSetFactory;

public class RowSetProviderTests extends BaseTest {

    // Default RowSetFactory Implementation
    private final String DEFFAULT_FACTORY_CLASSNAME = "com.sun.rowset.RowSetFactoryImpl";
    // Stub RowSetFactory Implementation
    private final String STUB_FACTORY_CLASSNAME = "util.StubRowSetFactory";
    // Indicator that the factory implementation does not need to be checked
    private final String NO_VALADATE_IMPL = "";
    // Original System property value for javax.sql.rowset.RowSetFactory
    private static String origFactoryProperty;
    // Original ClassLoader
    private static ClassLoader cl;
    // Path to the location of the jar files used by the ServiceLoader API
    private static String jarPath;

    /*
     * Save off the original property value for javax.sql.rowset.RowSetFactory,
     * original classloader and define the path to the jars directory
     */
    @BeforeClass
    public static void setUpClass() throws Exception {
        origFactoryProperty = System.getProperty("javax.sql.rowset.RowSetFactory");
        cl = Thread.currentThread().getContextClassLoader();
        jarPath = System.getProperty("test.src", ".") + File.separatorChar
                + "jars" +  File.separatorChar;
    }

    /*
     * Install the original javax.sql.rowset.RowSetFactory property value
     */
    @AfterClass
    public static void tearDownClass() throws Exception {
        if (origFactoryProperty != null) {
            System.setProperty("javax.sql.rowset.RowSetFactory",
                    origFactoryProperty);
        }
    }

    /*
     * Clear the javax.sql.rowset.RowSetFactory property value and
     * reset the classloader to its original value
     */
    @AfterMethod
    public void tearDownMethod() throws Exception {
        System.clearProperty("javax.sql.rowset.RowSetFactory");
        Thread.currentThread().setContextClassLoader(cl);
    }

    /*
     * Validate that the correct RowSetFactory is returned by newFactory().
     */
    @Test(dataProvider = "RowSetFactoryValues")
    public void test(RowSetFactory rsf, String impl) throws SQLException {
        validateProvider(rsf, impl);
    }

    /*
     * Validate that the default RowSetFactory is returned by newFactory()
     * when specified by the javax.sql.rowset.RowSetFactory property.
     */
    @Test
    public void test01() throws SQLException {
        System.setProperty("javax.sql.rowset.RowSetFactory",
                DEFFAULT_FACTORY_CLASSNAME);
        validateProvider(RowSetProvider.newFactory(), DEFFAULT_FACTORY_CLASSNAME);
    }

    /*
     * Validate that the correct RowSetFactory is returned by newFactory()
     * when specified by the javax.sql.rowset.RowSetFactory property.
     */
    @Test(enabled = true)
    public void test02() throws SQLException {
        System.setProperty("javax.sql.rowset.RowSetFactory", STUB_FACTORY_CLASSNAME);
        validateProvider(RowSetProvider.newFactory(), STUB_FACTORY_CLASSNAME);
    }

    /*
     * Validate that a SQLException is thrown by newFactory()
     * when specified  RowSetFactory specified by the
     * javax.sql.rowset.RowSetFactory property is not valid.
     */
    @Test(expectedExceptions = SQLException.class)
    public void test03() throws SQLException {
        System.setProperty("javax.sql.rowset.RowSetFactory",
                "invalid.RowSetFactoryImpl");
        RowSetFactory rsf = RowSetProvider.newFactory();
    }

    /*
     * Validate that the correct RowSetFactory is returned by newFactory()
     * when specified by the ServiceLoader API.
     */
    @Test
    public void test04() throws Exception {
        File f = new File(jarPath + "goodFactory");
        URLClassLoader loader = new URLClassLoader(new URL[]{
            new URL(f.toURI().toString())}, getClass().getClassLoader());
        Thread.currentThread().setContextClassLoader(loader);
        validateProvider(RowSetProvider.newFactory(), STUB_FACTORY_CLASSNAME);
    }

    /*
     * Validate that a SQLException is thrown by newFactory() if the default
     * RowSetFactory specified by the ServiceLoader API is not valid
     */
    @Test(expectedExceptions = SQLException.class)
    public void test05() throws Exception {
        File f = new File(jarPath + "badFactory");
        URLClassLoader loader = new URLClassLoader(new URL[]{
            new URL(f.toURI().toString())}, getClass().getClassLoader());
        Thread.currentThread().setContextClassLoader(loader);
        RowSetProvider.newFactory();
    }

    /*
     * Utility Method to validate that the RowsetFactory returned from
     * RowSetProvider.newFactory() is correct
     */
    private void validateProvider(RowSetFactory rsf, String implName) {
        assertNotNull(rsf, "RowSetFactory should not be null");
        switch (implName) {
            case DEFFAULT_FACTORY_CLASSNAME:
                assertTrue(rsf instanceof RowSetFactoryImpl);
                break;
            case STUB_FACTORY_CLASSNAME:
                assertTrue(rsf instanceof StubRowSetFactory);
                break;
            default:
        }
    }

    /*
     * DataProvider used to provide a RowSetFactory and the expected
     * RowSetFactory implementation that should be returned
     */
    @DataProvider(name = "RowSetFactoryValues")
    private Object[][] RowSetFactoryValues() throws SQLException {
        RowSetFactory rsf = RowSetProvider.newFactory();
        RowSetFactory rsf1 = RowSetProvider.newFactory(STUB_FACTORY_CLASSNAME, null);
        RowSetFactory rsf2 = RowSetProvider.newFactory(DEFFAULT_FACTORY_CLASSNAME, null);
        return new Object[][]{
            {rsf, NO_VALADATE_IMPL},
            {rsf, DEFFAULT_FACTORY_CLASSNAME},
            {rsf1, STUB_FACTORY_CLASSNAME},
            {rsf2, DEFFAULT_FACTORY_CLASSNAME}
        };
    }
}
