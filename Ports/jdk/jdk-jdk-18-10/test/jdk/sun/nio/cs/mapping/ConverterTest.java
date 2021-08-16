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
 * @summary test Bug 4199484
 * @modules jdk.charsets
 * @run main ConverterTest
 * @bug 4199484 4199599 4199601 4199602 4159519 4201529 4199604 4201532 4947038 6217210
*/

import java.util.*;
import java.io.*;

/*
 * (C) Copyright IBM Corp. 1999 - All Rights Reserved
 *
 * The original version of this source code and documentation is
 * copyrighted and owned by IBM. These materials are provided
 * under terms of a License Agreement between IBM and Sun.
 * This technology is protected by multiple US and International
 * patents. This notice and attribution to IBM may not be removed.
 */
public class ConverterTest extends TestFmwk {
    public static void main(String[] args) throws Exception {
        new ConverterTest().run(args);
    }

    public void test4199484() throws Exception {
        checkPages(new String[] { "Cp33722" });
    }

    public void test4199599() throws Exception {
        checkPages(new String[] { "Cp930", "Cp939" });
    }

    public void test4199601() throws Exception {
        checkPages(new String[] { "Cp942" });
    }

    public void test4199602() throws Exception {
        checkPages(new String[] { "Cp943" });
    }

    public void test6217210() throws Exception {
        checkPages(new String[] { "Cp833" });
    }

    public void test4159519() throws Exception {
        checkPages(new String[] { "Cp037", "Cp1025", "Cp1026", "Cp1112" });
        checkPages(new String[] { "Cp1122", "Cp1123", "Cp273", "Cp277" });
        checkPages(new String[] { "Cp278", "Cp280", "Cp284", "Cp285" });
        checkPages(new String[] { "Cp297", "Cp420", "Cp424", "Cp500" });
        checkPages(new String[] { "Cp838", "Cp870", "Cp871", "Cp875" });
        checkPages(new String[] { "Cp918", "Cp930", "Cp935", "Cp937" });
        checkPages(new String[] { "Cp939" });
    }

    public void test4201529() throws Exception {
        // Test fallback mapping for U+00B7
        byte[] b = new String("\u00B7").getBytes("Cp1381");

        int b1 = b[0] & 0xff;
        int b2 = b[1] & 0xff;

        if (b.length != 2 || b1 != 0xa1 || b2 != 0xa4)
            errln("Error in Converter: Cp1381");
    }

    public void test4199604() throws Exception {
        checkPages(new String[] { "Cp970" });
    }

    public void test4201532() throws Exception {
        checkPages(new String[] { "Cp1383" });
    }

    private static class Parameter {
        public final String value;
        public final boolean statefull;
        private final Vector nativeValues = new Vector();
        private final Vector unicodeValues = new Vector();

        public Parameter(final String param) throws IOException {
            final int ndx = param.indexOf(":");
            if (ndx >= 0) {
                value = param.substring(0, ndx);
            } else {
                value = param;
            }
            final String args = (ndx < 0) ? "" : param.substring(ndx+1);
            boolean isStatefull = false;
            for (int i = 0; i < args.length(); i++) {
                final char flag = args.charAt(i);
                switch (flag) {
                case 's':
                    isStatefull = true;
                    break;
                default:
                }
            }

            final String fileName = value+".b2c";
            final FileReader f = new FileReader(new File(System.getProperty("test.src", "."), fileName));
            final BufferedReader in = new BufferedReader(f);
            String line = in.readLine();
            while (line != null) {
                if (line.startsWith("#")) {
                    //ignore all comments except ones that indicate this is a
                    //statefull conversion.
                    if (line.indexOf("STATEFULL") > 0) {
                        isStatefull = true;
                    }
                } else {
                    final StringTokenizer tokenizer = new StringTokenizer(line);
                    String key = tokenizer.nextToken();
                    String value = tokenizer.nextToken();
                    if (key.startsWith("0x")) key = key.substring(2);
                    if (value.startsWith("0x")) value = value.substring(2);

                    final char c = (char)Integer.parseInt(value, 16);
                    final String unicodeValue = String.valueOf(c);

                    final long keyValue = Long.parseLong(key, 16);
                    if (isStatefull) {
                        final int keyLength = key.length();
                        if (keyLength == 2) {
                            byte[] nativeValue = new byte[1];
                            nativeValue[0] = (byte)((keyValue) & 0x00FF);
                            nativeValues.addElement(nativeValue);
                            unicodeValues.addElement(unicodeValue);
                        } else if (keyLength == 8) {
                            byte[] nativeValue = new byte[4];
                            nativeValue[0] = 0x0E;
                            nativeValue[1] = (byte)((keyValue >> 16) & 0x00FF);
                            nativeValue[2] = (byte)((keyValue >> 8) & 0x00FF);
                            nativeValue[3] = 0x0F;
                            nativeValues.addElement(nativeValue);
                            unicodeValues.addElement(unicodeValue);
                        } else {
                            System.err.println("Agh!");
                        }
                    } else {
                        if (key.length() == 2) {
                            byte[] nativeValue = new byte[1];
                            nativeValue[0] = (byte)(keyValue & 0x00FF);
                            nativeValues.addElement(nativeValue);
                            unicodeValues.addElement(unicodeValue);
                        } else if (key.length() == 4) {
                            byte[] nativeValue = new byte[2];
                            nativeValue[0] = (byte)((keyValue >> 8) & 0x00FF);
                            nativeValue[1] = (byte)((keyValue) & 0x00FF);
                            nativeValues.addElement(nativeValue);
                            unicodeValues.addElement(unicodeValue);
                        } else {
                            byte[] nativeValue = new byte[3];
                            nativeValue[0] = (byte)((keyValue >> 16) & 0x00FF);
                            nativeValue[1] = (byte)((keyValue >> 8) & 0x00FF);
                            nativeValue[2] = (byte)((keyValue) & 0x00FF);
                            nativeValues.addElement(nativeValue);
                            unicodeValues.addElement(unicodeValue);
                        }
                    }
                }
                line = in.readLine();
            }
            statefull = isStatefull;
        }

        public String toString() {
            return value;
        }

        public void getMapping(final Vector keys, final Vector values, final boolean toUnicode)
                throws IOException {
            final Hashtable temp = new Hashtable();
            for (int i = nativeValues.size() - 1; i >= 0; --i) {
                final byte[] key = (byte[])nativeValues.elementAt(i);
                final String value = (String)unicodeValues.elementAt(i);

                if (toUnicode) {
                    final String keyString = printable(key);
                    if (temp.get(keyString) == null) {
                        temp.put(keyString, keyString);
                        keys.addElement(key);
                        values.addElement(value);
                    }
                } else {
                    if (temp.get(value) == null) {
                        temp.put(value, value);
                        keys.addElement(value);
                        values.addElement(key);
                    }
                }
            }
        }
    }

    public void checkPages(String[] args) {
        for (int j = 0; j < args.length; j++) {
            logln("Checking converter: "+args[j]);
            boolean err = false;
            try {
                final Parameter param = new Parameter(args[j]);

                final Vector keys = new Vector();
                final Vector values = new Vector();

                param.getMapping(keys, values, true);
                for (int i = 0; i < keys.size(); i++) {
                    final byte[] key = (byte[])keys.elementAt(i);
                    final String value = (String)values.elementAt(i);
                    try {
                        final String actualValue = new String(key, param.value);
                        if (!value.equals(actualValue)) {
                            logln(printable(key)+" ==> "+printable(value)+" produced "+printable(actualValue));
                            err = true;
                        }
                    } catch (UnsupportedEncodingException e) {
                        logln(param.value+" encoding not supported: "+e);
                        err = true;
                        break;
                    }
                }

                keys.removeAllElements();
                values.removeAllElements();
                param.getMapping(keys, values, false);
                for (int i = 0; i < keys.size(); i++) {
                    final String key = (String)keys.elementAt(i);
                    final byte[] value = (byte[])values.elementAt(i);
                    try {
                        final byte[] actualValue = key.getBytes(param.value);
                        boolean diff = false;
                        if (value.length != actualValue.length) {
                            logln(printable(key)+" ==> "+printable(value)+" produced "+printable(actualValue));
                            err = true;
                        } else {
                            for (int k = 0; k < value.length; k++) {
                                if (value[k] != actualValue[k]) {
                                    logln(printable(key)+" ==> "+printable(value)+" produced "+printable(actualValue));
                                    err = true;
                                    break;
                                }
                            }
                        }
                    } catch (UnsupportedEncodingException e) {
                        logln(param.value+" encoding not supported: "+e);
                        err = true;
                        break;
                    }
                }
            } catch (IOException e) {
                logln("Could not load table: "+e);
                err = true;
            }
            if (err) {
                errln("Error in converter: "+args[j]);
            } else {
                logln("    passed.");
            }
        }
    }

    protected static String printable(String c) {
        final StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < c.length(); i++) {
            buffer.append(printable(c.charAt(i)));
        }
        return buffer.toString();
    }

    protected static String printable(byte c) {
        final StringBuffer buffer = new StringBuffer("0x");
        final int value = ((int)c) & 0x00FF;
        buffer.append(HEX_DIGIT[(value & 0x00F0) >> 4]);
        buffer.append(HEX_DIGIT[(value & 0x000F)]);
        return buffer.toString();
    }

    protected static String printable(byte[] c) {
        final StringBuffer buffer = new StringBuffer("[");
        for (int i = 0; i < c.length; i++) {
            final int value = ((int)c[i]) & 0x00FF;
            buffer.append(HEX_DIGIT[(value & 0x00F0) >> 4]);
            buffer.append(HEX_DIGIT[(value & 0x000F)]);
            buffer.append(" ");
        }
        buffer.append("]");
        return buffer.toString();
    }

    protected static String printable(char[] c) {
        final StringBuffer buffer = new StringBuffer("[");
        for (int i = 0; i < c.length; i++) {
            buffer.append(printable(c[i]));
        }
        buffer.append("]");
        return buffer.toString();
    }

    protected static String printable(char c) {
        final StringBuffer buffer = new StringBuffer("\\u");
        final int value = ((int)c) & 0xFFFF;
        buffer.append(HEX_DIGIT[(value & 0xF000) >> 12]);
        buffer.append(HEX_DIGIT[(value & 0x0F00) >> 8]);
        buffer.append(HEX_DIGIT[(value & 0x00F0) >> 4]);
        buffer.append(HEX_DIGIT[(value & 0x000F)]);
        return buffer.toString();
    }

    static final char[] HEX_DIGIT = {'0','1','2','3','4','5','6','7',
                     '8','9','A','B','C','D','E','F'};

}
