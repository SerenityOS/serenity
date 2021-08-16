/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8072773
 * @library /test/lib /lib/testlibrary/bootlib
 * @build java.base/java.util.stream.OpTestCase
 *        jdk.test.lib.RandomFactory
 * @run testng/othervm StreamLinesTest
 * @summary Tests streams returned from Files.lines, primarily focused on
 *          testing the file-channel-based stream stream with supported
 *          character sets
 * @key randomness
 */

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.OpTestCase;
import java.util.stream.Stream;
import java.util.stream.TestData;
import jdk.test.lib.RandomFactory;

public class StreamLinesTest extends OpTestCase {

    enum LineSeparator {
        NONE(""),
        N("\n"),
        R("\r"),
        RN("\r\n");

        public final String value;

        LineSeparator(String value) {
            this.value = value;
        }

        public String toString() {
            return name();
        }
    }

    static Path generateTempFileWithLines(IntFunction<String> lineGenerator,
                                          IntFunction<LineSeparator> lineSeparatorGenerator,
                                          int lines, Charset cs, boolean endLineSep) throws IOException {
        Path p = Files.createTempFile("lines", null);
        BufferedWriter bw = Files.newBufferedWriter(p, cs);

        for (int i = 0; i < lines - 1; i++) {
            bw.write(lineGenerator.apply(i));
            bw.write(lineSeparatorGenerator.apply(i).value);
        }
        if (lines > 0) {
            bw.write(lineGenerator.apply(lines - 1));
            if (endLineSep)
                bw.write(lineSeparatorGenerator.apply(lines - 1).value);
        }

        bw.flush();
        bw.close();
        p.toFile().deleteOnExit();

        return p;
    }

    static void writeLineSeparator(Path p,
                                   IntFunction<LineSeparator> lineSeparatorGenerator,
                                   int lines, Charset cs) throws IOException {
        BufferedWriter bw = Files.newBufferedWriter(p, cs, StandardOpenOption.APPEND);
        bw.write(lineSeparatorGenerator.apply(lines - 1).value);
        bw.flush();
        bw.close();
    }

    static List<String> readAllLines(Path path, Charset cs) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(path, cs)) {
            List<String> result = new ArrayList<>();
            for (; ; ) {
                String line = reader.readLine();
                if (line == null)
                    break;
                result.add(line);
            }
            return result;
        }
    }

    static Object[] of(String description, IntFunction<String> lineGenerator,
                       IntFunction<LineSeparator> separatorGenerator, int n, Charset cs) {
        return new Object[]{description, lineGenerator, separatorGenerator, n, cs};
    }

    private static final Random random = RandomFactory.getRandom();

    @DataProvider
    public static Object[][] lines() {
        List<Object[]> l = new ArrayList<>();

        // Include the three supported optimal-line charsets and one
        // which does not
        List<Charset> charsets = Arrays.asList(StandardCharsets.UTF_8,
                                               StandardCharsets.US_ASCII,
                                               StandardCharsets.ISO_8859_1,
                                               StandardCharsets.UTF_16);
        String[] lines = {"", "A", "AB", "ABC", "ABCD"};
        int[] linesSizes = {0, 1, 2, 3, 4, 16, 256, 1024};

        for (Charset charset : charsets) {
            for (int linesSize : linesSizes) {
                if (linesSize > 0) {
                    for (String line : lines) {
                        for (LineSeparator ls : EnumSet.complementOf(EnumSet.of(LineSeparator.NONE))) {
                            String description = String.format("%d lines of \"%s\" with separator %s", linesSize, line, ls);
                            l.add(of(description,
                                    i -> line,
                                    i -> ls,
                                    linesSize, charset));
                        }
                    }
                } else {
                    l.add(of("Empty file: 0 lines",
                            i -> "",
                            i -> LineSeparator.NONE,
                            0, charset));
                }
            }
        }

        for (Charset charset : charsets) {
            l.add(of("A maximum of 1024 random lines and separators",
                     i -> lines[1 + random.nextInt(lines.length - 1)],
                     i -> LineSeparator.values()[random.nextInt(LineSeparator.values().length)],
                     1024, charset));
        }

        for (Charset charset : charsets) {
            l.add(of("One large line with no separators",
                     i -> "ABCD",
                     i -> LineSeparator.NONE,
                     1024, charset));
        }

        return l.toArray(new Object[][]{});
    }

    @Test(dataProvider = "lines")
    public void test(String description,
                     IntFunction<String> lineGenerator, IntFunction<LineSeparator> separatorGenerator,
                     int lines, Charset cs) throws IOException {
        Path p = generateTempFileWithLines(lineGenerator, separatorGenerator, lines, cs, false);

        Supplier<Stream<String>> ss = () -> {
            try {
                return Files.lines(p, cs);
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        };

        // Test without a separator at the end
        List<String> expected = readAllLines(p, cs);
        withData(TestData.Factory.ofSupplier("Lines with no separator at end", ss))
                .stream(s -> s)
                .expectedResult(expected)
                .exercise();

        // Test with a separator at the end
        writeLineSeparator(p, separatorGenerator, lines, cs);
        expected = readAllLines(p, cs);
        withData(TestData.Factory.ofSupplier("Lines with separator at end", ss))
                .stream(s -> s)
                .expectedResult(expected)
                .exercise();
    }

}
