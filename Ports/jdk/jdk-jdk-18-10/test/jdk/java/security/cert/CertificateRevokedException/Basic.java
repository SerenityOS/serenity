/*
 * Copyright (c) 2007, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4946388
 * @summary Unit test for CertificateRevokedException
 * @modules java.base/sun.security.x509
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.cert.CertificateRevokedException;
import java.security.cert.CRLReason;
import java.security.cert.Extension;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.security.auth.x500.X500Principal;

import sun.security.x509.InvalidityDateExtension;

public class Basic {

    public static void main(String[] args) throws Exception {

        // test ctor for NPE
        CertificateRevokedException cre = null;
        try {
            cre = new CertificateRevokedException(null, null, null, null);
            throw new Exception("Did not throw expected NullPointerException");
        } catch (NullPointerException npe) {}

        // test getRevocationDate returns clone
        Date date = Calendar.getInstance().getTime();
        long time = date.getTime();
        Map<String, Extension> extensions = new HashMap<String, Extension>();
        Date invDate = new Date(date.getTime());
        extensions.put("2.5.29.24", new InvalidityDateExtension(invDate));
        cre = new CertificateRevokedException(date, CRLReason.UNSPECIFIED,
            new X500Principal("CN=TestCA"), extensions);
        Date date2 = cre.getRevocationDate();
        if (date2 == date) {
            throw new Exception("getRevocationDate does not return copy");
        }

        // test getRevocationDate returns the same date as specified in ctor
        if (!date.equals(date2)) {
            throw new Exception("getRevocationDate returns different date");
        }

        // test ctor copies date
        date.setTime(777);
        date2 = cre.getRevocationDate();
        if (date2.getTime() != time) {
            throw new Exception("Constructor did not copy date parameter");
        }

        // test getReason returns same reason as specified in ctor
        CRLReason reason = cre.getRevocationReason();
        if (reason != CRLReason.UNSPECIFIED) {
            throw new Exception("getRevocationReason returns different reason");
        }

        // test getAuthorityName returns same name as specified in ctor
        if (!cre.getAuthorityName().equals(new X500Principal("CN=TestCA"))) {
            throw new Exception("getAuthorityName returns different name");
        }

        // test getInvalidityDate returns invalidity date
        Date invalidity = cre.getInvalidityDate();
        if (invalidity == null) {
            throw new Exception("getInvalidityDate returned null");
        }
        if (invalidity.getTime() != time) {
            throw new Exception("getInvalidityDate returned wrong date");
        }
        // test getInvalidityDate copies date
        invDate.setTime(777);
        if (invalidity.getTime() != time) {
            throw new Exception("getInvalidityDate did not return copy of date");
        }

        // test serialization
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(cre);
        oos.close();

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        CertificateRevokedException cre2 =
            (CertificateRevokedException) ois.readObject();

        if (cre2.getRevocationDate().getTime() != time) {
            throw new Exception("deserialized exception returns different date");
        }
        if (cre2.getRevocationReason() != CRLReason.UNSPECIFIED) {
            throw new Exception("deserialized exception returns different reason");
        }
        if (!cre2.getAuthorityName().equals(new X500Principal("CN=TestCA"))) {
            throw new Exception("deserialized exception returns different name");
        }
        if (!cre2.getExtensions().keySet().equals(cre.getExtensions().keySet())) {
            throw new Exception("deserialized exception returns different extensions");
        }
    }
}
