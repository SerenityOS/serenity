/*
 * Copyright (c) 2002, 2016, Oracle and/or its affiliates. All rights reserved.
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
package test.astro;

import static javax.xml.XMLConstants.DEFAULT_NS_PREFIX;
import static javax.xml.XMLConstants.NULL_NS_URI;
import static org.testng.Assert.assertEquals;

import javax.xml.namespace.QName;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow test.astro.NamespaceContextTest
 * @run testng/othervm test.astro.NamespaceContextTest
 * @summary javax.xml.namespace.QName tests
 */
@Listeners({jaxp.library.BasePolicy.class})
public class NamespaceContextTest {
    private static final String PREFIX = "astro";
    private static final String LOCAL_PART = "stardb";
    private static final String NS_URI = "http://www.astro.com";

    /*
     * Test QName(String, String, String) and accessors.
     */
    @Test
    public void testQNameConstructor() {
        QName qname = new QName(NS_URI, LOCAL_PART, PREFIX);
        assertEquals(qname.getNamespaceURI(), NS_URI);
        assertEquals(qname.getLocalPart(), LOCAL_PART);
        assertEquals(qname.getPrefix(), PREFIX);
    }

    /*
     * Construct QName(String localpart), then test for default ns_uri and
     * prefix constant.
     */
    @Test
    public void testDefaultFields() {
        QName qname = new QName(LOCAL_PART); // just the local part specified
        assertEquals(qname.getNamespaceURI(), NULL_NS_URI);
        assertEquals(qname.getLocalPart(), LOCAL_PART);
        assertEquals(qname.getPrefix(), DEFAULT_NS_PREFIX);
    }

    /*
     * Construct QName(String ns,String localpart), then test for default prefix
     * constant.
     */
    @Test
    public void testDefaultPrefix() {
        QName qname = new QName(NS_URI, LOCAL_PART); // no pref
        assertEquals(qname.getNamespaceURI(), NS_URI);
        assertEquals(qname.getLocalPart(), LOCAL_PART);
        assertEquals(qname.getPrefix(), DEFAULT_NS_PREFIX);
    }

    /*
     * Round trip testing of QName to String, String to QName and test for
     * equality.
     */
    @Test
    public void testQNameString() {
        QName qname = new QName(NS_URI, LOCAL_PART, PREFIX);
        assertEquals(QName.valueOf(qname.toString()), qname);
    }
}
