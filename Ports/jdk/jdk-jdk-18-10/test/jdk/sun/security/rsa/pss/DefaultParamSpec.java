/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import sun.security.x509.X500Name;
import sun.security.x509.X509CRLImpl;

import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.Date;

/**
 * @test
 * @bug 8242811
 * @modules java.base/sun.security.x509
 * @summary AlgorithmId::getDefaultAlgorithmParameterSpec returns incompatible
 *          PSSParameterSpec for an RSASSA-PSS key
 */
public class DefaultParamSpec {
    public static void main(String[] args) throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSASSA-PSS");
        KeyFactory kf = KeyFactory.getInstance("RSASSA-PSS");
        kpg.initialize(new RSAKeyGenParameterSpec(2048,
                RSAKeyGenParameterSpec.F4,
                new PSSParameterSpec(
                        "SHA-384", "MGF1",
                        new MGF1ParameterSpec("SHA-384"),
                        48, PSSParameterSpec.TRAILER_FIELD_BC)));

        X509CRLImpl crl = new X509CRLImpl(
                new X500Name("CN=Issuer"), new Date(), new Date());
        crl.sign(kpg.generateKeyPair().getPrivate(), "RSASSA-PSS");
    }
}
