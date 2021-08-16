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

package stream.EventsTest;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.events.StartDocument;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow stream.EventsTest.Issue53Test
 * @run testng/othervm stream.EventsTest.Issue53Test
 * @summary Test encodingSet/standaloneSet returns correct result in case encoding/standalone is set when constructing StartDocument.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class Issue53Test {

    @Test
    public void testEncodingSet() {
        XMLEventFactory f = XMLEventFactory.newInstance();

        try {
            StartDocument sd = f.createStartDocument("UTF-8");
            System.out.println("Encoding: " + sd.getCharacterEncodingScheme());
            System.out.println("Encoding set: " + sd.encodingSet());
            Assert.assertTrue(sd.encodingSet(), "encoding is set, should return true.");
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }

    }

    @Test
    public void testStandaloneSet() {
        XMLEventFactory f = XMLEventFactory.newInstance();

        try {
            StartDocument sd = f.createStartDocument("UTF-8", "1.0", true);
            System.out.println(sd.isStandalone());
            System.out.println(sd.standaloneSet());
            Assert.assertTrue(sd.standaloneSet(), "standalone is set, should return true.");
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }

    }

}
