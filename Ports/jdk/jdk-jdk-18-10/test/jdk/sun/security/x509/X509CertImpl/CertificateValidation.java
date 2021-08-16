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

/*
 * @test
 * @bug 8238452
 * @modules java.base/sun.security.x509
 *          java.base/sun.security.tools.keytool
 * @summary This test generates V3 certificate with certain validity period
 * and checks whether the validity has expired or not.
 */

import sun.security.tools.keytool.CertAndKeyGen;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import sun.security.x509.X509CertImpl;
import sun.security.x509.X500Name;


public class CertificateValidation {

    public static void main(String[] args) throws Exception {

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        cal.set(2050, 00, 01, 01, 00, 00);
        Date lastDate = cal.getTime();
        // Seconds till lastDate plus one hour
        long validity = (lastDate.getTime() - System.currentTimeMillis())/1000L + 3600;
        Date firstDate = new Date(lastDate.getTime() - validity * 1000L);
        CertAndKeyGen ckg = new CertAndKeyGen("RSA", "SHA256withRSA");
        ckg.generate(2048);
        X509Certificate crt = ckg.getSelfCertificate(
                new X500Name("CN=Me"), firstDate, validity);
        byte[] encoded = crt.getEncoded();
        X509CertImpl certImpl = new X509CertImpl(encoded);
        certImpl.checkValidity();
    }
}
