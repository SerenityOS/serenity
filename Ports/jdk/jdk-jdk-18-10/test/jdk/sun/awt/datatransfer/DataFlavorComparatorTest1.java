/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

/* @test
   @bug 8058473
   @summary "Comparison method violates its general contract" when using Clipboard
            Ensure that DataFlavorComparator conforms to Comparator contract
   @author Anton Nashatyrev
   @modules java.datatransfer/sun.datatransfer
   @run main DataFlavorComparatorTest1
*/
import sun.datatransfer.DataFlavorUtil;

import java.awt.datatransfer.DataFlavor;
import java.util.Comparator;

public class DataFlavorComparatorTest1 {

    public static void main(String[] args) throws Exception {
        String[] mimes = new String[] {
                "text/plain;class=java.nio.ByteBuffer;charset=UTF-8",
                "text/uri-list;class=java.nio.ByteBuffer;charset=UTF-8",
                "text/plain;class=java.nio.ByteBuffer;charset=UTF-16LE",
                "text/uri-list;class=java.nio.ByteBuffer;charset=UTF-16LE",
                "application/x-java-text-encoding",
                "application/x-java-serialized-object;class=java.lang.String",
                "text/plain;class=java.io.InputStream;charset=UTF-8",
                "text/uri-list;class=java.io.InputStream;charset=UTF-8",
                "text/plain;class=java.io.InputStream;charset=windows-1252",
                "text/uri-list;class=java.io.InputStream;charset=windows-1252",
                "application/x-java-url;class=java.net.URL",
                "text/plain;class=java.io.Reader",
                "text/plain;charset=windows-1252",
                "text/uri-list;class=java.io.Reader",
                "text/uri-list;charset=windows-1252",
                "text/plain;charset=UTF-8",
                "text/uri-list;charset=UTF-8",
                "text/plain;class=java.io.InputStream;charset=US-ASCII",
                "text/uri-list;class=java.io.InputStream;charset=US-ASCII",
                "text/plain;class=java.io.InputStream;charset=UTF-16LE",
                "text/plain;charset=US-ASCII",
                "text/uri-list;class=java.io.InputStream;charset=UTF-16LE",
                "text/uri-list;charset=US-ASCII",
                "text/plain;charset=UTF-16LE",
                "text/uri-list;charset=UTF-16LE",
                "text/plain;class=java.nio.ByteBuffer;charset=UTF-16BE",
                "text/uri-list;class=java.nio.ByteBuffer;charset=UTF-16BE",
                "text/plain;class=java.nio.ByteBuffer;charset=ISO-8859-1",
                "text/uri-list;class=java.nio.ByteBuffer;charset=ISO-8859-1",
                "text/plain",
                "text/uri-list",
                "text/plain;class=java.nio.ByteBuffer;charset=UTF-16",
                "text/uri-list;class=java.nio.ByteBuffer;charset=UTF-16",
                "text/plain;class=java.io.InputStream;charset=unicode",
                "text/uri-list;class=java.io.InputStream;charset=UTF-16",
                "text/plain;class=java.nio.CharBuffer",
                "text/uri-list;class=java.nio.CharBuffer",
                "text/plain;class=java.lang.String",
                "text/plain;charset=UTF-16BE",
                "text/uri-list;class=java.lang.String",
                "text/uri-list;charset=UTF-16BE",
                "text/plain;charset=ISO-8859-1",
                "text/uri-list;charset=ISO-8859-1",
                "text/plain;class=java.io.InputStream;charset=UTF-16BE",
                "text/uri-list;class=java.io.InputStream;charset=UTF-16BE",
                "text/plain;class=java.nio.ByteBuffer;charset=US-ASCII",
                "text/uri-list;class=java.nio.ByteBuffer;charset=US-ASCII",
                "text/plain;class=java.io.InputStream;charset=ISO-8859-1",
                "text/uri-list;class=java.io.InputStream;charset=ISO-8859-1",
                "text/plain;charset=UTF-16",
                "text/plain;class=java.nio.ByteBuffer;charset=windows-1252",
                "text/uri-list;charset=UTF-16",
                "text/uri-list;class=java.nio.ByteBuffer;charset=windows-1252",
                "text/plain;class=java.io.InputStream;charset=windows-1252",
                "text/uri-list;class=java.io.InputStream;charset=windows-1252",
        };

        DataFlavor[] flavors = new DataFlavor[mimes.length];
        for (int i = 0; i < flavors.length; i++) {
            flavors[i] = new DataFlavor(mimes[i]);
        }

        testComparator(DataFlavorUtil.getDataFlavorComparator(), flavors);

        System.out.println("Passed.");
    }

    private static void testComparator(Comparator cmp, DataFlavor[] flavs)
            throws ClassNotFoundException {

        for (DataFlavor x: flavs) {
            for (DataFlavor y: flavs) {
                if (Math.signum(cmp.compare(x,y)) != -Math.signum(cmp.compare(y,x))) {
                    throw new RuntimeException("Antisymmetry violated: " + x + ", " + y);
                }
                if (cmp.compare(x,y) == 0 && !x.equals(y)) {
                    throw new RuntimeException("Equals rule violated: " + x + ", " + y);
                }
                for (DataFlavor z: flavs) {
                    if (cmp.compare(x,y) == 0) {
                        if (Math.signum(cmp.compare(x, z)) != Math.signum(cmp.compare(y, z))) {
                            throw new RuntimeException("Transitivity (1) violated: " + x + ", " + y + ", " + z);
                        }
                    } else {
                        if (Math.signum(cmp.compare(x, y)) == Math.signum(cmp.compare(y, z))) {
                            if (Math.signum(cmp.compare(x, y)) != Math.signum(cmp.compare(x, z))) {
                                throw new RuntimeException("Transitivity (2) violated: " + x + ", " + y + ", " + z);
                            }
                        }
                    }
                }
            }
        }
    }
}
