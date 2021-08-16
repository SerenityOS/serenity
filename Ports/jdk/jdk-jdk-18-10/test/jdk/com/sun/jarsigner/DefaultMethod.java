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

/*
 * @test
 * @bug 8039358
 * @summary com.sun.jarsigner.ContentSignerParameters.getTSAPolicyID() should be default
 * @modules jdk.jartool
 * @compile DefaultMethod.java
 */

import com.sun.jarsigner.ContentSignerParameters;

import java.net.URI;
import java.security.cert.X509Certificate;
import java.util.zip.ZipFile;

public class DefaultMethod implements ContentSignerParameters {

    @Override
    public String[] getCommandLine() {
        return new String[0];
    }

    @Override
    public URI getTimestampingAuthority() {
        return null;
    }

    @Override
    public X509Certificate getTimestampingAuthorityCertificate() {
        return null;
    }

    @Override
    public byte[] getSignature() {
        return new byte[0];
    }

    @Override
    public String getSignatureAlgorithm() {
        return null;
    }

    @Override
    public X509Certificate[] getSignerCertificateChain() {
        return new X509Certificate[0];
    }

    @Override
    public byte[] getContent() {
        return new byte[0];
    }

    @Override
    public ZipFile getSource() {
        return null;
    }
}
