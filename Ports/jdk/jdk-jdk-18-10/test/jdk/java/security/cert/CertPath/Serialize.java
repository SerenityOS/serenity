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
 * @bug 4404718
 * @summary Make sure that a CertPath object can be serialized
 */
import java.io.*;
import java.security.cert.*;
import java.util.Collections;

public class Serialize {

    public static void main(String args[]) throws Exception {

        // create a certpath consisting of one certificate
        File f = new File(System.getProperty("test.src", "."), "cert_file");
        FileInputStream fis = new FileInputStream(f);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        Certificate c = cf.generateCertificate(fis);
        fis.close();
        CertPath outcp = cf.generateCertPath(Collections.singletonList(c));

        // serialize certpath and write it out
        FileOutputStream fos = new FileOutputStream("certpath.ser");
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(outcp);
        oos.flush();
        oos.close();
        fos.close();

        // read certpath in and deserialize
        FileInputStream cfis = new FileInputStream("certpath.ser");
        ObjectInputStream ois = new ObjectInputStream(cfis);
        CertPath incp = (CertPath)ois.readObject();
        ois.close();
        cfis.close();

        if (!incp.equals(outcp))
            throw new Exception("CertPath serialization test FAILED");
    }
}
