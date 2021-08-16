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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.CharArrayReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static java.lang.String.format;

/*
 * @test
 * @bug 8029689
 * @summary checks the bounds part of the contract of java.io.Reader.read(char[], int, int):
 *
 *              0 <= off <= off+len <= cbuf.length
 *
 *          for publicly exported subtypes of java.io.Reader
 */
public class ReaderBulkReadContract {

    public static void main(String[] args) throws IOException {
        ReaderBulkReadContract t = new ReaderBulkReadContract();
        t.test();
    }

    private void test() throws IOException {
        Iterator<Object[]> args = args();
        while (args.hasNext()) {
            Object[] a = args.next();
            Reader r = (Reader) a[0];
            int size = (int) a[1];
            int off = (int) a[2];
            int len = (int) a[3];
            try {
                read(r, size, off, len);
            } finally {
                r.close();
            }
        }
    }

    private Iterator<Object[]> args() {

        Integer[] lens = {Integer.MIN_VALUE, -5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5, Integer.MAX_VALUE};
        Integer[] offs = {Integer.MIN_VALUE, -5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5, Integer.MAX_VALUE};
        Integer[] sizes = {0, 1, 2, 3, 4, 5};
        String[] contents = {"", "a", "ab"};

        List<Function<String, Reader>> fs = Arrays.asList(
                (String s) -> new BufferedReader(new StringReader(s)),
                (String s) -> new LineNumberReader(new StringReader(s)),
                (String s) -> new CharArrayReader(s.toCharArray()),
                (String s) -> new InputStreamReader(new ByteArrayInputStream(s.getBytes())),
                (String s) -> newFileReader(s),
                (String s) -> new PushbackReader(new StringReader(s)),
                (String s) -> newPipedReader(s),
                (String s) -> new StringReader(s)
        );

        // The easiest way to produce a cartesian product from a small fixed number of sets
        List<Object[]> tuples = Collections.synchronizedList(new LinkedList<>());
        for (Integer len : lens)
            for (Integer off : offs)
                for (String s : contents)
                    for (Integer size : sizes)
                        for (Function<String, Reader> f : fs)
                            tuples.add(new Object[]{f.apply(s), size, off, len});

        return tuples.iterator();
    }

    private void read(Reader r, int size, int off, int len) throws IOException {
        IndexOutOfBoundsException ex = null;
        try {
            r.read(new char[size], off, len);
        } catch (IndexOutOfBoundsException e) {
            ex = e;
        }

        boolean incorrectBounds = off < 0 || len < 0 || len > size - off;
        boolean exceptionThrown = ex != null;

        if (incorrectBounds != exceptionThrown) { // incorrectBounds iff exceptionThrown
            throw new AssertionError(format("r=%s, size=%s, off=%s, len=%s, incorrectBounds=%s, exceptionThrown=%s",
                    r, size, off, len, incorrectBounds, exceptionThrown));
        }
    }

    private static PipedReader newPipedReader(String contents) {
        try (PipedWriter w = new PipedWriter()) {
            PipedReader r = new PipedReader(w);
            w.write(contents);
            return r;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private FileReader newFileReader(String contents) {
        try {
            // To not create an enormous amount of files
            File f = cache.computeIfAbsent(contents,
                    ReaderBulkReadContract::createTempFileWithContents);
            return new FileReader(f);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static File createTempFileWithContents(String contents) {
        try {
            File testDir = new File(System.getProperty("test.dir", "."));
            File file = File.createTempFile("ReaderContract", "", testDir);
            try (FileWriter w = new FileWriter(file)) {
                w.write(contents);
            }
            return file;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    //
    // To avoid myriads of tiny files a cache is used.
    // ConcurrentHashMap.computeIfAbsent promises a crucial thing:
    //
    // ...The entire method invocation is performed atomically, so the
    // function is applied at most once per key...
    //  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //
    private final ConcurrentHashMap<String, File> cache = new ConcurrentHashMap<>();
}
