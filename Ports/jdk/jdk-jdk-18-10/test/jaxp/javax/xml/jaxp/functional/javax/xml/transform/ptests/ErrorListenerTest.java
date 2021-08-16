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
import static org.testng.Assert.fail;

import java.io.File;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/**
 * Class containing the test cases for ErrorListener interface
 */
/*
 * @test
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow javax.xml.transform.ptests.ErrorListenerTest
 * @run testng/othervm javax.xml.transform.ptests.ErrorListenerTest
 */
@Listeners({jaxp.library.FilePolicy.class})
public class ErrorListenerTest implements ErrorListener {
    /**
     * Define ErrorListener's status.
     */
    private static enum ListenerStatus{NOT_INVOKED, ERROR, WARNING, FATAL};

    /**
     * No ErrorListener invoked at the beginning.
     */
    private volatile ListenerStatus status = ListenerStatus.NOT_INVOKED;

    /**
     * Expect a TransformerConfigurationException when transforming a file
     * invalid.xsl that has some well-formedness error.
     */
    @Test
    public void errorListener01() {
        ErrorListenerTest listener = new ErrorListenerTest();
        try {
            TransformerFactory tfactory = TransformerFactory.newInstance();
            tfactory.setErrorListener (listener);
            tfactory.newTransformer(new StreamSource(
                                        new File(XML_DIR + "invalid.xsl")));
            fail("Expect TransformerConfigurationException here");
        } catch (TransformerConfigurationException ex) {
            assertEquals(listener.status, ListenerStatus.FATAL);
        }
    }

    /**
     * Set status as ERROR when receiving notification of a recoverable error.
     * @param e The error information encapsulated in a transformer exception.
     */
    @Override
    public void error (TransformerException e) {
        this.status = ListenerStatus.ERROR;
    }

    /**
     * Set status as WARNING when receiving notification of a warning.
     * @param e The error information encapsulated in a transformer exception.
     */
    @Override
    public void warning (TransformerException e) {
        this.status = ListenerStatus.WARNING;
    }

    /**
     * Set status as FATAL when receiving notification of a non-recoverable error.
     * @param e The error information encapsulated in a transformer exception.
     */
    @Override
    public void fatalError (TransformerException e) {
        this.status = ListenerStatus.FATAL;
    }
}
