/*
 * Copyright (c) 2008, 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6659990 8147772
 * @summary test the immutability of the Date fields in KerberosTicket class,
 *          serialization, and behavior after being destroyed.
 */

/*
 * Must setup KDC and Kerberos configuration file
 */

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Date;
import java.io.*;
import javax.security.auth.RefreshFailedException;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.kerberos.KerberosTicket;
import java.util.Base64;

public class KerberosTixDateTest {

    // Serialized KerberosTicket from JDK6 (encoded in BASE64)
    // Note: the KerberosTicket object is created using the same values as
    // the KerberosTicket 't' in main(). Deserialization should succeed
    // and the deserialized object should equal to 't'.
    static String serializedKerberosTix =
"rO0ABXNyACtqYXZheC5zZWN1cml0eS5hdXRoLmtlcmJlcm9zLktlcmJlcm9zVGlja2V0ZqGBbXB3" +
"w7sCAApbAAxhc24xRW5jb2Rpbmd0AAJbQkwACGF1dGhUaW1ldAAQTGphdmEvdXRpbC9EYXRlO0wA" +
"BmNsaWVudHQAMExqYXZheC9zZWN1cml0eS9hdXRoL2tlcmJlcm9zL0tlcmJlcm9zUHJpbmNpcGFs" +
"O1sAD2NsaWVudEFkZHJlc3Nlc3QAF1tMamF2YS9uZXQvSW5ldEFkZHJlc3M7TAAHZW5kVGltZXEA" +
"fgACWwAFZmxhZ3N0AAJbWkwACXJlbmV3VGlsbHEAfgACTAAGc2VydmVycQB+AANMAApzZXNzaW9u" +
"S2V5dAAmTGphdmF4L3NlY3VyaXR5L2F1dGgva2VyYmVyb3MvS2V5SW1wbDtMAAlzdGFydFRpbWVx" +
"AH4AAnhwdXIAAltCrPMX+AYIVOACAAB4cAAAAARhc24xc3IADmphdmEudXRpbC5EYXRlaGqBAUtZ" +
"dBkDAAB4cHcIAAAAAAC8YU54c3IALmphdmF4LnNlY3VyaXR5LmF1dGgua2VyYmVyb3MuS2VyYmVy" +
"b3NQcmluY2lwYWyZp31dDx4zKQMAAHhwdXEAfgAIAAAAEzARoAMCAQGhCjAIGwZjbGllbnR1cQB+" +
"AAgAAAAVGxNKTEFCUy5TRkJBWS5TVU4uQ09NeHBxAH4AC3VyAAJbWlePIDkUuF3iAgAAeHAAAAAg" +
"AAAAAAAAAAABAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABxAH4AC3NxAH4ADHVxAH4ACAAAABMwEaAD" +
"AgEBoQowCBsGc2VydmVydXEAfgAIAAAAFRsTSkxBQlMuU0ZCQVkuU1VOLkNPTXhzcgAkamF2YXgu" +
"c2VjdXJpdHkuYXV0aC5rZXJiZXJvcy5LZXlJbXBskoOG6DyvS9cDAAB4cHVxAH4ACAAAABUwE6AD" +
        "AgEBoQwECnNlc3Npb25LZXl4cQB+AAs=";

    public static void main(String[] args) throws Exception {
        byte[] asn1Bytes = "asn1".getBytes();
        KerberosPrincipal client = new KerberosPrincipal("client@JLABS.SFBAY.SUN.COM");
        KerberosPrincipal server = new KerberosPrincipal("server@JLABS.SFBAY.SUN.COM");
        byte[] keyBytes = "sessionKey".getBytes();
        long originalTime = 12345678L;
        Date inDate = new Date(originalTime);
        boolean[] flags = new boolean[9];
        flags[8] = true; // renewable
        KerberosTicket t = new KerberosTicket(asn1Bytes, client, server,
                keyBytes, 1 /*keyType*/, flags, inDate /*authTime*/,
                inDate /*startTime*/, inDate /*endTime*/,
                inDate /*renewTill*/, null /*clientAddresses*/);
        inDate.setTime(0); // for testing the constructor

        testDateImmutability(t, originalTime);
        testS11nCompatibility(t); // S11n: Serialization
        testDestroy(t);
    }

    private static void checkTime(KerberosTicket kt, long timeValue) {
        if (kt.getAuthTime().getTime() != timeValue) {
            throw new RuntimeException("authTime check fails!");
        }
        if (kt.getStartTime().getTime() != timeValue) {
            throw new RuntimeException("startTime check fails!");
        }
        if (kt.getEndTime().getTime() != timeValue) {
            throw new RuntimeException("endTime check fails!");
        }
        if (kt.getRenewTill().getTime() != timeValue) {
            throw new RuntimeException("renewTill check fails!");
        }
    }

    private static void testDateImmutability(KerberosTicket t, long origTime)
        throws Exception {
        // test the constructor
        System.out.println("Testing constructor...");
        checkTime(t, origTime);

        // test the getAuth/Start/EndTime() & getRenewTill() methods
        System.out.println("Testing getAuth/Start/EndTime() & getRenewTill()...");
        t.getAuthTime().setTime(0);
        t.getStartTime().setTime(0);
        t.getEndTime().setTime(0);
        t.getRenewTill().setTime(0);
        checkTime(t, origTime);

        System.out.println("DateImmutability Test Passed");
    }

    private static void checkEqualsAndHashCode(byte[] bytes, KerberosTicket t)
        throws IOException, ClassNotFoundException {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        KerberosTicket deserializedTicket = (KerberosTicket)
                (new ObjectInputStream(bais).readObject());
        if (!deserializedTicket.equals(t)) {
            throw new RuntimeException("equals() check fails!");
        }
        if (deserializedTicket.hashCode() != t.hashCode()) {
            throw new RuntimeException("hashCode() check fails!");
        }
    }

    private static void testS11nCompatibility(KerberosTicket t)
        throws Exception {

        System.out.println("Testing against KerberosTicket from JDK6...");
        byte[] serializedBytes =
            Base64.getMimeDecoder().decode(serializedKerberosTix);
        checkEqualsAndHashCode(serializedBytes, t);

        System.out.println("Testing against KerberosTicket from current rel...");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new ObjectOutputStream(baos).writeObject(t);
        checkEqualsAndHashCode(baos.toByteArray(), t);

        System.out.println("S11nCompatibility Test Passed");
    }

    private static void testDestroy(KerberosTicket t) throws Exception {
        t.destroy();
        if (!t.isDestroyed()) {
            throw new RuntimeException("ticket should have been destroyed");
        }
        // Although these methods are meaningless, they can be called
        for (Method m: KerberosTicket.class.getDeclaredMethods()) {
            if (Modifier.isPublic(m.getModifiers())
                    && m.getParameterCount() == 0) {
                System.out.println("Testing " + m.getName() + "...");
                try {
                    m.invoke(t);
                } catch (InvocationTargetException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof RefreshFailedException ||
                            cause instanceof IllegalStateException) {
                        // this is OK
                    } else {
                        throw e;
                    }
                }
            }
        }
        System.out.println("Destroy Test Passed");
    }
}
