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

package xpath;

import static jaxp.library.JAXPTestUtilities.runWithAllPerm;

import javax.xml.xpath.XPathFactory;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 6354969
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow xpath.ClassLoaderTest
 * @run testng/othervm xpath.ClassLoaderTest
 * @summary Test XPathFactory newInstance() with ContextClassLoader.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class ClassLoaderTest {

    @Test
    public void testClassLoader() {
        MyClassLoader cl = runWithAllPerm(() -> new MyClassLoader());
        runWithAllPerm(() -> Thread.currentThread().setContextClassLoader(cl));
        XPathFactory xPathFactory = XPathFactory.newInstance();

        if (!cl.isCalled()) {
            Assert.fail("Context class loader should be used.");
        }

    }
}
