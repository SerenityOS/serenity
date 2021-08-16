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
package javax.xml.transform.ptests;

import static javax.xml.transform.ptests.TransformerTestConst.XML_DIR;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.fail;

import java.io.File;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/**
 *  Basic test for TransformerException specification.
 */
/*
 * @test
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow javax.xml.transform.ptests.TransformerExcpTest
 * @run testng/othervm javax.xml.transform.ptests.TransformerExcpTest
 */
@Listeners({jaxp.library.FilePolicy.class})
public class TransformerExcpTest {
    /**
     * Transform an unformatted style-sheet file. TransformerException is thrown.
     */
    @Test
    public void tfexception() {
        try {
            // invalid.xsl has well-formedness error. Therefore transform throws
            // TransformerException
            StreamSource streamSource
                    = new StreamSource(new File(XML_DIR + "invalid.xsl"));
            TransformerFactory tFactory = TransformerFactory.newInstance();
            Transformer transformer = tFactory.newTransformer(streamSource);
            transformer.transform(
                    new StreamSource(new File(XML_DIR + "cities.xml")),
                    new SAXResult());
            fail("TransformerException is not thrown as expected");
        } catch (TransformerException e) {
            assertNotNull(e.getCause());
            assertNotNull(e.getException());
            assertNull(e.getLocationAsString());
            assertEquals(e.getMessageAndLocation(),e.getMessage());
        }
    }


    /**
     * Spec says, "if the throwable was created with
     * TransformerException(Throwable), initCause should throw
     * IllegalStateException
     */
    @Test(expectedExceptions = IllegalStateException.class)
    public void tfexception06() {
        TransformerException te = new TransformerException(new Throwable());
        te.initCause(null);
    }

    /**
     * Spec says, "if the throwable was created with TransformerException(String,
     * Throwable), initCause should throw IllegalStateException
     */
    @Test(expectedExceptions = IllegalStateException.class)
    public void tfexception07() {
        TransformerException te = new TransformerException("MyMessage", new Throwable());
        te.initCause(null);
    }

    /**
     * Tests if initCause(null) is allowed in other case.
     */
    @Test
    public void tfexception08() {
        TransformerException te = new TransformerException("My Message");
        assertNotNull(te.initCause(null));
    }
}
