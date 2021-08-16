/*
 * Copyright (c) 2003, 2016, Oracle and/or its affiliates. All rights reserved.
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
package org.xml.sax.ptests;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import java.util.Enumeration;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * Unit test cases for NamespaceSupport API
 */
/*
 * @test
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow org.xml.sax.ptests.NSSupportTest
 * @run testng/othervm org.xml.sax.ptests.NSSupportTest
 */
@Listeners({jaxp.library.BasePolicy.class})
public class NSSupportTest {
    /**
     * Empty prefix name.
     */
    private final static String EMPTY_PREFIX = "";

    /**
     * A URI for W3 1999 HTML sepc.
     */
    private final static String W3_URI = "http://www.w3.org/1999/xhtml";

    /**
     * A prefix named "dc".
     */
    private final static String DC_PREFIX = "dc";

    /**
     * A URI for "http://www.purl.org/dc#".
     */
    private final static String PURL_URI = "http://www.purl.org/dc#";

    /**
     * Test for NamespaceSupport.getDeclaredPrefixes().
     */
    @Test
    public void testcase01() {
        String[] prefixes = new String[2];
        NamespaceSupport support = new NamespaceSupport();
        support.pushContext();
        support.declarePrefix(EMPTY_PREFIX, W3_URI);
        support.declarePrefix(DC_PREFIX, PURL_URI);

        Enumeration e = support.getDeclaredPrefixes();
        int i = 0;
        while(e.hasMoreElements()) {
            prefixes[i++] = e.nextElement().toString();
        }
        support.popContext();

        assertEquals(prefixes, new String[]{EMPTY_PREFIX, DC_PREFIX});
    }

    /**
     * Test for NamespaceSupport.getDeclaredPrefixes() and support.processName().
     */
    @Test
    public void testcase02() {
        String[] parts = new String[3];
        NamespaceSupport support = new NamespaceSupport();

        support.pushContext();
        support.declarePrefix(DC_PREFIX, PURL_URI);
        parts = support.processName("dc:title", parts, false);
        support.popContext();
        assertEquals(parts, new String[]{PURL_URI, "title", "dc:title"});
    }

    /**
     * Test for NamespaceSupport.getDeclaredPrefixes() and support.processName().
     */
    @Test
    public void testcase03() {
        String[] parts = new String[3];
        NamespaceSupport support = new NamespaceSupport();
        support.pushContext();
        support.declarePrefix(EMPTY_PREFIX, W3_URI);
        parts = support.processName("a", parts, false);
        support.popContext();
        assertEquals(parts, new String[]{W3_URI, "a", "a"});
    }


    /**
     * Test for NamespaceSupport.popContext().
     */
    @Test
    public void testcase04() {
        NamespaceSupport support = new NamespaceSupport();

        support.pushContext();
        support.declarePrefix(EMPTY_PREFIX, W3_URI);
        support.declarePrefix(DC_PREFIX, PURL_URI);

        assertEquals(support.getURI(EMPTY_PREFIX), W3_URI);
        assertEquals(support.getURI(DC_PREFIX), PURL_URI);
        support.popContext();
        assertNull(support.getURI(EMPTY_PREFIX));
        assertNull(support.getURI(DC_PREFIX));
    }
}
