/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

package sax;

import java.util.Enumeration;

import org.testng.Assert;
import org.testng.AssertJUnit;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.xml.sax.helpers.NamespaceSupport;

/*
 * @test
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow sax.NSSupportTest
 * @run testng/othervm sax.NSSupportTest
 * @summary Test NamespaceSupport.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class NSSupportTest {

    @Test
    public void testProcessName() {
        NamespaceSupport nssupport = new NamespaceSupport();

        nssupport.pushContext();
        nssupport.declarePrefix("", "http://www.java.com");
        nssupport.declarePrefix("dc", "http://www.purl.org/dc");

        String[] parts = new String[3];
        nssupport.processName("dc:name1", parts, false);
        Assert.assertTrue(parts[0].equals("http://www.purl.org/dc"));
        Assert.assertTrue(parts[1].equals("name1"));
        Assert.assertTrue(parts[2].equals("dc:name1"));

        nssupport.processName("name2", parts, false);
        Assert.assertTrue(parts[0].equals("http://www.java.com"));
        Assert.assertTrue(parts[1].equals("name2"));
        Assert.assertTrue(parts[2].equals("name2"));
    }

    @Test
    public void testNamespaceDeclUris() {
        String[] parts = new String[3];
        NamespaceSupport nssupport = new NamespaceSupport();

        nssupport.pushContext();
        Assert.assertFalse(nssupport.isNamespaceDeclUris());
        nssupport.declarePrefix("xmlns", "");
        nssupport.processName("xmlns:name", parts, true);
        Assert.assertNull(parts[0]);
        Assert.assertNull(parts[1]);
        Assert.assertNull(parts[2]);

        nssupport.reset();

        nssupport.setNamespaceDeclUris(true);
        nssupport.declarePrefix("xmlns", "");
        nssupport.processName("xmlns:name", parts, true);
        Assert.assertTrue(parts[0].equals(NamespaceSupport.NSDECL));
        Assert.assertTrue(parts[1].equals("name"));
        Assert.assertTrue(parts[2].equals("xmlns:name"));

        nssupport.reset();

        nssupport.setNamespaceDeclUris(true);
        nssupport.declarePrefix("xml", "");
        nssupport.processName("xml:name", parts, true);
        Assert.assertTrue(parts[0].equals(NamespaceSupport.XMLNS));
        Assert.assertTrue(parts[1].equals("name"));
        Assert.assertTrue(parts[2].equals("xml:name"));

    }

    @Test
    public void testPopContext() {
        String[] parts = new String[3];
        NamespaceSupport nssupport = new NamespaceSupport();

        nssupport.pushContext();
        nssupport.declarePrefix("dc", "http://www.purl.org/dc");
        Assert.assertEquals(nssupport.getPrefix("http://www.purl.org/dc"), "dc");

        nssupport.popContext();
        Assert.assertNull(nssupport.getPrefix("http://www.purl.org/dc"));
        nssupport.processName("dc:name1", parts, false);
        Assert.assertNull(parts[0]);
        Assert.assertNull(parts[1]);
        Assert.assertNull(parts[2]);
    }

    @Test
    public void testPrefixAndUri1() {
        boolean hasdc = false;
        boolean hasdc1 = false;
        boolean hasdc2 = false;
        boolean hasdcnew = false;
        NamespaceSupport nssupport = new NamespaceSupport();

        nssupport.pushContext();
        nssupport.declarePrefix("dc", "http://www.purl.org/dc");

        nssupport.pushContext();
        nssupport.declarePrefix("dc1", "http://www.purl.org/dc");
        nssupport.declarePrefix("dc2", "http://www.purl.org/dc2");
        nssupport.declarePrefix("dcnew", "http://www.purl.org/dcnew");

        Enumeration enu1 = nssupport.getDeclaredPrefixes();
        while (enu1.hasMoreElements()) {
            String str = (String) enu1.nextElement();
            if (str.equals("dc")) {
                hasdc = true;
            } else if (str.equals("dc1")) {
                hasdc1 = true;
            } else if (str.equals("dc2")) {
                hasdc2 = true;
            } else if (str.equals("dcnew")) {
                hasdcnew = true;
            }
        }
        AssertJUnit.assertTrue(hasdcnew && hasdc1 && hasdc2);
        AssertJUnit.assertFalse(hasdc);
    }

    @Test
    public void testPrefixAndUri2() {
        boolean hasdc = false;
        boolean hasdc1 = false;
        boolean hasdc2 = false;
        boolean hasdcnew = false;
        NamespaceSupport nssupport = new NamespaceSupport();

        nssupport.pushContext();
        nssupport.declarePrefix("dc", "http://www.purl.org/dc");

        nssupport.pushContext();
        nssupport.declarePrefix("dc1", "http://www.purl.org/dc");
        nssupport.declarePrefix("dc2", "http://www.purl.org/dc2");
        nssupport.declarePrefix("dcnew", "http://www.purl.org/dcnew");

        Enumeration enu1 = nssupport.getPrefixes();
        while (enu1.hasMoreElements()) {
            String str = (String) enu1.nextElement();
            if (str.equals("dc")) {
                hasdc = true;
            } else if (str.equals("dc1")) {
                hasdc1 = true;
            } else if (str.equals("dc2")) {
                hasdc2 = true;
            } else if (str.equals("dcnew")) {
                hasdcnew = true;
            }
        }
        AssertJUnit.assertTrue(hasdcnew && hasdc1 && hasdc2 && hasdc);
    }

    @Test
    public void testPrefixAndUri3() {
        boolean hasdc = false;
        boolean hasdc1 = false;
        boolean hasdc2 = false;
        boolean hasdcnew = false;
        NamespaceSupport nssupport = new NamespaceSupport();

        nssupport.pushContext();
        nssupport.declarePrefix("dc", "http://www.purl.org/dc");

        nssupport.pushContext();
        nssupport.declarePrefix("dc1", "http://www.purl.org/dc");
        nssupport.declarePrefix("dc2", "http://www.purl.org/dc2");
        nssupport.declarePrefix("dcnew", "http://www.purl.org/dcnew");

        Enumeration enu1 = nssupport.getPrefixes("http://www.purl.org/dc");
        while (enu1.hasMoreElements()) {
            String str = (String) enu1.nextElement();
            if (str.equals("dc")) {
                hasdc = true;
            } else if (str.equals("dc1")) {
                hasdc1 = true;
            } else if (str.equals("dc2")) {
                hasdc2 = true;
            } else if (str.equals("dcnew")) {
                hasdcnew = true;
            }
        }
        AssertJUnit.assertTrue(hasdc1 && hasdc);
        AssertJUnit.assertFalse(hasdc2);
        AssertJUnit.assertFalse(hasdcnew);
    }

    @Test
    public void testPrefixAndUri4() {
        NamespaceSupport nssupport = new NamespaceSupport();

        nssupport.pushContext();
        nssupport.declarePrefix("dc", "http://www.purl.org/dc");

        nssupport.pushContext();
        nssupport.declarePrefix("dc1", "http://www.purl.org/dc");
        nssupport.declarePrefix("dc2", "http://www.purl.org/dc2");
        nssupport.declarePrefix("dcnew", "http://www.purl.org/dcnew");

        AssertJUnit.assertTrue(nssupport.getURI("dc").equals("http://www.purl.org/dc"));
        AssertJUnit.assertTrue(nssupport.getURI("dc1").equals("http://www.purl.org/dc"));
        AssertJUnit.assertTrue(nssupport.getURI("dc2").equals("http://www.purl.org/dc2"));
        AssertJUnit.assertTrue(nssupport.getURI("dcnew").equals("http://www.purl.org/dcnew"));

        // Negative test
        Assert.assertNull(nssupport.getURI("wrong_prefix"));
        Assert.assertNull(nssupport.getURI(""));
    }
}
