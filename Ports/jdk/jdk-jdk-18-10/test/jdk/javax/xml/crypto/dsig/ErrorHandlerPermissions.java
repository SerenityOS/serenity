/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import javax.xml.XMLConstants;
import javax.xml.crypto.Data;
import javax.xml.crypto.KeySelector;
import javax.xml.crypto.OctetStreamData;
import javax.xml.crypto.URIDereferencer;
import javax.xml.crypto.URIReference;
import javax.xml.crypto.URIReferenceException;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * @test
 * @bug 8079140
 * @summary Check if IgnoreAllErrorHandler doesn't require additional permission
 * @run main/othervm/java.security.policy=ErrorHandlerPermissions.policy
 *                                                      ErrorHandlerPermissions
 */
public class ErrorHandlerPermissions {

    private final static String FS = System.getProperty("file.separator");
    private final static String DIR = System.getProperty("test.src", ".");
    private final static String DATA_DIR = DIR + FS + "data";
    private final static String SIGNATURE = DATA_DIR + FS +
            "signature-external-rsa.xml";

    private static final String validationKey =
        "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCnx4TdvPSA5vcsPi0OJZi9Ox0Z" +
        "2FRz2oeUCtuWoyEg0kUCeFd+jJZMstDJUiZNSOeuCO3FWSpdJgAwI4zlveHvuU/o" +
        "qHSa1eYTObOCvxfVYGGflWsSvGXyiANtRWVUrYODBeyL+2pWxDYh+Fi5EKizPfTG" +
        "wRjBVRSkRZKTnSjnQwIDAQAB";

    private static final URIDereferencer dereferencer =
            new DummyURIDereferencer();

    public static void main(String[] args) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setValidating(false);
        dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, Boolean.TRUE);
        Document doc = dbf.newDocumentBuilder().parse(new File(SIGNATURE));
        NodeList nl = doc.getElementsByTagNameNS(XMLSignature.XMLNS,
                "Signature");
        if (nl.getLength() == 0) {
            throw new RuntimeException("Couldn't find 'Signature' element");
        }
        Element element = (Element) nl.item(0);

        byte[] keyBytes = Base64.getDecoder().decode(validationKey);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PublicKey key = kf.generatePublic(spec);
        KeySelector ks = KeySelector.singletonKeySelector(key);

        DOMValidateContext vc = new DOMValidateContext(ks, element);

        // disable secure validation mode
        vc.setProperty("org.jcp.xml.dsig.secureValidation", Boolean.FALSE);

        // set a dummy dereferencer to be able to get content by references
        vc.setURIDereferencer(dereferencer);

        XMLSignatureFactory factory = XMLSignatureFactory.getInstance();
        XMLSignature signature = factory.unmarshalXMLSignature(vc);

        // run validation
        signature.validate(vc);
    }

    /**
     * This URIDereferencer returns a static XML document.
     */
    private static class DummyURIDereferencer implements URIDereferencer {

        @Override
        public Data dereference(final URIReference ref, XMLCryptoContext ctx)
                throws URIReferenceException {
            // return static content
            return new OctetStreamData(new ByteArrayInputStream(
                    "<test>test</test>".getBytes()), ref.getURI(),
                    ref.getType());
        }
    }

}
