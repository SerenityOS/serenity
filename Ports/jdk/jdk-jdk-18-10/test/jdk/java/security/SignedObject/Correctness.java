/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6618658
 * @summary Deserialization allows creation of mutable SignedObject
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.SignedObject;


public class Correctness {

    public static void main(String[] args) throws Exception {

        String SIGALG = "SHA1withRSA";
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        KeyPair kp = kpg.generateKeyPair();

        SignedObject so1 = new SignedObject("Hello", kp.getPrivate(),
                Signature.getInstance(SIGALG));

        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(byteOut);
        out.writeObject(so1);
        out.close();

        byte[] data = byteOut.toByteArray();

        SignedObject so2 = (SignedObject)new ObjectInputStream(
                new ByteArrayInputStream(data)).readObject();

        if (!so2.getObject().equals("Hello")) {
            throw new Exception("Content changed");
        }
        if (!so2.getAlgorithm().equals(SIGALG)) {
            throw new Exception("Signature algorithm unknown");
        }
        if (!so2.verify(kp.getPublic(), Signature.getInstance(SIGALG))) {
            throw new Exception("Not verified");
        }
    }
}
