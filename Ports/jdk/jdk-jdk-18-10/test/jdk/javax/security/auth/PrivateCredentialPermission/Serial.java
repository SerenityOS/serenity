/*
 * Copyright (c) 2000, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4364855
 * @summary     PrivateCredentialPermission serialized set has
 *              implementation-dependent class
 * @run main/othervm/policy=Serial.policy Serial
 */

import javax.security.auth.*;
import java.io.*;
import java.util.*;

public class Serial implements java.io.Serializable {

    public static void main(String[] args) {

        try {
            FileOutputStream fos = new FileOutputStream("serial.tmp");
            ObjectOutputStream oos = new ObjectOutputStream(fos);

            PrivateCredentialPermission pcp = new PrivateCredentialPermission
                        ("cred1 pc1 \"pn1\" pc2 \"pn2\"", "read");
            oos.writeObject(pcp);
            oos.flush();
            fos.close();

            FileInputStream fis = new FileInputStream("serial.tmp");
            ObjectInputStream ois = new ObjectInputStream(fis);

            PrivateCredentialPermission pcp2 =
                (PrivateCredentialPermission)ois.readObject();
            fis.close();

            System.out.println("pcp2 = " + pcp2.toString());
            System.out.println("pcp2.getPrincipals().length = " +
                                pcp2.getPrincipals().length);
            if (!pcp.equals(pcp2) || !pcp2.equals(pcp)) {
                throw new SecurityException("Serial test failed: " +
                                        "EQUALS TEST FAILED");
            }

            System.out.println("Serial test succeeded");
        } catch (Exception e) {
            e.printStackTrace();
            throw new SecurityException("Serial test failed");
        }
    }
}
