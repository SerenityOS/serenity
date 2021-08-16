/*
 * Copyright (c) 2006, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6436919 6460930
 * @summary check that XML Signatures can be generated and validated with
 *  SecurityManager enabled and default policy
 * @run main/othervm -Djava.security.manager=allow XMLDSigWithSecMgr
 * @author Sean Mullan
 */
import java.io.*;
import java.net.*;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.ArrayList;
import java.util.Collections;
import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class XMLDSigWithSecMgr implements Runnable {

    private XMLSignatureFactory fac;
    private DigestMethod sha1;
    private CanonicalizationMethod withoutComments;
    private DocumentBuilder db;

    private ServerSocket ss;

    private void setup() throws Exception {
        ss = new ServerSocket(0);
        Thread thr = new Thread(this);
        thr.start();

        fac = XMLSignatureFactory.getInstance();
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        db = dbf.newDocumentBuilder();
        sha1 = fac.newDigestMethod(DigestMethod.SHA1, null);
        withoutComments = fac.newCanonicalizationMethod
            (CanonicalizationMethod.INCLUSIVE, (C14NMethodParameterSpec)null);
    }

    public void run() {
        try {

        for (int i=0; i<2; i++) {
            Socket s = ss.accept();
            s.setTcpNoDelay(true);

            PrintStream out = new PrintStream(
                                 new BufferedOutputStream(
                                    s.getOutputStream() ));

            out.print("HTTP/1.1 200 OK\r\n");
            out.print("Content-Length: 11\r\n");
            out.print("Content-Type: text/plain\r\n");
            out.print("\r\n");
            out.print("l;ajfdjafd\n");
            out.flush();

            // don't close the connection immediately as otherwise
            // the http headers may not have been received and the
            // http client will re-connect.
            Thread.currentThread().sleep(2000);

            s.close();
        }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    XMLDSigWithSecMgr() throws Exception {
        setup();
        Document doc = db.newDocument();
        Element envelope = doc.createElementNS
            ("http://example.org/envelope", "Envelope");
        envelope.setAttributeNS("http://www.w3.org/2000/xmlns/",
            "xmlns", "http://example.org/envelope");
        doc.appendChild(envelope);

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        KeyPair kp = kpg.genKeyPair();

        // the policy only grants this test SocketPermission to accept, resolve
        // and connect to localhost so that it can dereference 2nd reference
        System.setProperty("java.security.policy",
                System.getProperty("test.src", ".") + File.separator + "policy");
        System.setSecurityManager(new SecurityManager());

        try {
            // generate a signature with SecurityManager enabled
            ArrayList refs = new ArrayList();
            refs.add(fac.newReference
                ("", sha1,
                 Collections.singletonList
                    (fac.newTransform(Transform.ENVELOPED,
                     (TransformParameterSpec) null)), null, null));
            refs.add(fac.newReference("http://localhost:" + ss.getLocalPort()
                + "/anything.txt", sha1));
            SignedInfo si = fac.newSignedInfo(withoutComments,
                fac.newSignatureMethod(SignatureMethod.RSA_SHA1, null), refs);
            XMLSignature sig = fac.newXMLSignature(si, null);
            DOMSignContext dsc = new DOMSignContext(kp.getPrivate(), envelope);
            sig.sign(dsc);

            // validate a signature with SecurityManager enabled
            DOMValidateContext dvc = new DOMValidateContext
                (kp.getPublic(), envelope.getFirstChild());

            // disable secure validation mode so that http reference will work
            dvc.setProperty("org.jcp.xml.dsig.secureValidation", Boolean.FALSE);

            sig = fac.unmarshalXMLSignature(dvc);
            if (!sig.validate(dvc)) {
                throw new Exception
                    ("XMLDSigWithSecMgr signature validation FAILED");
            }
        } catch (SecurityException se) {
            throw new Exception("XMLDSigWithSecMgr FAILED", se);
        }
        ss.close();
    }

    public static void main(String[] args) throws Exception {
        new XMLDSigWithSecMgr();
    }
}
