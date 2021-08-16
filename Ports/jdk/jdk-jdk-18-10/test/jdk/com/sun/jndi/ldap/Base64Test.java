/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8035807
 * @summary Confirm that old and new Base64 encodings are compatible.
 */

import java.io.*;
import java.util.*;
import javax.naming.*;
import javax.naming.directory.*;

/*
 * RFC 2713 specifies an encoding for Java objects stored in an LDAP directory.
 * Section 3.6 specifies how a binary-valued JNDI RefAddr object is encoded
 * in the value of a javaReferenceAttribute LDAP attribute: first the RefAddr
 * object is serialized and then it is encoded using Base64.
 *
 * Since JDK 9, the JNDI/LDAP provider uses the public Base64 encoder which
 * adheres strictly to the MIME encoding rules. The encoder inserts '\r\n'
 * as the line separator at intervals of 76 characters. Previously the
 * JNDI/LDAP provider used a private Base64 encoder which inserted '\n'
 * as the line separator. It is a compatible change.
 *
 * This test demonstrates that there is no compatability problem when
 * decoding using the new Base64 coder:
 *
 *   encoded bytes captured from s.m.BASE64Encoder, decode with j.u.Base64.Decoder => OK
 *   encoded bytes captured from j.u.Base64.Encoder, decode with j.u.Base64.Decoder => OK
 *
 *
 * NOTE: The two Base64 encodings used in this test were captured from
 *       LDAP protocol exchanges during attempts by the JNDI/LDAP provider
 *       to store a JNDI Reference test object.
 */

public class Base64Test {
    /*
     * The old Base64 encoding uses '\n' as the line separator at 76 character
     * intervals:
     *
     * 0000: 72 4F 30 41 42 58 4E 79 41 42 70 71 59 58 5A 68  rO0ABXNyABpqYXZh
     * 0010: 65 43 35 75 59 57 31 70 62 6D 63 75 51 6D 6C 75  eC5uYW1pbmcuQmlu
     * 0020: 59 58 4A 35 55 6D 56 6D 51 57 52 6B 63 74 43 61  YXJ5UmVmQWRkctCa
     * 0030: 6B 37 4C 65 73 34 68 48 41 67 41 42 57 77 41 44  k7Les4hHAgABWwAD
     * 0040: 59 6E 56 6D 64 41 41 43 57 30 4A 34 0A 63 67 41  YnVmdAACW0J4.cgA <
     * 0050: 55 61 6D 46 32 59 58 67 75 62 6D 46 74 61 57 35  UamF2YXgubmFtaW5
     * 0060: 6E 4C 6C 4A 6C 5A 6B 46 6B 5A 48 4C 72 6F 41 65  nLlJlZkFkZHLroAe
     * 0070: 61 41 6A 69 76 53 67 49 41 41 55 77 41 43 47 46  aAjivSgIAAUwACGF
     * 0080: 6B 5A 48 4A 55 65 58 42 6C 64 41 41 53 54 47 70  kZHJUeXBldAASTGp
     * 0090: 68 64 6D 45 76 62 47 46 75 0A 5A 79 39 54 64 48  hdmEvbGFu.Zy9TdH <
     * 00A0: 4A 70 62 6D 63 37 65 48 42 30 41 41 52 30 5A 58  Jpbmc7eHB0AAR0ZX
     * 00B0: 4E 30 64 58 49 41 41 6C 74 43 72 50 4D 58 2B 41  N0dXIAAltCrPMX+A
     * 00C0: 59 49 56 4F 41 43 41 41 42 34 63 41 41 41 41 49  YIVOACAAB4cAAAAI
     * 00D0: 41 41 41 51 49 44 42 41 55 47 42 77 67 4A 43 67  AAAQIDBAUGBwgJCg
     * 00E0: 73 4D 44 51 34 50 0A 45 42 45 53 45 78 51 56 46  sMDQ4P.EBESExQVF <
     * 00F0: 68 63 59 47 52 6F 62 48 42 30 65 48 79 41 68 49  hcYGRobHB0eHyAhI
     * 0100: 69 4D 6B 4A 53 59 6E 4B 43 6B 71 4B 79 77 74 4C  iMkJSYnKCkqKywtL
     * 0110: 69 38 77 4D 54 49 7A 4E 44 55 32 4E 7A 67 35 4F  i8wMTIzNDU2Nzg5O
     * 0120: 6A 73 38 50 54 34 2F 51 45 46 43 51 30 52 46 52  js8PT4/QEFCQ0RFR
     * 0130: 6B 64 49 0A 53 55 70 4C 54 45 31 4F 54 31 42 52  kdI.SUpLTE1OT1BR <
     * 0140: 55 6C 4E 55 56 56 5A 58 57 46 6C 61 57 31 78 64  UlNUVVZXWFlaW1xd
     * 0150: 58 6C 39 67 59 57 4A 6A 5A 47 56 6D 5A 32 68 70  Xl9gYWJjZGVmZ2hp
     * 0160: 61 6D 74 73 62 57 35 76 63 48 46 79 63 33 52 31  amtsbW5vcHFyc3R1
     * 0170: 64 6E 64 34 65 58 70 37 66 48 31 2B 66 77 3D 3D  dnd4eXp7fH1+fw==
     * 0180: 0A                                                                <
     */
    private static final String OLD_ENCODING = "rO0ABXNyABpqYXZheC5uYW1pbmcuQmluYXJ5UmVmQWRkctCak7Les4hHAgABWwADYnVmdAACW0J4\ncgAUamF2YXgubmFtaW5nLlJlZkFkZHLroAeaAjivSgIAAUwACGFkZHJUeXBldAASTGphdmEvbGFu\nZy9TdHJpbmc7eHB0AAR0ZXN0dXIAAltCrPMX+AYIVOACAAB4cAAAAIAAAQIDBAUGBwgJCgsMDQ4P\nEBESExQVFhcYGRobHB0eHyAhIiMkJSYnKCkqKywtLi8wMTIzNDU2Nzg5Ojs8PT4/QEFCQ0RFRkdI\nSUpLTE1OT1BRUlNUVVZXWFlaW1xdXl9gYWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXp7fH1+fw==\n";

    /*
     * The new Base64 encoding uses '\r\n' as the line separator at 76 character
     * intervals:
     *
     * 0000: 72 4F 30 41 42 58 4E 79 41 42 70 71 59 58 5A 68  rO0ABXNyABpqYXZh
     * 0010: 65 43 35 75 59 57 31 70 62 6D 63 75 51 6D 6C 75  eC5uYW1pbmcuQmlu
     * 0020: 59 58 4A 35 55 6D 56 6D 51 57 52 6B 63 74 43 61  YXJ5UmVmQWRkctCa
     * 0030: 6B 37 4C 65 73 34 68 48 41 67 41 42 57 77 41 44  k7Les4hHAgABWwAD
     * 0040: 59 6E 56 6D 64 41 41 43 57 30 4A 34 0D 0A 63 67  YnVmdAACW0J4..cg <
     * 0050: 41 55 61 6D 46 32 59 58 67 75 62 6D 46 74 61 57  AUamF2YXgubmFtaW
     * 0060: 35 6E 4C 6C 4A 6C 5A 6B 46 6B 5A 48 4C 72 6F 41  5nLlJlZkFkZHLroA
     * 0070: 65 61 41 6A 69 76 53 67 49 41 41 55 77 41 43 47  eaAjivSgIAAUwACG
     * 0080: 46 6B 5A 48 4A 55 65 58 42 6C 64 41 41 53 54 47  FkZHJUeXBldAASTG
     * 0090: 70 68 64 6D 45 76 62 47 46 75 0D 0A 5A 79 39 54  phdmEvbGFu..Zy9T <
     * 00A0: 64 48 4A 70 62 6D 63 37 65 48 42 30 41 41 52 30  dHJpbmc7eHB0AAR0
     * 00B0: 5A 58 4E 30 64 58 49 41 41 6C 74 43 72 50 4D 58  ZXN0dXIAAltCrPMX
     * 00C0: 2B 41 59 49 56 4F 41 43 41 41 42 34 63 41 41 41  +AYIVOACAAB4cAAA
     * 00D0: 41 49 41 41 41 51 49 44 42 41 55 47 42 77 67 4A  AIAAAQIDBAUGBwgJ
     * 00E0: 43 67 73 4D 44 51 34 50 0D 0A 45 42 45 53 45 78  CgsMDQ4P..EBESEx <
     * 00F0: 51 56 46 68 63 59 47 52 6F 62 48 42 30 65 48 79  QVFhcYGRobHB0eHy
     * 0100: 41 68 49 69 4D 6B 4A 53 59 6E 4B 43 6B 71 4B 79  AhIiMkJSYnKCkqKy
     * 0110: 77 74 4C 69 38 77 4D 54 49 7A 4E 44 55 32 4E 7A  wtLi8wMTIzNDU2Nz
     * 0120: 67 35 4F 6A 73 38 50 54 34 2F 51 45 46 43 51 30  g5Ojs8PT4/QEFCQ0
     * 0130: 52 46 52 6B 64 49 0D 0A 53 55 70 4C 54 45 31 4F  RFRkdI..SUpLTE1O <
     * 0140: 54 31 42 52 55 6C 4E 55 56 56 5A 58 57 46 6C 61  T1BRUlNUVVZXWFla
     * 0150: 57 31 78 64 58 6C 39 67 59 57 4A 6A 5A 47 56 6D  W1xdXl9gYWJjZGVm
     * 0160: 5A 32 68 70 61 6D 74 73 62 57 35 76 63 48 46 79  Z2hpamtsbW5vcHFy
     * 0170: 63 33 52 31 64 6E 64 34 65 58 70 37 66 48 31 2B  c3R1dnd4eXp7fH1+
     * 0180: 66 77 3D 3D
     */
    private static final String NEW_ENCODING = "rO0ABXNyABpqYXZheC5uYW1pbmcuQmluYXJ5UmVmQWRkctCak7Les4hHAgABWwADYnVmdAACW0J4\r\ncgAUamF2YXgubmFtaW5nLlJlZkFkZHLroAeaAjivSgIAAUwACGFkZHJUeXBldAASTGphdmEvbGFu\r\nZy9TdHJpbmc7eHB0AAR0ZXN0dXIAAltCrPMX+AYIVOACAAB4cAAAAIAAAQIDBAUGBwgJCgsMDQ4P\r\nEBESExQVFhcYGRobHB0eHyAhIiMkJSYnKCkqKywtLi8wMTIzNDU2Nzg5Ojs8PT4/QEFCQ0RFRkdI\r\nSUpLTE1OT1BRUlNUVVZXWFlaW1xdXl9gYWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXp7fH1+fw==";

    /*
     * Binary-valued JNDI RefAddr test object
     */
    private static final RefAddr BINARY_REF_ADDR =
        new BinaryRefAddr("test", new byte[] {
        0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B,
        0x0C, 0x0D, 0x0E, 0x0F, 0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17,
        0x18, 0x19, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E, 0x1F, 0x20, 0x21, 0x22, 0x23,
        0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x2A, 0x2B, 0x2C, 0x2D, 0x2E, 0x2F,
        0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x3A, 0x3B,
        0x3C, 0x3D, 0x3E, 0x3F, 0x40, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47,
        0x48, 0x49, 0x4A, 0x4B, 0x4C, 0x4D, 0x4E, 0x4F, 0x50, 0x51, 0x52, 0x53,
        0x54, 0x55, 0x56, 0x57, 0x58, 0x59, 0x5A, 0x5B, 0x5C, 0x5D, 0x5E, 0x5F,
        0x60, 0x61, 0x62, 0x63, 0x64, 0x65, 0x66, 0x67, 0x68, 0x69, 0x6A, 0x6B,
        0x6C, 0x6D, 0x6E, 0x6F, 0x70, 0x71, 0x72, 0x73, 0x74, 0x75, 0x76, 0x77,
        0x78, 0x79, 0x7A, 0x7B, 0x7C, 0x7D, 0x7E, 0x7F
    });

    public static void main(String[] args) throws Exception {

        System.out.println("\nOriginal RefAddr object:\n" + BINARY_REF_ADDR);
        System.out.println("Old Base64 encoded serialized RefAddr object:\n" +
                OLD_ENCODING + "\n");
        System.out.println("Decode using new Base64 decoder...");
        deserialize(Base64.getMimeDecoder().decode(OLD_ENCODING));

        System.out.println("----");

        System.out.println("\nOriginal RefAddr object:\n" + BINARY_REF_ADDR);
        System.out.println("New Base64 encoded serialized RefAddr object:\n" +
            NEW_ENCODING + "\n");
        System.out.println("Decode using new Base64 decoder...");
        deserialize(Base64.getMimeDecoder().decode(NEW_ENCODING));

        System.out.println("----");
    }

    /*
     * Deserialize the decoded Base64 bytes to recover the BinaryRefAddr object.
     */
    private static void deserialize(byte[] bytes) throws Exception {

        ObjectInputStream objectStream =
            new ObjectInputStream(new ByteArrayInputStream(bytes));
        Object object = objectStream.readObject();
        if (!BINARY_REF_ADDR.equals(object)) {
            throw new Exception("Recovered object does not match the original");
        }
        System.out.println("Recovered RefAddr object:\n" + object);
    }

    /*
     * Dumps the encoding of a JNDI Reference object during an attempt to store
     * in an LDAP directory.
     */
    private static void storeObjectInLDAP() {
        Hashtable<Object, Object> env = new Hashtable<>();
        env.put(Context.REFERRAL, "follow"); // omit an LDAP control
        env.put("java.naming.ldap.version", "3"); // omit LDAP bind operation
        env.put("com.sun.jndi.ldap.trace.ber", System.err); // dump protocol
        try {
            DirContext ctx = new InitialDirContext(env);
            Reference reference = new Reference("test", BINARY_REF_ADDR);
            ctx.bind("ldap://ldap.example.com/cn=test", reference);
        } catch (NamingException ignore) {
        }
    }
}
