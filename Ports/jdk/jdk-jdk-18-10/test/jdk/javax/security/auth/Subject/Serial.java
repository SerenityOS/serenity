/*
 * Copyright (c) 2000, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4364826
 * @modules jdk.security.auth
 * @summary     Subject serialized principal set is
 *              implementation-dependent class
 * @run main/othervm/policy=Serial.policy Serial
 */

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashSet;
import java.util.Set;
import javax.security.auth.Subject;

public class Serial implements java.io.Serializable {

    public static void main(String[] args) {

        try {
            FileOutputStream fos = new FileOutputStream("serial.tmp");
            ObjectOutputStream oos = new ObjectOutputStream(fos);

            HashSet principals = new HashSet();
            principals.add
                (new com.sun.security.auth.NTUserPrincipal("test"));
            principals.add
                (new com.sun.security.auth.NTDomainPrincipal("test2"));

            Subject s = new Subject
                                (false,
                                principals,
                                new HashSet(),
                                new HashSet());
            oos.writeObject(s);
            oos.flush();
            fos.close();

            FileInputStream fis = new FileInputStream("serial.tmp");
            ObjectInputStream ois = new ObjectInputStream(fis);

            Subject s2 = (Subject)ois.readObject();
            fis.close();

            System.out.println("s2 = " + s2.toString());
            System.out.println("s2.getPrincipals().size() = " +
                                s2.getPrincipals().size());
            if (!s.equals(s2) || !s2.equals(s)) {
                throw new SecurityException("Serial test failed: " +
                                        "EQUALS TEST FAILED");
            }

            // make sure private credentials are not serializable
            // without permissions

            Set privateCredentials = s.getPrivateCredentials();
            privateCredentials.add(new Serial());

            fos = new FileOutputStream("serial2.tmp");
            oos = new ObjectOutputStream(fos);
            try {
                oos.writeObject(privateCredentials);
                oos.flush();
                fos.close();
                throw new RuntimeException("Serial test failed: " +
                        "allowed to serialize private credential set");
            } catch (SecurityException se) {
                // good
                se.printStackTrace();
            }

            System.out.println("Serial test succeeded");
        } catch (Exception e) {
            e.printStackTrace();
            throw new SecurityException("Serial test failed");
        }
    }
}
