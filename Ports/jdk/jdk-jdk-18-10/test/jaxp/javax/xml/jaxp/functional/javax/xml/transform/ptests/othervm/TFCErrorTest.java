/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
package javax.xml.transform.ptests.othervm;

import static jaxp.library.JAXPTestUtilities.setSystemProperty;

import static org.testng.Assert.fail;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/**
 * Negative test for set invalid TransformerFactory property.
 */
/*
 * @test
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow javax.xml.transform.ptests.othervm.TFCErrorTest
 * @run testng/othervm javax.xml.transform.ptests.othervm.TFCErrorTest
 */
@Listeners({jaxp.library.BasePolicy.class})
public class TFCErrorTest {
    @Test(expectedExceptions = ClassNotFoundException.class)
    public void tfce01() throws Exception {
        try{
            setSystemProperty("javax.xml.transform.TransformerFactory","xx");
            TransformerFactory.newInstance();
            fail("Expect TransformerFactoryConfigurationError here");
        } catch (TransformerFactoryConfigurationError expected) {
            throw expected.getException();
        }
    }
}
