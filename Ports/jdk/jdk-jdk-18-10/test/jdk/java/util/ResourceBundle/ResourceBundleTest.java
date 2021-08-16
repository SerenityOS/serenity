/*
 * Copyright (c) 2007, 2017, Oracle and/or its affiliates. All rights reserved.
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
    @test
    @bug 4049325 4073127 4083270 4106034 4108126 8027930 8171139
    @summary test Resource Bundle
    @build TestResource TestResource_de TestResource_fr TestResource_fr_CH
    @build TestResource_it FakeTestResource
    @run main ResourceBundleTest
*/
/*
 *
 *
 * (C) Copyright Taligent, Inc. 1996, 1997 - All Rights Reserved
 * (C) Copyright IBM Corp. 1996 - 1998 - All Rights Reserved
 *
 * Portions copyright (c) 2007 Sun Microsystems, Inc.
 * All Rights Reserved.
 *
 * The original version of this source code and documentation
 * is copyrighted and owned by Taligent, Inc., a wholly-owned
 * subsidiary of IBM. These materials are provided under terms
 * of a License Agreement between Taligent and Sun. This technology
 * is protected by multiple US and International patents.
 *
 * This notice and attribution to Taligent may not be removed.
 * Taligent is a registered trademark of Taligent, Inc.
 *
 * Permission to use, copy, modify, and distribute this software
 * and its documentation for NON-COMMERCIAL purposes and without
 * fee is hereby granted provided that this copyright notice
 * appears in all copies. Please refer to the file "copyright.html"
 * for further important copyright and licensing information.
 *
 * SUN MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF
 * THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE, OR NON-INFRINGEMENT. SUN SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES.
 *
 */

import java.text.*;
import java.util.*;
import java.util.ResourceBundle.Control;
import java.io.*;
import java.net.URL;

public class ResourceBundleTest extends RBTestFmwk {
    public static void main(String[] args) throws Exception {
        new ResourceBundleTest().run(args);
    }

    public ResourceBundleTest() {
        makePropertiesFile();
    }

    public void TestResourceBundle() {
        Locale  saveDefault = Locale.getDefault();
        Locale.setDefault(new Locale("fr", "FR"));

        // load up the resource bundle, and make sure we got the right one
        ResourceBundle  bundle = ResourceBundle.getBundle("TestResource");
        if (!bundle.getClass().getName().equals("TestResource_fr"))
            errln("Expected TestResource_fr, got " + bundle.getClass().getName());

        // these resources are defines in ResourceBundle_fr
        String  test1 = bundle.getString("Time");
        if (!test1.equals("Time keeps on slipping..."))
            errln("TestResource_fr returned wrong value for \"Time\":  got " + test1);

        test1 = bundle.getString("For");
        if (!test1.equals("Four score and seven years ago..."))
            errln("TestResource_fr returned wrong value for \"For\":  got " + test1);

        String[] test2 = bundle.getStringArray("All");
        if (test2.length != 4)
            errln("TestResource_fr returned wrong number of elements for \"All\": got " + test2.length);
        else if (!test2[0].equals("'Twas brillig, and the slithy toves") ||
                 !test2[1].equals("Did gyre and gimble in the wabe.") ||
                 !test2[2].equals("All mimsy were the borogoves,") ||
                 !test2[3].equals("And the mome raths outgrabe."))
            errln("TestResource_fr returned the wrong value for one of the elements in \"All\"");

        Object  test3 = bundle.getObject("Good");
        if (test3 == null || test3.getClass() != Integer.class)
            errln("TestResource_fr returned an object of the wrong class for \"Good\"");
        else if (((Integer)test3).intValue() != 3)
            errln("TestResource_fr returned the wrong value for \"Good\": got " + test3);

        // This resource is defined in TestResource and inherited by TestResource_fr
        test2 = bundle.getStringArray("Men");
        if (test2.length != 3)
            errln("TestResource_fr returned wrong number of elements for \"Men\": got " + test2.length);
        else if (!test2[0].equals("1") ||
                 !test2[1].equals("2") ||
                 !test2[2].equals("C"))
            errln("TestResource_fr returned the wrong value for one of the elements in \"All\"");

        // This resource is defined in neither TestResource not TestResource_fr
        try {
            test3 = bundle.getObject("Is");
            errln("TestResource_fr returned a value for \"Is\" when it shouldn't: got " + test3);
        }
        catch (MissingResourceException e) {
        }

        String[] keys = { "Now", "Time", "For", "All", "Good", "Men", "Come" };
        checkKeys(bundle.getKeys(),  keys);

        Locale.setDefault(saveDefault);
    }

    public void TestListResourceBundle() {
        // load up the resource and check to make sure we got the right class
        // (we don't define be_BY or be, so we fall back on the root default)
        ResourceBundle  bundle = ResourceBundle.getBundle("TestResource",
                            new Locale("be", "BY"),
                            Control.getNoFallbackControl(Control.FORMAT_DEFAULT));
        if (!bundle.getClass().getName().equals("TestResource"))
            errln("Expected TestResource, got " + bundle.getClass().getName());

        doListResourceBundleTest(bundle);
    }

    /**
     * @bug 4073127
     * Repeat TestListResourceBundle on TestResource_it, which is a ListResourceBundle
     * with NO contents.  It should gracefully inherit everything from the root
     * TestResource.
     */
    public void TestEmptyListResourceBundle() {
        ResourceBundle bundle = ResourceBundle.getBundle("TestResource",
                            new Locale("it", "IT"));
        doListResourceBundleTest(bundle);
    }

    private void doListResourceBundleTest(ResourceBundle bundle) {
        // load up the resource and check to make sure we got the right class
        // all of these resources are defined in TestResource; it doesn' inherit from anybody
        String  test1 = bundle.getString("Now");
        if (!test1.equals("Now is the time for all..."))
            errln("TestResource returned wrong value for \"Now\":  got " + test1);

        test1 = bundle.getString("Time");
        if (!test1.equals("Howdy Doody Time!"))
            errln("TestResource returned wrong value for \"Time\":  got " + test1);

        test1 = bundle.getString("Come");
        if (!test1.equals("Come into my parlor..."))
            errln("TestResource returned wrong value for \"Come\":  got " + test1);

        Object  test3 = bundle.getObject("Good");
        if (test3 == null || test3.getClass() != Integer.class)
            errln("TestResource returned an object of the wrong class for \"Good\"");
        else if (((Integer)test3).intValue() != 27)
            errln("TestResource returned the wrong value for \"Good\": got " + test3);

        String[] test2 = bundle.getStringArray("Men");
        if (test2.length != 3)
            errln("TestResource returned wrong number of elements for \"Men\": got " + test2.length);
        else if (!test2[0].equals("1") ||
                 !test2[1].equals("2") ||
                 !test2[2].equals("C"))
            errln("TestResource returned the wrong value for one of the elements in \"All\"");

        // this item isn't defined in TestResource
        try {
            test3 = bundle.getObject("All");
            errln("TestResource_en returned a value for \"All\" when it shouldn't: got " + test3);
        }
        catch (MissingResourceException e) {
        }

        String[] keys = { "Now", "Time", "Good", "Men", "Come" };
        checkKeys(bundle.getKeys(), keys);
    }

    /**
     * @bug 4049325
     * @ summary Bug 4049325 says ResourceBundle.findBundle() uses a hard-coded '/' as
     * the directory separator when searching for properties files.  Interestingly, it
     * still works on my NT installation.  I can't tell whether this is a required
     * property of all Java implementations (the magic appears to happen ClassLoader.
     * getResourceAsStream(), which is a native function) or a lucky property of my
     * particular implementation.  If this bug regresses, this test may still pass
     * because a lower-level facility translates the / to the platform-specific separator
     * for us.
     */
    public void TestPropertyResourceBundle() {
        ResourceBundle  bundle = ResourceBundle.getBundle("TestResource",
                            new Locale("es", "ES"));

        // these resources are defined in TestResource_es.properties
        String  test = bundle.getString("Now");
        if (!test.equals("How now brown cow"))
            errln("TestResource_es returned wrong value for \"Now\":  got " + test);

        test = bundle.getString("Is");
        if (!test.equals("Is there a dog?"))
            errln("TestResource_es returned wrong value for \"Is\":  got " + test);

        test = bundle.getString("The");
        if (!test.equals("The rain in Spain"))
            errln("TestResource_es returned wrong value for \"The\":  got " + test);

        test = bundle.getString("Time");
        if (!test.equals("Time marches on..."))
            errln("TestResource_es returned wrong value for \"Time\":  got " + test);

        // this resource is defined in TestResource and inherited by TestResource_es
        String[] test2 = bundle.getStringArray("Men");
        if (test2.length != 3)
            errln("TestResource returned wrong number of elements for \"Men\": got " + test2.length);
        else if (!test2[0].equals("1") ||
                 !test2[1].equals("2") ||
                 !test2[2].equals("C"))
            errln("TestResource returned the wrong value for one of the elements in \"All\"");

        // this resource is defined in neither TestResource nor TestResource_es
        try {
            test = bundle.getString("All");
            errln("TestResource_es returned a value for \"All\" when it shouldn't: got " + test);
        }
        catch (MissingResourceException e) {
        }

        String[] keys = { "Now", "Is", "The", "Time", "Good", "Men", "Come" };
        checkKeys(bundle.getKeys(), keys);
    }

    /**
     * @bug 4108126
     */
    public void TestGetLocale() {
        // try to find TestResource_fr_CH.  Should get fr_CH as its locale
        ResourceBundle test = ResourceBundle.getBundle("TestResource",
                        new Locale("fr", "CH", ""));
        Locale locale = test.getLocale();
        if (!(locale.getLanguage().equals("fr")) || !(locale.getCountry().equals("CH")))
            errln("Actual locale for TestResource_fr_CH should have been fr_CH, got " + locale);

        // try to find TestResource_fr_BE, which doesn't exist.  Should get fr as its locale
        test = ResourceBundle.getBundle("TestResource",
                        new Locale("fr", "BE", ""));
        locale = test.getLocale();
        if (!(locale.getLanguage().equals("fr")) || !(locale.getCountry().equals("")))
            errln("Actual locale for TestResource_fr_BE should have been fr, got " + locale);

        // try to find TestResource_iw_IL, which doesn't exist.  Should get root locale
        // as its locale
        test = ResourceBundle.getBundle("TestResource",
                        new Locale("iw", "IL", ""),
                        Control.getNoFallbackControl(Control.FORMAT_DEFAULT));
        locale = test.getLocale();
        if (!(locale.getLanguage().equals("")) || !(locale.getCountry().equals("")))
            errln("Actual locale for TestResource_iw_IL should have been the root locale, got "
                            + locale);
    }

    /*
     * @bug 4083270
     */
    public void TestNonSubclass() {
        // ResourceBundle.getBundle should never return an object that isn't an instance
        // of ResourceBundle or one of its subclasses.  We have a class called FakeTestResource
        // in this package that isn't a ResourceBundle.  If we get that back, we barf.
        // (Actually, at the time I fixed this bug, getResource() would throw a
        // ClassCastException in that case.)
        // There's also a properties file called FakeTestResource; we should get back a
        // PropertyResourceBundle pointing to that file.
        Object test1 = ResourceBundle.getBundle("FakeTestResource",
                Locale.US);

        if (!(test1 instanceof ResourceBundle))
            errln("Got back a " + test1.getClass().getName() + " instead of a PropertyResourceBundle when looking for FakeTestResource.");

        ResourceBundle test = (ResourceBundle)test1;

        // there's also a properties file called FakeTestResource.  getBundle() should
        // find it, and it should have the following contents
        String message = test.getString("message");
        if (!message.equals("Hello!"))
            errln("Supposedly found FakeTestResource.properties, but it had the wrong contents.");
    }

    /*
     * @bug 4106034
     */
    public void TestErrorMessage() {
        // Ensure that the message produced by the exception thrown
        // by ResourceBundle.getObject contains both the class name and
        // the key name.
        final String className = "TestResource";
        final String keyName = "DontGetThis";
        ResourceBundle bundle = ResourceBundle.getBundle(className,
                            new Locale("it", "IT"));
        try {
            Object o = bundle.getObject(keyName);
            errln(bundle.getClass().getName()+" returned a value for tag \""+keyName+"\" when it should have thrown an exception.  It returned "+o);
        } catch (MissingResourceException e) {
            String message = e.getMessage();
            boolean found = false;
            if (message.indexOf(className) < 0) {
                    errln("MissingResourceException error message did not contain class name.");
            }
            if (message.indexOf(keyName) < 0) {
                    errln("MissingResourceException error message did not contain resource key name.");
            }
        }
    }

    /*
     * @bug 8171139
     * @summary Make sure clearCache() clears cached ResourceBundle instances
     */
    public void TestClearCache() {
        final String className = "TestResource";
        Locale loc = Locale.getDefault();

        // testing no-arg clearCache()
        ResourceBundle rb1 = ResourceBundle.getBundle(className, loc);
        ResourceBundle.clearCache();
        ResourceBundle rb2 = ResourceBundle.getBundle(className, loc);
        if (rb1 == rb2) {
            errln("clearCache(no-arg) did not clear cache");
        }

        // clearCache() with a custom classloader
        ClassLoader cl1 = new DummyClassLoader();
        rb1 = ResourceBundle.getBundle(className, loc, cl1);
        if (rb1 == rb2) {
            errln("Same bundle was returned for different class loaders");
        }
        ResourceBundle.clearCache(cl1);
        rb2= ResourceBundle.getBundle(className, loc, cl1);
        if (rb1 == rb2) {
            errln("clearCache(classLoader) did not clear cache");
        }
        ClassLoader cl2 = new DummyClassLoader();
        rb1 = ResourceBundle.getBundle(className, loc, cl2);
        if (rb1 == rb2) {
            errln("Same bundle was returned for different class loaders");
        }
        ResourceBundle.clearCache(cl1);
        rb2 = ResourceBundle.getBundle(className, loc, cl2);
        if (rb1 != rb2) {
            errln("clearCache(classLoader) incorrectly cleared cache");
        }
    }

    private void makePropertiesFile() {
        try {
            //
            // The getProperty call is to ensure that this test will work with
            // the JTREG test harness.  When running in the harness, the current
            // directory is often set to someplace that isn't on the CLASSPATH,
            // so we can't just create the properties files in the current
            // directory.  But the harness uses the "test.classes" property to
            // tell us where the classes directory is.
            //
            String classesDir = System.getProperty("test.classes", ".");
            File    file = new File(classesDir, "TestResource_es.properties");
            if (!file.exists()) {
                FileOutputStream stream = new FileOutputStream(file);
                Properties  props = new Properties();

                props.put("Now", "How now brown cow");
                props.put("Is", "Is there a dog?");
                props.put("The", "The rain in Spain");
                props.put("Time", "Time marches on...");

                props.save(stream, "Test property list");

                stream.close();
            }

            file = new File(classesDir, "FakeTestResource.properties");
            if (!file.exists()) {
                FileOutputStream stream = new FileOutputStream(file);
                Properties props = new Properties();

                props.put("message", "Hello!");

                props.save(stream, "Test property list");

                stream.close();
            }
        }
        catch (java.io.IOException e) {
            errln("Got exception: " + e);
        }
    }

    private void checkKeys(Enumeration testKeys, String[] expectedKeys) {
        Hashtable   hash = new Hashtable();
        String      element;
        int         elementCount = 0;

        for (int i=0; i < expectedKeys.length; i++)
            hash.put(expectedKeys[i], expectedKeys[i]);

        while (testKeys.hasMoreElements()) {
            element = (String)testKeys.nextElement();
            elementCount++;
            if (!hash.containsKey(element))
                errln(element + " missing from key list.");
        }

        if (elementCount != expectedKeys.length)
            errln("Wrong number of elements in key list: expected " + expectedKeys.length +
                " got " + elementCount);
    }

    private static class DummyClassLoader extends ClassLoader {
        public DummyClassLoader() {
            super(DummyClassLoader.class.getClassLoader());
        }

        public Class<?> loadClass(String name) throws ClassNotFoundException {
            return DummyClassLoader.class.getClassLoader().loadClass(name);
        }

        public URL getResource(String name) {
            return DummyClassLoader.class.getClassLoader().getResource(name);
        }

        public InputStream getResourceAsStream(String name) {
            return DummyClassLoader.class.getClassLoader().getResourceAsStream(name);
        }
    }
}
