/*
 * Copyright (c) 2001, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4416946
 * @summary Make sure {CertStore,CertPathBuilder,CertPathValidator,
 * CertificateFactory}.getInstance throws InvalidAlgorithmParameterException
 * if invalid params are specified and NoSuchAlgorithmException (or
 * CertificateException for CertificateFactory) if bogus type is specified
 */
import java.security.*;
import java.security.cert.*;
import java.util.*;

public class GetInstance {

    public static void main(String[] args) throws Exception {

        CollectionCertStoreParameters ccsp
            = new CollectionCertStoreParameters(new ArrayList());
        try {
            CertStore cs = CertStore.getInstance("LDAP", ccsp);
            throw new Exception("CertStore.getInstance() should have thrown " +
                "InvalidAlgorithmParameterException");
        } catch (InvalidAlgorithmParameterException iape) { }

        try {
            CertStore cs = CertStore.getInstance("BOGUS", null);
            throw new Exception("CertStore.getInstance() should have thrown " +
                "NoSuchAlgorithmException");
        } catch (NoSuchAlgorithmException nsae) { }

        try {
            CertPathBuilder cpb = CertPathBuilder.getInstance("BOGUS");
            throw new Exception("CertPathBuilder.getInstance() should have " +
                "thrown NoSuchAlgorithmException");
        } catch (NoSuchAlgorithmException nsae) { }

        try {
            CertPathValidator cpv = CertPathValidator.getInstance("BOGUS");
            throw new Exception("CertPathValidator.getInstance() should have " +
                "thrown NoSuchAlgorithmException");
        } catch (NoSuchAlgorithmException nsae) { }

        try {
            CertificateFactory cf = CertificateFactory.getInstance("BOGUS");
            throw new Exception("CertificateFactory.getInstance() should " +
                "have thrown CertificateException");
        } catch (CertificateException ce) { }
    }
}
