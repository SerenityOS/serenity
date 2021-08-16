/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8003258 8029434
 * @run testng Lines
 */

import java.io.BufferedReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.LineNumberReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.stream.Stream;
import java.util.concurrent.atomic.AtomicInteger;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

@Test(groups = "unit")
public class Lines {
    private static final Map<String, Integer> cases = new HashMap<>();

    static {
        cases.put("", 0);
        cases.put("Line 1", 1);
        cases.put("Line 1\n", 1);
        cases.put("Line 1\n\n\n", 3);
        cases.put("Line 1\nLine 2\nLine 3", 3);
        cases.put("Line 1\nLine 2\nLine 3\n", 3);
        cases.put("Line 1\n\nLine 3\n\nLine5", 5);
    }

    /**
     * Helper Reader class which generate specified number of lines contents
     * with each line will be "<code>Line &lt;line_number&gt;</code>".
     *
     * <p>This class also support to simulate {@link IOException} when read pass
     * a specified line number.
     */
    private static class MockLineReader extends Reader {
        final int line_count;
        boolean closed = false;
        int line_no = 0;
        String line = null;
        int pos = 0;
        int inject_ioe_after_line;

        MockLineReader(int cnt) {
            this(cnt, cnt);
        }

        MockLineReader(int cnt, int inject_ioe) {
            line_count = cnt;
            inject_ioe_after_line = inject_ioe;
        }

        public void reset() {
            synchronized(lock) {
                line = null;
                line_no = 0;
                pos = 0;
                closed = false;
            }
        }

        public void inject_ioe() {
            inject_ioe_after_line = line_no;
        }

        public int getLineNumber() {
            synchronized(lock) {
                return line_no;
            }
        }

        @Override
        public void close() {
            closed = true;
        }

        @Override
        public int read(char[] buf, int off, int len) throws IOException {
            synchronized(lock) {
                if (closed) {
                    throw new IOException("Stream is closed.");
                }

                if (line == null) {
                    if (line_count > line_no) {
                        line_no += 1;
                        if (line_no > inject_ioe_after_line) {
                            throw new IOException("Failed to read line " + line_no);
                        }
                        line = "Line " + line_no + "\n";
                        pos = 0;
                    } else {
                        return -1; // EOS reached
                    }
                }

                int cnt = line.length() - pos;
                assert(cnt != 0);
                // try to fill with remaining
                if (cnt >= len) {
                    line.getChars(pos, pos + len, buf, off);
                    pos += len;
                    if (cnt == len) {
                        assert(pos == line.length());
                        line = null;
                    }
                    return len;
                } else {
                    line.getChars(pos, pos + cnt, buf, off);
                    off += cnt;
                    len -= cnt;
                    line = null;
                    /* hold for next read, so we won't IOE during fill buffer
                    int more = read(buf, off, len);
                    return (more == -1) ? cnt : cnt + more;
                    */
                    return cnt;
                }
            }
        }
    }

    private static void verify(Map.Entry<String, Integer> e) {
        final String data = e.getKey();
        final int total_lines = e.getValue();
        try (BufferedReader br = new BufferedReader(
                                    new StringReader(data))) {
            assertEquals(br.lines()
                           .mapToInt(l -> 1).reduce(0, (x, y) -> x + y),
                         total_lines,
                         data + " should produce " + total_lines + " lines.");
        } catch (IOException ioe) {
            fail("Should not have any exception.");
        }
    }

    public void testLinesBasic() {
        // Basic test cases
        cases.entrySet().stream().forEach(Lines::verify);
        // Similar test, also verify MockLineReader is correct
        for (int i = 0; i < 10; i++) {
            try (BufferedReader br = new BufferedReader(new MockLineReader(i))) {
                assertEquals(br.lines()
                               .peek(l -> assertTrue(l.matches("^Line \\d+$")))
                               .mapToInt(l -> 1).reduce(0, (x, y) -> x + y),
                             i,
                             "MockLineReader(" + i + ") should produce " + i + " lines.");
            } catch (IOException ioe) {
                fail("Unexpected IOException.");
            }
        }
    }

    public void testUncheckedIOException() throws IOException {
        MockLineReader r = new MockLineReader(10, 3);
        ArrayList<String> ar = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(r)) {
            br.lines().limit(3L).forEach(ar::add);
            assertEquals(ar.size(), 3, "Should be able to read 3 lines.");
        } catch (UncheckedIOException uioe) {
            fail("Unexpected UncheckedIOException");
        }
        r.reset();
        try (BufferedReader br = new BufferedReader(r)) {
            br.lines().forEach(ar::add);
            fail("Should had thrown UncheckedIOException.");
        } catch (UncheckedIOException uioe) {
            assertEquals(r.getLineNumber(), 4, "should fail to read 4th line");
            assertEquals(ar.size(), 6, "3 + 3 lines read");
        }
        for (int i = 0; i < ar.size(); i++) {
            assertEquals(ar.get(i), "Line " + (i % 3 + 1));
        }
    }

    public void testIterator() throws IOException {
        MockLineReader r = new MockLineReader(6);
        BufferedReader br = new BufferedReader(r);
        String line = br.readLine();
        assertEquals(r.getLineNumber(), 1, "Read one line");
        Stream<String> s = br.lines();
        Iterator<String> it = s.iterator();
        // Ensure iterate with only next works
        for (int i = 0; i < 5; i++) {
            String str = it.next();
            assertEquals(str, "Line " + (i + 2), "Addtional five lines");
        }
        // NoSuchElementException
        try {
            it.next();
            fail("Should have run out of lines.");
        } catch (NoSuchElementException nsse) {}
    }

    public void testPartialReadAndLineNo() throws IOException {
        MockLineReader r = new MockLineReader(5);
        LineNumberReader lr = new LineNumberReader(r);
        char[] buf = new char[5];
        lr.read(buf, 0, 5);
        assertEquals(0, lr.getLineNumber(), "LineNumberReader start with line 0");
        assertEquals(1, r.getLineNumber(), "MockLineReader start with line 1");
        assertEquals(new String(buf), "Line ");
        String l1 = lr.readLine();
        assertEquals(l1, "1", "Remaining of the first line");
        assertEquals(1, lr.getLineNumber(), "Line 1 is read");
        assertEquals(1, r.getLineNumber(), "MockLineReader not yet go next line");
        lr.read(buf, 0, 4);
        assertEquals(1, lr.getLineNumber(), "In the middle of line 2");
        assertEquals(new String(buf, 0, 4), "Line");
        ArrayList<String> ar = lr.lines()
             .peek(l -> assertEquals(lr.getLineNumber(), r.getLineNumber()))
             .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        assertEquals(ar.get(0), " 2", "Remaining in the second line");
        for (int i = 1; i < ar.size(); i++) {
            assertEquals(ar.get(i), "Line " + (i + 2), "Rest are full lines");
        }
    }

    public void testInterlacedRead() throws IOException {
        MockLineReader r = new MockLineReader(10);
        BufferedReader br = new BufferedReader(r);
        char[] buf = new char[5];
        Stream<String> s = br.lines();
        Iterator<String> it = s.iterator();

        br.read(buf);
        assertEquals(new String(buf), "Line ");
        assertEquals(it.next(), "1");
        try {
            s.iterator().next();
            fail("Should failed on second attempt to get iterator from s");
        } catch (IllegalStateException ise) {}
        br.read(buf, 0, 2);
        assertEquals(new String(buf, 0, 2), "Li");
        // Get stream again should continue from where left
        // Only read remaining of the line
        br.lines().limit(1L).forEach(line -> assertEquals(line, "ne 2"));
        br.read(buf, 0, 2);
        assertEquals(new String(buf, 0, 2), "Li");
        br.read(buf, 0, 2);
        assertEquals(new String(buf, 0, 2), "ne");
        assertEquals(it.next(), " 3");
        // Line 4
        br.readLine();
        // interator pick
        assertEquals(it.next(), "Line 5");
        // Another stream instantiated by lines()
        AtomicInteger line_no = new AtomicInteger(6);
        br.lines().forEach(l -> assertEquals(l, "Line " + line_no.getAndIncrement()));
        // Read after EOL
        assertFalse(it.hasNext());
    }

    public void testCharacteristics() {
        try (BufferedReader br = new BufferedReader(
                                    new StringReader(""))) {
            Spliterator<String> instance = br.lines().spliterator();
            assertTrue(instance.hasCharacteristics(Spliterator.NONNULL));
            assertTrue(instance.hasCharacteristics(Spliterator.ORDERED));
        } catch (IOException ioe) {
            fail("Should not have any exception.");
        }
    }
}
