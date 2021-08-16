/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8151893 8259709
 * @summary Tests for the jdk.xml.dsig.secureValidationPolicy security property
 * @modules java.xml.crypto/org.jcp.xml.dsig.internal.dom
 */

import java.security.Security;
import java.util.List;
import org.jcp.xml.dsig.internal.dom.Policy;

public class SecureValidationPolicy {

    public static void main(String[] args) throws Exception {

        List<String> restrictedSchemes = List.of("file:/tmp/foo",
            "http://java.com", "https://java.com");
        List<String> restrictedAlgs = List.of(
            "http://www.w3.org/TR/1999/REC-xslt-19991116",
            "http://www.w3.org/2001/04/xmldsig-more#rsa-md5",
            "http://www.w3.org/2001/04/xmldsig-more#hmac-md5",
            "http://www.w3.org/2001/04/xmldsig-more#md5",
            "http://www.w3.org/2000/09/xmldsig#sha1",
            "http://www.w3.org/2000/09/xmldsig#dsa-sha1",
            "http://www.w3.org/2000/09/xmldsig#rsa-sha1",
            "http://www.w3.org/2007/05/xmldsig-more#sha1-rsa-MGF1",
            "http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha1");

        // Test expected defaults
        System.out.println("Testing defaults");
        if (!Policy.restrictNumTransforms(6)) {
            throw new Exception("maxTransforms not enforced");
        }
        if (!Policy.restrictNumReferences(31)) {
            throw new Exception("maxReferences not enforced");
        }
        for (String scheme : restrictedSchemes) {
            if (!Policy.restrictReferenceUriScheme(scheme)) {
                throw new Exception(scheme + " scheme not restricted");
            }
        }
        for (String alg : restrictedAlgs) {
            if (!Policy.restrictAlg(alg)) {
                throw new Exception(alg + " alg not restricted");
            }
        }
        if (!Policy.restrictDuplicateIds()) {
            throw new Exception("noDuplicateIds not enforced");
        }
        if (!Policy.restrictRetrievalMethodLoops()) {
            throw new Exception("noRetrievalMethodLoops not enforced");
        }
    }
}
