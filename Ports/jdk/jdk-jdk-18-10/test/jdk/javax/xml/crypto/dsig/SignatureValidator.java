/*
 * Copyright (c) 2005, 2006, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.util.*;
import java.security.MessageDigest;
import javax.xml.crypto.*;
import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dom.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;

/**
 * This is a class which performs xml signature validation upon request
 */
class SignatureValidator {

    private File dir;

    SignatureValidator(File base) {
        dir = base;
    }

    boolean validate(String fn, KeySelector ks, boolean cache)
        throws Exception {
        return validate(fn, ks, null, cache);
    }

    boolean validate(String fn, KeySelector ks, URIDereferencer ud,
        boolean cache) throws Exception {

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setValidating(false);
        Document doc = dbf.newDocumentBuilder().parse(new File(dir, fn));
        NodeList nl =
            doc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
        if (nl.getLength() == 0) {
            throw new Exception("Couldn't find signature Element");
        }
        Element sigElement = (Element) nl.item(0);
        DOMValidateContext vc = new DOMValidateContext(ks, sigElement);
        vc.setBaseURI(dir.toURI().toString());
        if (cache) {
            vc.setProperty("javax.xml.crypto.dsig.cacheReference", Boolean.TRUE);
        }
        XMLSignatureFactory factory = XMLSignatureFactory.getInstance();
        XMLSignature signature = factory.unmarshalXMLSignature(vc);
        if (ud != null) {
            vc.setURIDereferencer(ud);
        }
        boolean coreValidity = signature.validate(vc);

        // Check reference cache
        if (cache) {
            Iterator<Reference> i =
                signature.getSignedInfo().getReferences().iterator();
            for (int j = 0; i.hasNext(); j++) {
                Reference ref = i.next();
                if (!digestInputEqual(ref)) {
                    throw new Exception
                        ("cached data for Reference[" + j + "] is not correct");
                }
                // check that dereferenced data does not contain comment nodes
                if (ref.getURI() == "") {
                    System.out.println("checking deref data");
                    @SuppressWarnings("unchecked")
                    NodeSetData<Node> data =
                        (NodeSetData<Node>)ref.getDereferencedData();
                    for (Node n : data) {
                        if (n.getNodeType() == Node.COMMENT_NODE) {
                            throw new Exception("dereferenced data for " +
                                " Reference[" + j + " contains comment node");
                        }
                    }
                }
            }
        }
        return coreValidity;
    }

    private boolean digestInputEqual(Reference ref) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA1");
        InputStream is = ref.getDigestInputStream();
        int nbytes;
        byte[] buf = new byte[256];
        while ((nbytes = is.read(buf, 0, buf.length)) != -1) {
            md.update(buf, 0, nbytes);
        }
        return Arrays.equals(md.digest(), ref.getDigestValue());
    }
}
