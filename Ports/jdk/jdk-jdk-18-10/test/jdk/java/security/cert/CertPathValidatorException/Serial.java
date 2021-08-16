/*
 * Copyright (c) 2008, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6465942
 * @summary Test deserialization of CertPathValidatorException
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
//import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertPathValidatorException.BasicReason;
import java.util.Collections;

/**
 * This class tests to see if CertPathValidatorException can be serialized and
 * deserialized properly.
 */
public class Serial {
    private static volatile boolean failed = false;
    public static void main(String[] args) throws Exception {

        File f = new File(System.getProperty("test.src", "."), "cert_file");
        FileInputStream fis = new FileInputStream(f);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        Certificate c = cf.generateCertificate(fis);
        fis.close();
        CertPath cp = cf.generateCertPath(Collections.singletonList(c));

        CertPathValidatorException cpve1 =
            new CertPathValidatorException
                ("Test", new Exception("Expired"), cp, 0, BasicReason.EXPIRED);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        FileOutputStream fos = new FileOutputStream("jdk7.serial");
        ObjectOutputStream oos = new ObjectOutputStream(baos);
//        ObjectOutputStream foos = new ObjectOutputStream(fos);
        oos.writeObject(cpve1);
//        foos.writeObject(cpve1);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        CertPathValidatorException cpve2 =
            (CertPathValidatorException) ois.readObject();
        check(!cpve1.getMessage().equals(cpve2.getMessage()),
            "CertPathValidatorException messages not equal");
        check(!cpve1.getCause().getMessage().equals(cpve2.getCause().getMessage()),
            "CertPathValidatorException causes not equal");
        check(!cpve1.getCertPath().equals(cpve2.getCertPath()),
            "CertPathValidatorException certpaths not equal");
        check(cpve1.getIndex() != cpve2.getIndex(),
            "CertPathValidatorException indexes not equal");
        check(cpve1.getReason() != cpve2.getReason(),
            "CertPathValidatorException reasons not equal");
        oos.close();
        ois.close();

        f = new File(System.getProperty("test.src", "."), "jdk6.serial");
        fis = new FileInputStream(f);
        ois = new ObjectInputStream(fis);
        cpve2 = (CertPathValidatorException) ois.readObject();
        check(!cpve1.getMessage().equals(cpve2.getMessage()),
            "CertPathValidatorException messages not equal");
        check(!cpve1.getCause().getMessage().equals(cpve2.getCause().getMessage()),
            "CertPathValidatorException causes not equal");
        check(!cpve1.getCertPath().equals(cpve2.getCertPath()),
            "CertPathValidatorException certpaths not equal");
        check(cpve1.getIndex() != cpve2.getIndex(),
            "CertPathValidatorException indexes not equal");
//      System.out.println(cpve2.getReason());
        check(cpve2.getReason() != BasicReason.UNSPECIFIED,
            "CertPathValidatorException reasons not equal");
        oos.close();
        ois.close();
        if (failed) {
            throw new Exception("Some tests FAILED");
        }
    }

    private static void check(boolean expr, String message) {
        if (expr) {
            failed = true;
            System.err.println("FAILED: " + message);
        }
    }
}
