/*
 * Copyright (c) 1999, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @author Gary Ellison
 * @bug 4170635 8242151
 * @summary Verify equals()/hashCode() contract honored
 * @modules java.base/sun.security.util
 *          java.base/sun.security.x509
 * @run main/othervm/policy=Allow.policy AVAEqualsHashCode
 */

import java.io.*;
import sun.security.x509.*;
import sun.security.util.*;
import java.lang.reflect.*;

public class AVAEqualsHashCode {

   public static void main(String[] args) throws Exception {

        // encode
        String name = "CN=eve s. dropper";
        X500Name dn = new X500Name(name);
        DerOutputStream deros = new DerOutputStream();
        ObjectIdentifier oid = ObjectIdentifier.of("1.2.840.113549.2.5");

        dn.encode(deros);
        byte[] ba = deros.toByteArray();
        DerValue dv = new DerValue(ba);

        GetAVAConstructor a = new GetAVAConstructor();
        java.security.AccessController.doPrivileged(a);
        Constructor c = a.getCons();

        Object[] objs = new Object[2];
        objs[0] = oid;
        objs[1] = dv;
        Object ava1 = null, ava2 = null;
        try {
            ava1 = c.newInstance(objs);
            ava2 = c.newInstance(objs);
        } catch (Exception e) {
            System.out.println("Caught unexpected exception " + e);
            throw e;
        }

        // System.out.println(ava1.equals(ava2));
        // System.out.println(ava1.hashCode() == ava2.hashCode());
        if ( (ava1.equals(ava2)) == (ava1.hashCode()==ava2.hashCode()) )
            System.out.println("PASSED");
        else
            throw new Exception("FAILED equals()/hashCode() contract");
    }
}

class GetAVAConstructor implements java.security.PrivilegedExceptionAction {

    private Class avaClass = null;
    private Constructor avaCons = null;

    public Object run() throws Exception {
        try {
            avaClass = Class.forName("sun.security.x509.AVA");
            Constructor[] cons = avaClass.getDeclaredConstructors();

            int i;
            for (i = 0; i < cons.length; i++) {
                Class [] parms = cons[i].getParameterTypes();
                if (parms.length == 2) {
                    if (parms[0].getName().equalsIgnoreCase("sun.security.util.ObjectIdentifier") &&
                            parms[1].getName().equalsIgnoreCase("sun.security.util.DerValue")) {
                        avaCons = cons[i];
                        avaCons.setAccessible(true);
                        break;
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("Caught unexpected exception " + e);
            throw e;
        }
        return avaCons;
    }

    public Constructor getCons(){
        return avaCons;
    }

}
