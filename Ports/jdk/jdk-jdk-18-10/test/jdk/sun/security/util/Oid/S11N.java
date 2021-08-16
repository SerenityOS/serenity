/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4811968 6908628 8006564 8130696 8242151
 * @modules java.base/sun.security.util
 * @run main S11N check
 * @summary Serialization compatibility with old versions (and fixes)
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import sun.security.util.ObjectIdentifier;

public class S11N {
    static String[] SMALL= {
        "0.0",
        "1.1",
        "2.2",
        "1.2.3456",
        "1.2.2147483647.4",
        "1.2.268435456.4",
    };

    static String[] HUGE = {
        "2.16.764.1.3101555394.1.0.100.2.1",
        "1.2.2147483648.4",
        "2.3.4444444444444444444444",
        "1.2.8888888888888888.33333333333333333.44444444444444",
    };

    public static void main(String[] args) throws Exception {
        if (args[0].equals("check")) {
            String jv = System.getProperty("java.version");
            // java.version format: $VNUM\-$PRE
            String [] va = (jv.split("-")[0]).split("\\.");
            String v = (va.length == 1 || !va[0].equals("1")) ? va[0] : va[1];
            int version = Integer.valueOf(v);
            System.out.println("version is " + version);
            if (version >= 7) {
                for (String oid: SMALL) {
                    // 7 -> 7
                    check(out(oid), oid);
                    // 6 -> 7
                    check(out6(oid), oid);
                }
                for (String oid: HUGE) {
                    // 7 -> 7
                    check(out(oid), oid);
                }
            } else {
                for (String oid: SMALL) {
                    // 6 -> 6
                    check(out(oid), oid);
                    // 7 -> 6
                    check(out7(oid), oid);
                }
                for (String oid: HUGE) {
                    // 7 -> 6 fails for HUGE oids
                    boolean ok = false;
                    try {
                        check(out7(oid), oid);
                        ok = true;
                    } catch (Exception e) {
                        System.out.println(e);
                    }
                    if (ok) {
                        throw new Exception();
                    }
                }
            }
        } else {
            // Generates the JDK6 serialized string inside this test, call
            //      $JDK7/bin/java S11N dump7
            //      $JDK6/bin/java S11N dump6
            // and paste the output at the end of this test.
            dump(args[0], SMALL);
            // For jdk6, the following line will throw an exception, ignore it
            dump(args[0], HUGE);
        }
    }

    // Gets the serialized form for jdk6
    private static byte[] out6(String oid) throws Exception {
        return decode(dump6.get(oid));
    }

    // Gets the serialized form for jdk7
    private static byte[] out7(String oid) throws Exception {
        return decode(dump7.get(oid));
    }

    // Gets the serialized form for this java
    private static byte[] out(String oid) throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        new ObjectOutputStream(bout).writeObject(ObjectIdentifier.of(oid));
        return bout.toByteArray();
    }

    // Makes sure serialized form can be deserialized
    private static void check(byte[] in, String oid) throws Exception {
        ObjectIdentifier o = (ObjectIdentifier) (
                new ObjectInputStream(new ByteArrayInputStream(in)).readObject());
        if (!o.toString().equals(oid)) {
            throw new Exception("Read Fail " + o + ", not " + oid);
        }
    }

    // dump serialized form to java code style text
    private static void dump(String title, String[] oids) throws Exception {
        for (String oid: oids) {
            String hex = encode(out(oid));
            System.out.println("        " + title + ".put(\"" + oid + "\",");
            for (int i = 0; i<hex.length(); i+= 64) {
                int end = i + 64;
                if (end > hex.length()) {
                    end = hex.length();
                }
                System.out.print("            \"" + hex.substring(i, end) + "\"");
                if (end == hex.length()) {
                    System.out.println(");");
                } else {
                    System.out.println(" +");
                }
            }
        }
    }

    private static String encode(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b: bytes) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    private static byte[] decode(String var) {
        byte[] data = new byte[var.length()/2];
        for (int i=0; i<data.length; i++) {
            data[i] = Integer.valueOf(var.substring(2 * i, 2 * i + 2), 16).byteValue();
        }
        return data;
    }

    // Do not use diamond operator, this test is also meant to run in jdk6
    private static Map<String,String> dump7 = new HashMap<String,String>();
    private static Map<String,String> dump6 = new HashMap<String,String>();

    static {
        //////////////  PASTE BEGIN //////////////
        dump7.put("0.0",
            "aced00057372002273756e2e73656375726974792e7574696c2e4f626a656374" +
            "4964656e74696669657278b20eec64177f2e03000349000c636f6d706f6e656e" +
            "744c656e4c000a636f6d706f6e656e74737400124c6a6176612f6c616e672f4f" +
            "626a6563743b5b0008656e636f64696e677400025b4278700000000275720002" +
            "5b494dba602676eab2a50200007870000000020000000000000000757200025b" +
            "42acf317f8060854e00200007870000000010078");
        dump7.put("1.1",
            "aced00057372002273756e2e73656375726974792e7574696c2e4f626a656374" +
            "4964656e74696669657278b20eec64177f2e03000349000c636f6d706f6e656e" +
            "744c656e4c000a636f6d706f6e656e74737400124c6a6176612f6c616e672f4f" +
            "626a6563743b5b0008656e636f64696e677400025b4278700000000275720002" +
            "5b494dba602676eab2a50200007870000000020000000100000001757200025b" +
            "42acf317f8060854e00200007870000000012978");
        dump7.put("2.2",
            "aced00057372002273756e2e73656375726974792e7574696c2e4f626a656374" +
            "4964656e74696669657278b20eec64177f2e03000349000c636f6d706f6e656e" +
            "744c656e4c000a636f6d706f6e656e74737400124c6a6176612f6c616e672f4f" +
            "626a6563743b5b0008656e636f64696e677400025b4278700000000275720002" +
            "5b494dba602676eab2a50200007870000000020000000200000002757200025b" +
            "42acf317f8060854e00200007870000000015278");
        dump7.put("1.2.3456",
            "aced00057372002273756e2e73656375726974792e7574696c2e4f626a656374" +
            "4964656e74696669657278b20eec64177f2e03000349000c636f6d706f6e656e" +
            "744c656e4c000a636f6d706f6e656e74737400124c6a6176612f6c616e672f4f" +
            "626a6563743b5b0008656e636f64696e677400025b4278700000000375720002" +
            "5b494dba602676eab2a5020000787000000003000000010000000200000d8075" +
            "7200025b42acf317f8060854e00200007870000000032a9b0078");
        dump7.put("1.2.2147483647.4",
            "aced00057372002273756e2e73656375726974792e7574696c2e4f626a656374" +
            "4964656e74696669657278b20eec64177f2e03000349000c636f6d706f6e656e" +
            "744c656e4c000a636f6d706f6e656e74737400124c6a6176612f6c616e672f4f" +
            "626a6563743b5b0008656e636f64696e677400025b4278700000000475720002" +
            "5b494dba602676eab2a502000078700000000400000001000000027fffffff00" +
            "000004757200025b42acf317f8060854e00200007870000000072a87ffffff7f" +
            "0478");
        dump7.put("1.2.268435456.4",
            "aced00057372002273756e2e73656375726974792e7574696c2e4f626a656374" +
            "4964656e74696669657278b20eec64177f2e03000349000c636f6d706f6e656e" +
            "744c656e4c000a636f6d706f6e656e74737400124c6a6176612f6c616e672f4f" +
            "626a6563743b5b0008656e636f64696e677400025b4278700000000475720002" +
            "5b494dba602676eab2a502000078700000000400000001000000021000000000" +
            "000004757200025b42acf317f8060854e00200007870000000072a8180808000" +
            "0478");
        dump7.put("2.16.764.1.3101555394.1.0.100.2.1",
            "aced00057372002273756e2e73656375726974792e7574696c2e4f626a656374" +
            "4964656e74696669657278b20eec64177f2e03000349000c636f6d706f6e656e" +
            "744c656e4c000a636f6d706f6e656e74737400124c6a6176612f6c616e672f4f" +
            "626a6563743b5b0008656e636f64696e677400025b427870ffffffff7372003e" +
            "73756e2e73656375726974792e7574696c2e4f626a6563744964656e74696669" +
            "657224487567654f69644e6f74537570706f7274656442794f6c644a444b0000" +
            "0000000000010200007870757200025b42acf317f8060854e002000078700000" +
            "000e60857c018bc6f7f542010064020178");
        dump7.put("1.2.2147483648.4",
            "aced00057372002273756e2e73656375726974792e7574696c2e4f626a656374" +
            "4964656e74696669657278b20eec64177f2e03000349000c636f6d706f6e656e" +
            "744c656e4c000a636f6d706f6e656e74737400124c6a6176612f6c616e672f4f" +
            "626a6563743b5b0008656e636f64696e677400025b427870ffffffff7372003e" +
            "73756e2e73656375726974792e7574696c2e4f626a6563744964656e74696669" +
            "657224487567654f69644e6f74537570706f7274656442794f6c644a444b0000" +
            "0000000000010200007870757200025b42acf317f8060854e002000078700000" +
            "00072a88808080000478");
        dump7.put("2.3.4444444444444444444444",
            "aced00057372002273756e2e73656375726974792e7574696c2e4f626a656374" +
            "4964656e74696669657278b20eec64177f2e03000349000c636f6d706f6e656e" +
            "744c656e4c000a636f6d706f6e656e74737400124c6a6176612f6c616e672f4f" +
            "626a6563743b5b0008656e636f64696e677400025b427870ffffffff7372003e" +
            "73756e2e73656375726974792e7574696c2e4f626a6563744964656e74696669" +
            "657224487567654f69644e6f74537570706f7274656442794f6c644a444b0000" +
            "0000000000010200007870757200025b42acf317f8060854e002000078700000" +
            "000c5383e1ef87a4d1bdebc78e1c78");
        dump7.put("1.2.8888888888888888.33333333333333333.44444444444444",
            "aced00057372002273756e2e73656375726974792e7574696c2e4f626a656374" +
            "4964656e74696669657278b20eec64177f2e03000349000c636f6d706f6e656e" +
            "744c656e4c000a636f6d706f6e656e74737400124c6a6176612f6c616e672f4f" +
            "626a6563743b5b0008656e636f64696e677400025b427870ffffffff7372003e" +
            "73756e2e73656375726974792e7574696c2e4f626a6563744964656e74696669" +
            "657224487567654f69644e6f74537570706f7274656442794f6c644a444b0000" +
            "0000000000010200007870757200025b42acf317f8060854e002000078700000" +
            "00182a8fe58cdbc5ae9c38bb9b8fd7a48daa558a8dc0bacb8e1c78");

        dump6.put("0.0",
            "aced00057372002273756e2e73656375726974792e7574696c2e4f626a656374" +
            "4964656e74696669657278b20eec64177f2e02000249000c636f6d706f6e656e" +
            "744c656e5b000a636f6d706f6e656e74737400025b4978700000000275720002" +
            "5b494dba602676eab2a50200007870000000020000000000000000");
        dump6.put("1.1",
            "aced00057372002273756e2e73656375726974792e7574696c2e4f626a656374" +
            "4964656e74696669657278b20eec64177f2e02000249000c636f6d706f6e656e" +
            "744c656e5b000a636f6d706f6e656e74737400025b4978700000000275720002" +
            "5b494dba602676eab2a50200007870000000020000000100000001");
        dump6.put("2.2",
            "aced00057372002273756e2e73656375726974792e7574696c2e4f626a656374" +
            "4964656e74696669657278b20eec64177f2e02000249000c636f6d706f6e656e" +
            "744c656e5b000a636f6d706f6e656e74737400025b4978700000000275720002" +
            "5b494dba602676eab2a50200007870000000020000000200000002");
        dump6.put("1.2.3456",
            "aced00057372002273756e2e73656375726974792e7574696c2e4f626a656374" +
            "4964656e74696669657278b20eec64177f2e02000249000c636f6d706f6e656e" +
            "744c656e5b000a636f6d706f6e656e74737400025b4978700000000375720002" +
            "5b494dba602676eab2a5020000787000000003000000010000000200000d80");
        dump6.put("1.2.2147483647.4",
            "aced00057372002273756e2e73656375726974792e7574696c2e4f626a656374" +
            "4964656e74696669657278b20eec64177f2e02000249000c636f6d706f6e656e" +
            "744c656e5b000a636f6d706f6e656e74737400025b4978700000000475720002" +
            "5b494dba602676eab2a502000078700000000400000001000000027fffffff00" +
            "000004");
        dump6.put("1.2.268435456.4",
            "aced00057372002273756e2e73656375726974792e7574696c2e4f626a656374" +
            "4964656e74696669657278b20eec64177f2e02000249000c636f6d706f6e656e" +
            "744c656e5b000a636f6d706f6e656e74737400025b4978700000000475720002" +
            "5b494dba602676eab2a502000078700000000400000001000000021000000000" +
            "000004");

        //////////////  PASTE END //////////////
    }
}
