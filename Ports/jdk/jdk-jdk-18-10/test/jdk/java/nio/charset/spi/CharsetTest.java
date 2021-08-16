/*
 * Copyright (c) 2010, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Iterator;
import java.util.SortedMap;

public class CharsetTest {

    private static PrintStream out = System.err;
    private static final SortedMap available = Charset.availableCharsets();

    private static void fail(String csn, String msg) {
        throw new RuntimeException(csn + ": " + msg);
    }

    private static void testPositive(String csn) {
        if (!Charset.isSupported(csn))
            fail(csn, "Not supported");

        Charset cs = Charset.forName(csn);
        out.println(csn + " --> " + cs.getClass().getName());
        out.println("  " + cs.name() + " " + cs.aliases());

        if (!available.containsKey(cs.name()))
            fail(csn, "Not in available charsets: " + available.keySet());
        if (!((Charset)available.get(cs.name())).equals(cs))
            fail(csn, "Available charset != looked-up charset");

        if (csn.equalsIgnoreCase("FOO")) {
            if (!(cs instanceof FooCharset))
                fail(csn, "instanceof failed");
        }
    }

    private static void testNegative(String csn) {
        if (Charset.isSupported(csn))
            fail(csn, "Supported");
        if (available.containsKey(csn))
            fail(csn, "Available");
        try {
            Charset.forName(csn);
        } catch (UnsupportedCharsetException x) {
            out.println(csn + " not supported, as expected");
            return;
        }
        fail(csn, "Lookup succeeded");
    }

    public static void main(String [] args) {

        out.println("Default: "
                    + new InputStreamReader(System.in).getEncoding());

        out.print("Available:");
        for (Iterator i = available.keySet().iterator(); i.hasNext();)
            out.print(" " + (String)i.next());
        out.println();

        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            boolean not = a.startsWith("!");
            String csn = (not ? a.substring(1) : a);
            if (not)
                testNegative(csn);
            else
                testPositive(csn);
        }
    }

}
