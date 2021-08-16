/*
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 6372500
 * @summary Test that KeyInfo.marshal works correctly
 * @modules java.xml.crypto/org.jcp.xml.dsig.internal.dom
 * @compile -XDignore.symbol.file Marshal.java
 * @run main/othervm/java.security.policy==test.policy Marshal
 * @author Sean Mullan
 */

import java.util.Collections;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.crypto.dom.DOMStructure;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.jcp.xml.dsig.internal.dom.DOMUtils;

public class Marshal {

    public static void main(String[] args) throws Exception {
        KeyInfoFactory fac = KeyInfoFactory.getInstance();
        KeyInfo ki = fac.newKeyInfo
            (Collections.singletonList(fac.newKeyName("foo")), "keyid");
        try {
            ki.marshal(null, null);
            throw new Exception("Should raise a NullPointerException");
        } catch (NullPointerException npe) {}

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Document doc = dbf.newDocumentBuilder().newDocument();
        Element elem = doc.createElementNS("http://acme.org", "parent");
        doc.appendChild(elem);
        DOMStructure parent = new DOMStructure(elem);
        ki.marshal(parent, null);

        Element kiElem = DOMUtils.getFirstChildElement(elem);
        if (!kiElem.getLocalName().equals("KeyInfo")) {
            throw new Exception
                ("Should be KeyInfo element: " + kiElem.getLocalName());
        }
        Element knElem = DOMUtils.getFirstChildElement(kiElem);
        if (!knElem.getLocalName().equals("KeyName")) {
            throw new Exception
                ("Should be KeyName element: " + knElem.getLocalName());
        }
    }
}
