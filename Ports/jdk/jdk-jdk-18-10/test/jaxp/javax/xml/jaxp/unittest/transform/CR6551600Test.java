/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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

package transform;

import java.io.File;
import java.io.FilePermission;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import jaxp.library.JAXPTestUtilities;

/*
 * @test
 * @bug 6551600
 * @requires os.family == "windows"
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow transform.CR6551600Test
 * @run testng/othervm transform.CR6551600Test
 * @summary Test using UNC path as StreamResult.
 */
@Listeners({ jaxp.library.BasePolicy.class })
public class CR6551600Test {

    @Test
    public final void testUNCPath() {
        var hostName = "";
        try {
            hostName = java.net.InetAddress.getLocalHost().getHostName();
        } catch (java.net.UnknownHostException e) {
            // falls through
        }

        var uncPath = "\\\\" + hostName + "\\C$\\temp\\";

        if (!checkAccess(uncPath)) {
            System.out.println("Cannot access UNC path. Test exits.");
            return;
        }

        var uncFilePath = uncPath + "xslt_unc_test.xml";
        JAXPTestUtilities.runWithTmpPermission(() -> {
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.newDocument();
                Element root = doc.createElement("test");
                doc.appendChild(root);
                // create an identity transform
                Transformer t = TransformerFactory.newInstance().newTransformer();
                File f = new File(uncFilePath);
                StreamResult result = new StreamResult(f);
                DOMSource source = new DOMSource(doc);
                System.out.println("Writing to " + f);
                t.transform(source, result);
            } catch (Exception e) {
                // unexpected failure
                e.printStackTrace();
                Assert.fail(e.toString());
            }

            File file = new File(uncFilePath);
            if (file.exists()) {
                file.deleteOnExit();
            }
        }, new FilePermission(uncFilePath, "read,write,delete"));
    }

    private boolean checkAccess(String path) {
        return JAXPTestUtilities.runWithTmpPermission(() -> {
            try {
                Path tmepFile = Files.createTempFile(Paths.get(path), "test", "6551600");
                Files.deleteIfExists(tmepFile);
                return true;
            } catch (Exception e) {
                System.out.println("Access check failed.");
                e.printStackTrace();
                return false;
            }
        }, new FilePermission(path + "*", "read,write,delete"));
    }
}
