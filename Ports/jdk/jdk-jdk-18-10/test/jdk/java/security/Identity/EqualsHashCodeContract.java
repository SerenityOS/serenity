/*
 * Copyright (c) 1999, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4183136
 * @summary java.security.Identity violates equals/hashCode contract
 */
import java.security.*;
import java.io.*;

public class EqualsHashCodeContract
{
    public static void main(String args[]) throws Exception {

        Identity i1=new MyIdentity("identity",
                                   new MyIdentityScope("IdentityScope"));
        Identity i2=new MyIdentity("identity",
                                   new MyIdentityScope("IdentityScope"));
        Identity i3=new MyIdentity("identity",
                                   new MyIdentityScope(""));

        PublicKey pk1=new MyPublicKey();
        PublicKey pk2=new MyPublicKey();

        if ( !(i1.equals(i2)) == (i1.hashCode()==i2.hashCode()) ) {
            System.err.println("FAILED");
            Exception up = new
                Exception("Contract violated -- same name and same scope");
            throw up;
        }
        System.out.println("Test same name, same scope........... PASSED");

        i1.setPublicKey(pk1);
        i3.setPublicKey(pk1);
        if ( !((i1.equals(i3)) && (i1.hashCode()==i3.hashCode()))) {
            System.err.println("FAILED");
            Exception up = new Exception("Contract violated -- PublicKeys do not differ");
            throw up;
        }
        System.out.println("Test same name, same PublicKeys ..... PASSED");

        System.out.println("TEST PASSED");

    }
}

class MyIdentity extends Identity {
    public MyIdentity(String name, IdentityScope is) throws KeyManagementException {
        super(name, is);
    }
}

class MyPublicKey implements PublicKey, Certificate {
    private byte e[] = null;
    public String getAlgorithm() {
        return null;
    }
    public String getFormat() {
        return new String("PKCS15");
    }

    public byte[] getEncoded() {
        if (e == null) {
            ByteArrayOutputStream bs = new ByteArrayOutputStream();
            DataOutputStream ds = new DataOutputStream(bs);
            try {
                ds.writeLong(System.currentTimeMillis());
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            e = bs.toByteArray();
        }

        return e;
    }

    public void decode(InputStream stream) {
    }
    public void encode(OutputStream stream) {
    }
    public Principal getGuarantor() {
        return null;
    }
    public Principal getPrincipal() {
        return null;
    }
    public PublicKey getPublicKey() {
        return this;
    }
    public String toString(boolean detailed) {
        return null;
    }
}


class MyIdentityScope extends IdentityScope {
    public MyIdentityScope(String name) {
        super(name);
    }

    public int size() {
        return 0;
    }

    public Identity getIdentity(String name) {
        return null;
    }

    public Identity getIdentity(PublicKey key) {
        return null;
    }

    public void addIdentity(Identity identity)  {
    }

    public void removeIdentity(Identity identity)  {
    }

    public java.util.Enumeration identities() {
        return null;
    }


}
