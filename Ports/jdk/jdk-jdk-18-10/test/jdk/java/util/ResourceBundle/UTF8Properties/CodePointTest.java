/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8027607
 * @summary Test UTF-8 based properties files can be loaded successfully,
 * @run main CodePointTest
 * @run main/othervm -Djava.util.PropertyResourceBundle.encoding=ISO-8859-1 CodePointTest
 * @run main/othervm -Djava.util.PropertyResourceBundle.encoding=UTF-8 CodePointTest
 */

import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import static java.util.ResourceBundle.Control;
import java.util.stream.*;

/*
 * Dumps every legal characters in ISO-8859-1/UTF-8 into
 * a <CharSet>.properties file. Each entry has a form of
 * "keyXXXX=c", where "XXXX" is a code point (variable length)
 * and "c" is the character encoded in the passed character set.
 * Then, load it with ResourceBundle.Control.newBundle() and compare both
 * contents. This confirms the following two functions:
 *  - For UTF-8.properties, UTF-8 code points are loaded correctly
 *  - For ISO-8859-1.properties, UTF-8->ISO-8859-1 fallback works
 *
 * Does the same test with "java.util.PropertyResourceBundle.encoding"
 * to "ISO-8859-1", and confirms only UTF-8 properties loading fails.
 */
public class CodePointTest {
    static final Charset[] props = {StandardCharsets.ISO_8859_1,
                                    StandardCharsets.UTF_8,
                                    StandardCharsets.US_ASCII};
    static final String encoding =
        System.getProperty("java.util.PropertyResourceBundle.encoding", "");

    public static void main(String[] args) {
        for (Charset cs : props) {
            try {
                checkProps(cs,
                    cs == StandardCharsets.UTF_8 &&
                    encoding.equals("ISO-8859-1"));

                if (cs == StandardCharsets.ISO_8859_1 &&
                    encoding.equals("UTF-8")) {
                    // should not happen
                    throw new RuntimeException("Reading ISO-8859-1 properties in "+
                        "strict UTF-8 encoding should throw an exception");
                }
            } catch (IOException e) {
                if ((e instanceof MalformedInputException ||
                     e instanceof UnmappableCharacterException) &&
                    cs == StandardCharsets.ISO_8859_1 &&
                    encoding.equals("UTF-8")) {
                    // Expected exception is correctly detected.
                } else {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    static void checkProps(Charset cs, boolean shouldFail) throws IOException {
        int start = Character.MIN_CODE_POINT;
        int end= 0;

        switch (cs.name()) {
        case "ISO-8859-1":
            end = 0xff;
            break;
        case "UTF-8":
            end = Character.MAX_CODE_POINT;
            break;
        case "US-ASCII":
            end = 0x7f;
            break;
        default:
            assert false;
        }

        Properties p = new Properties();
        String outputName = cs.name() + ".properties";

        // Forget previous test artifacts
        ResourceBundle.clearCache();

        IntStream.range(start, end+1).forEach(c ->
            {
                if (Character.isDefined(c) &&
                    (Character.isSupplementaryCodePoint(c) ||
                     !Character.isSurrogate((char)c))) {
                    p.setProperty("key"+Integer.toHexString(c),
                        Character.isSupplementaryCodePoint(c) ?
                            String.valueOf(Character.toChars(c)) :
                            Character.toString((char)c));
                }
            }
        );

        try (BufferedWriter bw = Files.newBufferedWriter(
                 FileSystems.getDefault().getPath(System.getProperty("test.classes", "."),
                 outputName), cs)) {
            p.store(bw, null);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        // try loading it
        Control c = Control.getControl(Control.FORMAT_PROPERTIES);
        ResourceBundle rb;
        try {
            rb = c.newBundle(cs.name(), Locale.ROOT, "java.properties",
                        CodePointTest.class.getClassLoader(), false);
        } catch (IllegalAccessException |
                 InstantiationException ex) {
            throw new RuntimeException(ex);
        }
        Properties result = new Properties();
        rb.keySet().stream().forEach((key) -> {
            result.setProperty(key, rb.getString(key));
        });

        if (!p.equals(result) && !shouldFail) {
            System.out.println("Charset: "+cs);
            rb.keySet().stream().sorted().forEach((key) -> {
                if (!p.getProperty(key).equals(result.getProperty(key))) {
                    System.out.println(key+": file: "+p.getProperty(key)+", RB: "+result.getProperty(key));
                }
            });
            throw new RuntimeException("not equal!");
        }
    }
}
