/*
 * Copyright (c) 2005, 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4641821
 * @summary hashCode() and equals() for KerberosKey and KerberosTicket
 */

/*
 * Must setup KDC and Kerberos configuration file
 */

import java.net.InetAddress;
import java.util.Date;
import javax.security.auth.kerberos.*;

public class KerberosHashEqualsTest {
    public static void main(String[] args) throws Exception {
        new KerberosHashEqualsTest().check();
    }

    void checkSame(Object o1, Object o2) {
        if(!o1.equals(o2)) {
            throw new RuntimeException("equals() fails");
        }
        if(o1.hashCode() != o2.hashCode()) {
            throw new RuntimeException("hashCode() not same");
        }
    }

    void checkNotSame(Object o1, Object o2) {
        if(o1.equals(o2)) {
            throw new RuntimeException("equals() succeeds");
        }
    }

    void check() throws Exception {
        KerberosKey k1, k2;
        k1 = new KerberosKey(newKP("A"), "pass".getBytes(), 1, 1);
        k2 = new KerberosKey(newKP("A"), "pass".getBytes(), 1, 1);
        checkSame(k1, k1);  // me to me
        checkSame(k1, k2);  // same

        k2.destroy();
        checkNotSame(k1, k2);
        checkNotSame(k2, k1);
        checkSame(k2, k2);

        k1.destroy();
        checkNotSame(k1, k2);

        // Destroyed key has string and hashCode
        k1.toString(); k1.hashCode();

        // a little different
        k1 = new KerberosKey(newKP("A"), "pass".getBytes(), 1, 1);
        k2 = new KerberosKey(newKP("B"), "pass".getBytes(), 1, 1);
        checkNotSame(k1, k2);

        k2 = new KerberosKey(newKP("A"), "ssap".getBytes(), 1, 1);
        checkNotSame(k1, k2);

        k2 = new KerberosKey(newKP("A"), "pass".getBytes(), 2, 1);
        checkNotSame(k1, k2);

        k2 = new KerberosKey(newKP("A"), "pass".getBytes(), 1, 2);
        checkNotSame(k1, k2);

        // Null
        k1 = new KerberosKey(null, "pass".getBytes(), 1, 2);
        checkNotSame(k1, k2); // null to non-null
        k2 = new KerberosKey(null, "pass".getBytes(), 1, 2);
        checkSame(k1, k2);    // null to null

        // Even key with null principal has a string and hashCode
        k1.toString(); k1.hashCode();

        checkNotSame(k1, "Another Object");

        EncryptionKey e1, e2;
        e1 = new EncryptionKey("pass".getBytes(), 1);
        e2 = new EncryptionKey("pass".getBytes(), 1);
        checkSame(e1, e1);  // me to me
        checkSame(e1, e2);  // same

        e2.destroy();
        checkNotSame(e1, e2);
        checkNotSame(e2, e1);
        checkSame(e2, e2);

        e1.destroy();
        checkNotSame(e1, e2);

        // Destroyed key has string and hashCode
        e1.toString(); e1.hashCode();

        // a little different
        e1 = new EncryptionKey("pass".getBytes(), 1);
        e2 = new EncryptionKey("ssap".getBytes(), 1);
        checkNotSame(e1, e2);

        e2 = new EncryptionKey("pass".getBytes(), 2);
        checkNotSame(e1, e2);

        checkNotSame(e1, "Another Object");

        KerberosTicket t1, t2;
        t1 = new KerberosTicket("asn1".getBytes(), newKP("client"), newKP("server"), "pass".getBytes(), 1, new boolean[] {true, true}, new Date(0), new Date(0), new Date(0), new Date(0), null);
        t2 = new KerberosTicket("asn1".getBytes(), newKP("client"), newKP("server"), "pass".getBytes(), 1, new boolean[] {true, true}, new Date(0), new Date(0), new Date(0), new Date(0), null);
        checkSame(t1, t1);
        checkSame(t1, t2);
        t2 = new KerberosTicket("asn11".getBytes(), newKP("client"), newKP("server"), "pass".getBytes(), 1, new boolean[] {true, true}, new Date(0), new Date(0), new Date(0), new Date(0), null);
        checkNotSame(t1, t2);
        t2 = new KerberosTicket("asn1".getBytes(), newKP("client1"), newKP("server"), "pass".getBytes(), 1, new boolean[] {true, true}, new Date(0), new Date(0), new Date(0), new Date(0), null);
        checkNotSame(t1, t2);
        t2 = new KerberosTicket("asn1".getBytes(), newKP("client"), newKP("server1"), "pass".getBytes(), 1, new boolean[] {true, true}, new Date(0), new Date(0), new Date(0), new Date(0), null);
        checkNotSame(t1, t2);
        t2 = new KerberosTicket("asn1".getBytes(), newKP("client"), newKP("server"), "pass1".getBytes(), 1, new boolean[] {true, true}, new Date(0), new Date(0), new Date(0), new Date(0), null);
        checkNotSame(t1, t2);
        t2 = new KerberosTicket("asn1".getBytes(), newKP("client"), newKP("server"), "pass".getBytes(), 2, new boolean[] {true, true}, new Date(0), new Date(0), new Date(0), new Date(0), null);
        checkNotSame(t1, t2);
        t2 = new KerberosTicket("asn1".getBytes(), newKP("client"), newKP("server"), "pass".getBytes(), 1, new boolean[] {false, true}, new Date(0), new Date(0), new Date(0), new Date(0), null);
        checkNotSame(t1, t2);
        t2 = new KerberosTicket("asn1".getBytes(), newKP("client"), newKP("server"), "pass".getBytes(), 1, new boolean[] {true, true}, new Date(1), new Date(0), new Date(0), new Date(0), null);
        checkNotSame(t1, t2);
        t2 = new KerberosTicket("asn1".getBytes(), newKP("client"), newKP("server"), "pass".getBytes(), 1, new boolean[] {true, true}, new Date(0), new Date(1), new Date(0), new Date(0), null);
        checkNotSame(t1, t2);
        t2 = new KerberosTicket("asn1".getBytes(), newKP("client"), newKP("server"), "pass".getBytes(), 1, new boolean[] {true, true}, new Date(0), new Date(0), new Date(1), new Date(0), null);
        checkNotSame(t1, t2);
        t2 = new KerberosTicket("asn1".getBytes(), newKP("client"), newKP("server"), "pass".getBytes(), 1, new boolean[] {true, true}, new Date(0), new Date(0), new Date(0), new Date(0), new InetAddress[2]);
        checkNotSame(t1, t2);

        t2 = new KerberosTicket("asn1".getBytes(), newKP("client"), newKP("server"), "pass".getBytes(), 1, new boolean[] {true, true}, new Date(0), new Date(0), new Date(0), new Date(1), null);
        t1 = new KerberosTicket("asn1".getBytes(), newKP("client"), newKP("server"), "pass".getBytes(), 1, new boolean[] {true, true}, new Date(0), new Date(0), new Date(0), new Date(2), null);
        checkSame(t1, t2);  // renewtill is useless

        t2.destroy();
        checkNotSame(t1, t2);
        t2.hashCode(); t2.toString();

        // destroyed tickets doesn't equal to each other
        checkNotSame(t2, t1);
        checkSame(t2, t2);

        t2 = new KerberosTicket("asn1".getBytes(), newKP("client"), newKP("server"), "pass".getBytes(), 1, new boolean[] {true, true, true, true, true, true, true, true, true, true}, new Date(0), new Date(0), new Date(0), new Date(1), null);
        t1 = new KerberosTicket("asn1".getBytes(), newKP("client"), newKP("server"), "pass".getBytes(), 1, new boolean[] {true, true, true, true, true, true, true, true, true, true}, new Date(0), new Date(0), new Date(0), new Date(2), null);
        checkNotSame(t1, t2);  // renewtill is useful

        checkNotSame(t1, "Another Object");

        KerberosCredMessage m1, m2;
        m1 = new KerberosCredMessage(newKP("C"), newKP("S"), "message".getBytes());
        m2 = new KerberosCredMessage(newKP("C"), newKP("S"), "message".getBytes());
        checkSame(m1, m1);  // me to me
        checkSame(m1, m2);  // same

        m2.destroy();
        checkNotSame(m1, m2);
        checkNotSame(m2, m1);
        checkSame(m2, m2);

        m1.destroy();
        checkNotSame(m1, m2);

        // Destroyed message has string and hashCode
        m1.toString(); m1.hashCode();

        // a little different
        m1 = new KerberosCredMessage(newKP("C"), newKP("S"), "message".getBytes());
        m2 = new KerberosCredMessage(newKP("A"), newKP("S"), "message".getBytes());
        checkNotSame(m1, m2);

        m2 = new KerberosCredMessage(newKP("C"), newKP("B"), "message".getBytes());
        checkNotSame(m1, m2);

        m1 = new KerberosCredMessage(newKP("C"), newKP("S"), "hello".getBytes());
        checkNotSame(m1, m2);

        checkNotSame(m1, "Another Object");

        System.out.println("Good!");
    }

    KerberosPrincipal newKP(String s) {
        return new KerberosPrincipal(s + "@JLABS.SFBAY.SUN.COM");
    }
}
