/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.test.lib.hexdump;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Objects;


/**
 * A simple command line StreamDump utility for files.
 * Usage: [-formatter <class name>] <files>
 *
 * The fully qualified class name of a HexPrinter.Formatter can be supplied using `--formatter`.
 * If none is supplied, it attempts to guess the type (a ObjectStream)
 * or an ASN.1 encoded certificate; otherwise the default is HexPrinter.Formatters.PRINTABLE.
 */
public class StreamDump {
    static HexPrinter.Formatter defaultFormatter = null;

    public static void main(String[] args) {
        try {
            List<String> argList = parseOptions(args);
            dumpFiles(argList);
        } catch (IllegalArgumentException iae) {
            System.out.println(iae.getMessage());
            usage();
            System.exit(1);
        }
    }

    /**
     * Parse command line options and return any remaining (filename) arguments.
     * @param arg the array of string arguments to main
     * @return A
     * @throws IllegalArgumentException if any arguments cannot be parsed
     */
    private static List<String> parseOptions(String[] arg) {
        for (int i = 0; i < arg.length; i++) {
            if (!arg[i].startsWith("--")) {
                if (arg[i].startsWith("-"))
                    throw new IllegalArgumentException("options start with '--', not single '-'");
                return Arrays.asList(Arrays.copyOfRange(arg, i, arg.length));
            }
            if (arg[i].equals("--formatter")) {
                if (++i >= arg.length)
                    throw new IllegalArgumentException("Formatter class name missing");
                String fmtName = arg[i];
                try {
                    defaultFormatter = findFormatter(Class.forName(fmtName));
                } catch (ClassNotFoundException e) {
                    throw new IllegalArgumentException("Formatter class not found: " + fmtName);
                }
            }
        }
        throw new IllegalArgumentException("No file arguments");
    }

    private static void usage() {
        System.out.println("Usage: [--formatter <class name>] <files>");
    }

    /**
     * Dump the file preceded by the title.
     * If the formatter is null, the type of content is guessed to choose a formatter.
     *
     * @param args an array of file names
     */
    static void dumpFiles(List<String> args) {
        var beforeFileSpacing = "";
        for (int i = 0; i < args.size(); i++) {
            var file = args.get(i);
            try {
                System.out.print(beforeFileSpacing);
                var title = (args.size() > 1) ? String.format("File: %s%n" , file) : "";
                dumpFile(Path.of(file), defaultFormatter, title);
                beforeFileSpacing = System.lineSeparator();
            } catch (FileNotFoundException | NoSuchFileException fnf) {
                System.out.printf("File: %s file not found%n", file);
            } catch (IOException ioe) {
                System.out.printf("File: %s %s: %s%n", file, ioe.getClass().getName(), ioe.getMessage());
            }
        }
    }

    /**
     * Dump the file preceded by the title.
     * If the formatter is null, the type of content is guessed to choose a formatter.
     *
     * @param file a file name, not null
     * @param defaultFormatter a HexPrinter formatter, may be null
     * @param title a title, may be empty, not null
     * @throws IOException if an exception occurs
     */
    static void dumpFile(Path file, HexPrinter.Formatter defaultFormatter, String title) throws IOException {
        Objects.requireNonNull(file, "filename");
        Objects.requireNonNull(title, "title");
        try (InputStream fis = Files.newInputStream(file)) {
            System.out.print(title);
            dumpFile(fis, defaultFormatter);
        }
    }

    static void dumpFile(InputStream fis, HexPrinter.Formatter formatter) throws IOException {
        try (BufferedInputStream is = new BufferedInputStream(fis)) {
            is.mark(1024);

            InputStream decoded = decodeMaybe(is);
            if (!decoded.equals(is)) {
                if (formatter == null)
                    formatter = findFormatter(ASN1Formatter.class);     // Assume encoded ASN.1
                decoded = new BufferedInputStream(decoded);
                decoded.mark(1024);
            } else {
                decoded.reset();
            }
            if (formatter == null && guessSerializable(decoded)) {
                // Select formatter for a serializable stream
                formatter = findFormatter(ObjectStreamPrinter.class);
            }
            decoded.reset();

            if (formatter == null)
                formatter = HexPrinter.Formatters.PRINTABLE;
            HexPrinter.simple()
                    .formatter(formatter)
                    .dest(System.out)
                    .format(decoded);
        }
    }

    /**
     * If the stream looks like Base64 Mime, return a stream to decode it.
     * @param is InputStream
     * @return an InputStream, unchanged unless it is Base64 Mime
     * @throws IOException if an I/O Error occurs
     */
    static InputStream decodeMaybe(InputStream is) throws IOException {
        DataInputStream dis = new DataInputStream(is);
        is.mark(1024);
        String line1 = dis.readLine();
        if (line1.startsWith("-----")) {
            return Base64.getMimeDecoder().wrap(is);
        }
        is.reset();
        return is;
    }

    static boolean guessSerializable(InputStream is) throws IOException {
        byte[] bytes = new byte[4];
        int l = is.read(bytes, 0, bytes.length);
        if (l >= 4 && (bytes[0] & 0xff) == 0xAC && (bytes[1] & 0xff) == 0xED &&
                bytes[2] == 0x00 && bytes[3] == 0x05) {
            return true;
        }
        return false;
    }

    private static HexPrinter.Formatter findFormatter(Class<?> clazz) {
        Method[] methods = clazz.getDeclaredMethods();
        for (Method m : methods) {
            if (m.getReturnType() == clazz &&
                    m.getParameterCount() == 0 &&
                    Modifier.isStatic(m.getModifiers()) &&
                    Modifier.isPublic(m.getModifiers())) {
                try {
                    return (HexPrinter.Formatter)m.invoke(null);
                } catch (IllegalAccessException  | InvocationTargetException e) {
                    e.printStackTrace();
                    // fall through
                }
            }
        }

        Constructor<?>[] cons = clazz.getDeclaredConstructors();
        for (Constructor<?> m : cons) {
            if (m.getParameterCount() == 0 &&
                    Modifier.isPublic(m.getModifiers())) {
                try {
                    return (HexPrinter.Formatter)m.newInstance();
                } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
                    e.printStackTrace();
                    // fall through
                }
            }
        }
        throw new RuntimeException("No formatter for class " + clazz.getName());
    }
}
