/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import sun.security.util.ManifestDigester;

import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.testng.Assert.*;

/**
 * @test
 * @bug 8217375
 * @modules java.base/sun.security.util:+open
 * @compile ../../tools/jarsigner/Utils.java
 * @run testng/othervm FindSection
 * @summary Check {@link ManifestDigester#findSection}.
 */
public class FindSection {

    /*
     * TODO:
     * FIXED_8217375 is not intended to keep. it is intended to show what
     * exactly has changed with respect to the previous version for which no
     * such test existed.
     */
    static final boolean FIXED_8217375 = true;

    /**
     * {@link ManifestDigester.Entry#digestWorkaround} should not feed the
     * trailing blank line into the digester. Before resolution of 8217375 it
     * fed the trailing blank line into the digest if the second line break
     * was at the end of the file due to <pre>
     * if (allBlank || (i == len-1)) {
     *     if (i == len-1)
     *         pos.endOfSection = i;
     *     else
     *         pos.endOfSection = last;
     * </pre> in {@link ManifestDigester#findSection}. In that case at the end
     * of the manifest file, {@link ManifestDigester.Entry#digestWorkaround}
     * would have produced the same digest as
     * {@link ManifestDigester.Entry#digest} which was wrong and without effect
     * at best.
     * <p>
     * Once this fix is accepted, this flag can be removed along with
     * {@link #actualEndOfSection8217375}.
     */
    static final boolean FIXED_8217375_EOF_ENDOFSECTION = FIXED_8217375;

    /**
     * {@link ManifestDigester.Position.endOfSection} usually points to the
     * start position of the blank line trailing a section minus one.
     * If a {@link ManifestDigester.Position} returned by
     * {@link ManifestDigester#findSection} is based on a portion that starts
     * with a blank line, above statement is (or was) not true, because of the
     * initialization of {@code last} in {@link ManifestDigester#findSection}
     * <pre>
     * int last = offset;
     * </pre>
     * which would point after incrementing it in {@code pos.endOfSection + 1}
     * on line 128 (line number before this change) or {@code int sectionLen =
     * pos.endOfSection-start+1;} on line 133 (line number before this change)
     * at one byte after the first line break character of usually two and
     * possibly (assuming "{@code \r\n}" default line break normally) in between
     * the two characters of a line break. After subtracting again the index of
     * the section start position on former line 133, the last byte would be
     * missed to be digested by {@link ManifestDigester.Entry#digestWorkaround}.
     * <p>
     * All this, however could possibly matter (or have mattered) only when
     * {@link ManifestDigester#findSection} was invoked with an offset position
     * pointing straight to a line break which happens if a manifest starts
     * with an empty line or if there are superfluous blank lines between
     * sections in both cases no useful manifest portion is identified.
     * Superfluous blank lines are not identified as sections (because they
     * don't have a name and specifically don't meet {@code if (len > 6) {} on
     * former line 136. Manifests starting with a line break are not any more
     * useful either.
     * <p>
     * Once this fix is accepted, this flag can be removed along with
     * {@link #actualEndOfSection8217375}.
     */
    static final boolean FIXED_8217375_STARTWITHBLANKLINE_ENDOFSECTION =
            FIXED_8217375;

    static Constructor<?> PositionConstructor;
    static Method findSection;
    static Field rawBytes;
    static Field endOfFirstLine;
    static Field endOfSection;
    static Field startOfNext;

    @BeforeClass
    public static void setFindSectionAccessible() throws Exception {
        Class<?> Position = Arrays.stream(ManifestDigester.class.
                getDeclaredClasses()).filter(c -> c.getSimpleName().
                        equals("Position")).findFirst().get();
        PositionConstructor = Position.getDeclaredConstructor();
        PositionConstructor.setAccessible(true);
        findSection = ManifestDigester.class.getDeclaredMethod("findSection",
            int.class, Position);
        findSection.setAccessible(true);
        rawBytes = ManifestDigester.class.getDeclaredField("rawBytes");
        rawBytes.setAccessible(true);
        endOfFirstLine = Position.getDeclaredField("endOfFirstLine");
        endOfFirstLine.setAccessible(true);
        endOfSection = Position.getDeclaredField("endOfSection");
        endOfSection.setAccessible(true);
        startOfNext = Position.getDeclaredField("startOfNext");
        startOfNext.setAccessible(true);
    }

    static class Position {
        final int endOfFirstLine; // not including newline character

        final int endOfSection; // end of section, not including the blank line
                                // between sections
        final int startOfNext;  // the start of the next section

        Position(Object pos) throws ReflectiveOperationException {
            endOfFirstLine = FindSection.endOfFirstLine.getInt(pos);
            endOfSection = FindSection.endOfSection.getInt(pos);
            startOfNext = FindSection.startOfNext.getInt(pos);
        }
    }

    Position findSection(byte[] manifestBytes)
            throws ReflectiveOperationException {
        ManifestDigester manDig = new ManifestDigester("\n\n".getBytes(UTF_8));
        FindSection.rawBytes.set(manDig, manifestBytes);
        Object pos = PositionConstructor.newInstance();
        Object result = findSection.invoke(manDig, offset, pos);
        if (Boolean.FALSE.equals(result)) {
            return null; // indicates findSection having returned false
        } else {
            return new Position(pos);
        }
    }

    @DataProvider(name = "parameters")
    public static Object[][] parameters() {
        return new Object[][] { { 0 }, { 42 } };
    }

    @Factory(dataProvider = "parameters")
    public static Object[] createTests(int offset) {
        return new Object[]{ new FindSection(offset) };
    }

    final int offset;

    FindSection(int offset) {
        this.offset = offset;
    }

    @BeforeMethod
    public void verbose() {
        System.out.println("offset = " + offset);
    }

    Position findSection(String manifestString)
            throws ReflectiveOperationException {
        byte[] manifestBytes = manifestString.getBytes(UTF_8);
        byte[] manifestWithOffset = new byte[manifestBytes.length + offset];
        System.arraycopy(manifestBytes, 0, manifestWithOffset, offset,
                manifestBytes.length);
        return findSection(manifestWithOffset);
    }

    /**
     * Surprising, but the offset actually makes a difference in
     * {@link ManifestDigester#findSection} return value.
     */
    @SuppressWarnings("unused")
    int actualEndOfFirstLine8217375(int correctPosition) {
        // if the parsed portion of the manifest starts with a blank line,
        // and offset is 0, "pos.endOfFirstLine = -1;" probably denoting a
        // yet uninitialized value coincides with the assignment by
        // "pos.endOfFirstLine = i-1;" if i == 0 and
        // "if (pos.endOfFirstLine == -1)" after "case '\n':" happens to
        // become true even though already assigned.
        if (offset == 0 && correctPosition == -1 && !FIXED_8217375) return 0;
        return correctPosition;
    }

    @SuppressWarnings("unused")
    int actualEndOfSection8217375(int correctPosition, boolean eof, int lbl) {
        // if the parsed portion of the manifest ends with a blank line and
        // just before eof, the blank line is included in Position.endOfSection/
        // Section.length (the one usually without blank line as well as in
        // Position.startOfNext/Section.lengthWithBlankLine) which is used
        // in digestWorkaround (independent of the digest without workaround)
        if (eof && !FIXED_8217375_EOF_ENDOFSECTION) {
            return correctPosition + lbl;
        } else if (correctPosition == -1
                && !FIXED_8217375_STARTWITHBLANKLINE_ENDOFSECTION) {
            return 0;
        } else {
            return correctPosition;
        }
    }

    AssertionError collectErrors(AssertionError a, Runnable run) {
        try {
            run.run();
        } catch (AssertionError e) {
            if (a == null) a = new AssertionError();
            a.addSuppressed(e);
        }
        return a;
    }

    void assertPosition(Position pos,
            int endOfFirstLine, int endOfSection, int startOfNext) {
        AssertionError a = null;
        a = collectErrors(a, () -> assertEquals(
                pos.endOfFirstLine, endOfFirstLine + offset, "endOfFirstLine"));
        a = collectErrors(a, () -> assertEquals(
                pos.endOfSection, endOfSection + offset, "endOfSection"));
        a = collectErrors(a, () -> assertEquals(
                pos.startOfNext, startOfNext + offset, "startOfNext"));
        if (a != null) throw a;
    }

    void catchCrCausesIndexOutOfBoundsException(
            Callable<Position> test, Consumer<Position> asserts) {
        try {
            Position x = test.call();
            if (!FIXED_8217375) fail();
            asserts.accept(x);
        } catch (Exception e) {
            if (e instanceof IndexOutOfBoundsException ||
                e.getCause() instanceof IndexOutOfBoundsException) {
                if (FIXED_8217375) throw new AssertionError(e);
            } else {
                throw new AssertionError(e);
            }
        }
    }

    @Test
    public void testEmpty() throws Exception {
        assertNull(findSection(""));
    }

    @Test
    public void testOneLineBreakCr() throws Exception {
        catchCrCausesIndexOutOfBoundsException(
                () -> findSection("\r"),
                p -> assertPosition(p,
                        -1, actualEndOfSection8217375(-1, false, 1), 1)
        );
    }

    @Test
    public void testOneLineBreakLf() throws Exception {
        assertPosition(findSection("\n"),
                -1, actualEndOfSection8217375(-1, false, 1), 1);
    }

    @Test
    public void testOneLineBreakCrLf() throws Exception {
        assertPosition(findSection("\r\n"),
                actualEndOfFirstLine8217375(-1),
                actualEndOfSection8217375(-1, true, 2),
                2);
    }

    @Test
    public void testSpaceAndLineBreakCr() throws Exception {
        catchCrCausesIndexOutOfBoundsException(
                () -> findSection("   \r"),
                p -> assertPosition(p, 2, 3, 4)
        );
    }

    @Test
    public void testSpaceAndOneLineBreakLf() throws Exception {
        assertPosition(findSection("   \n"), 2, 3, 4);
    }

    @Test
    public void testSpaceAndOneLineBreakCrLf() throws Exception {
        assertPosition(findSection("   \r\n"), 2, 4, 5);
    }

    @Test
    public void testOneLineBreakCrAndSpace() throws Exception {
        assertPosition(findSection("\r   "),
                -1, actualEndOfSection8217375(-1, false, 1), 1);
    }

    @Test
    public void testOneLineBreakLfAndSpace() throws Exception {
        assertPosition(findSection("\n   "),
                -1, actualEndOfSection8217375(-1, false, 1), 1);
    }

    @Test
    public void testOneLineBreakCrLfAndSpace() throws Exception {
        assertPosition(findSection("\r\n   "),
                actualEndOfFirstLine8217375(-1),
                actualEndOfSection8217375(-1, false, 1),
                2);
    }

    @Test
    public void testCrEof() throws Exception {
        catchCrCausesIndexOutOfBoundsException(
                () -> findSection("abc\r"),
                p -> assertPosition(p, 2, 3, 4)
        );
    }

    @Test
    public void testLfEof() throws Exception {
        assertPosition(findSection("abc\n"), 2, 3, 4);
    }

    @Test
    public void testCrLfEof() throws Exception {
        assertPosition(findSection("abc\r\n"), 2, 4, 5);
    }

    @Test
    public void testCrContinued() throws Exception {
        assertPosition(findSection("abc\rxyz\r\n\r\n   "), 2, 8, 11);
    }

    @Test
    public void testLfContinued() throws Exception {
        assertPosition(findSection("abc\nxyz\r\n\r\n   "), 2, 8, 11);
    }

    @Test
    public void testCrLfContinued() throws Exception {
        assertPosition(findSection("abc\r\nxyz\r\n\r\n   "), 2, 9, 12);
    }

    @Test
    public void testCrCrEof() throws Exception {
        catchCrCausesIndexOutOfBoundsException(
                () -> findSection("abc\r\nxyz\r\r"),
                p -> assertPosition(p,
                        2, actualEndOfSection8217375(8, true, 1), 10)
        );
    }

    @Test
    public void testCrCrContinued() throws Exception {
        assertPosition(findSection("abc\r\nxyz\r\r   "), 2, 8, 10);
    }

    @Test
    public void testLfLfEof() throws Exception {
        assertPosition(findSection("abc\r\nxyz\n\n"),
                2, actualEndOfSection8217375(8, true, 1), 10);
    }

    @Test
    public void testLfLfContinued() throws Exception {
        assertPosition(findSection("abc\r\nxyz\n\n   "), 2, 8, 10);
    }

    @Test
    public void testCrLfEof2() throws Exception {
        assertPosition(findSection("abc\r\nxyz\r\n"), 2, 9, 10);
    }

    @Test
    public void testMainSectionNotTerminatedWithLineBreak() throws Exception {
        assertNull(findSection("abc\r\nxyz\r\n   "));
    }

    @Test
    public void testLfCrEof() throws Exception {
        catchCrCausesIndexOutOfBoundsException(
                () -> findSection("abc\r\nxyz\n\r"),
                p -> assertPosition(p,
                        2, actualEndOfSection8217375(8, true, 1), 10)
        );
    }

    @Test
    public void testLfCrContinued() throws Exception {
        assertPosition(findSection("abc\r\nxyz\n\r   "), 2, 8, 10);
    }

    @Test
    public void testCrLfCrEof() throws Exception {
        catchCrCausesIndexOutOfBoundsException(
                () -> findSection("abc\r\nxyz\r\n\r"),
                p -> assertPosition(p,
                        2, actualEndOfSection8217375(9, true, 2), 11)
        );
    }

    @Test
    public void testCrLfCrContinued() throws Exception {
        assertPosition(findSection("abc\r\nxyz\r\n\r   "), 2, 9, 11);
    }

    @Test
    public void testCrLfLfEof() throws Exception {
        assertPosition(findSection("abc\r\nxyz\r\n\n"),
                2, actualEndOfSection8217375(9, true, 1), 11);
    }

    @Test
    public void testCrLfLfContinued() throws Exception {
        assertPosition(findSection("abc\r\nxyz\r\n\n   "), 2, 9, 11);
    }

    @Test
    public void testCrLfCrLfEof() throws Exception {
        assertPosition(findSection("abc\r\nxyz\r\n\r\n"),
                2, actualEndOfSection8217375(9, true, 2), 12);
    }

    @Test
    public void testCrLfCfLfContinued() throws Exception {
        assertPosition(findSection("abc\r\nxyz\r\n\r\n   "), 2, 9, 12);
    }

    @Test
    public void testCrLfCrCrEof() throws Exception {
        assertPosition(findSection("abc\r\nxyz\r\n\r\r"), 2, 9, 11);
    }

    @Test
    public void testCrLfCrCrContinued() throws Exception {
        assertPosition(findSection("abc\r\nxyz\r\n\r\r   "), 2, 9, 11);
    }

    @Test
    public void testCrLfLfCrEof() throws Exception {
        assertPosition(findSection("abc\r\nxyz\r\n\n\r"), 2, 9, 11);
    }

    @Test
    public void testCrLfLfCrContinued() throws Exception {
        assertPosition(findSection("abc\r\nxyz\r\n\n\r   "), 2, 9, 11);
    }

    @Test
    public void testCrLfCrLfCrEof() throws Exception {
        assertPosition(findSection("abc\r\nxyz\r\n\r\n\r"), 2, 9, 12);
    }

    @Test
    public void testCrLfCfLfCrContinued() throws Exception {
        assertPosition(findSection("abc\r\nxyz\r\n\r\n\r   "), 2, 9, 12);
    }

    @Test
    public void testCrLfCrLfContinued() throws Exception {
        assertPosition(findSection("abc\r\nxyz\r\n\r\n   "), 2, 9, 12);
    }

    @Test
    public void testCrLfLfLfEof() throws Exception {
        assertPosition(findSection("abc\r\nxyz\r\n\n\n"), 2, 9, 11);
    }

    @Test
    public void testCrLfLfLfContinued() throws Exception {
        assertPosition(findSection("abc\r\nxyz\r\n\n\n   "), 2, 9, 11);
    }

    @Test
    public void testCrLfCrLfLfContinued() throws Exception {
        assertPosition(findSection("abc\r\nxyz\r\n\r\n\n   "), 2, 9, 12);
    }

    @Test
    public void testCrLfCrCrLfEof() throws Exception {
        assertPosition(findSection("abc\r\nxyz\r\n\r\r\n"), 2, 9, 11);
    }

    @Test
    public void testCrLfCrCrLfContinued() throws Exception {
        assertPosition(findSection("abc\r\nxyz\r\n\r\r\n   "), 2, 9, 11);
    }

    @Test
    public void testCrLfLfCrLfEof() throws Exception {
        assertPosition(findSection("abc\r\nxyz\r\n\n\r\n"), 2, 9, 11);
    }

    @Test
    public void testCrLfLfCrLfContinued() throws Exception {
        assertPosition(findSection("abc\r\nxyz\r\n\n\r\n   "), 2, 9, 11);
    }

    @Test
    public void testCrLfCrLfCrLfEof() throws Exception {
        assertPosition(findSection("abc\r\nxyz\r\n\r\n\r\n"), 2, 9, 12);
    }

    @Test
    public void testCrLfCfLfCrLfContinued() throws Exception {
        assertPosition(findSection("abc\r\nxyz\r\n\r\n\r\n   "), 2, 9, 12);
    }

    @Test
    public void testCrLfLfCrCrEof() throws Exception {
        assertPosition(findSection("abc\r\nxyz\r\n\n\r\r"), 2, 9, 11);
    }

    @Test
    public void testCrLfCrLfCrCrEof() throws Exception {
        assertPosition(findSection("abc\r\nxyz\r\n\r\n\r\r"), 2, 9, 12);
    }

    @Test
    public void testCrLfCrLfCrContinued() throws Exception {
        assertPosition(findSection("abc\r\nxyz\r\n\r\n\r   "), 2, 9, 12);
    }

    @Test
    public void testCrLfLfLfCrEof() throws Exception {
        assertPosition(findSection("abc\r\nxyz\r\n\n\n\r"), 2, 9, 11);
    }

    @Test
    public void testCrLfLfCrLfCrEof() throws Exception {
        assertPosition(findSection("abc\r\nxyz\r\n\n\r\n\r"), 2, 9, 11);
    }

    @Test
    public void testCrLfLfLfLfEof() throws Exception {
        assertPosition(findSection("abc\r\nxyz\r\n\n\n\n"), 2, 9, 11);
    }

    @Test
    public void testCrLfLfCrLfLfEof() throws Exception {
        assertPosition(findSection("abc\r\nxyz\r\n\n\r\n\n"), 2, 9, 11);
    }

    @Test
    public void testCrLfLfCrCrLfEof() throws Exception {
        assertPosition(findSection("abc\r\nxyz\r\n\n\r\r\n"), 2, 9, 11);
    }

    @Test
    public void testCrLfCrLfCrCrLfEof() throws Exception {
        assertPosition(findSection("abc\r\nxyz\r\n\r\n\r\r\n"), 2, 9, 12);
    }

    @Test
    public void testCrLfCrLfCrLfContinued() throws Exception {
        assertPosition(findSection("abc\r\nxyz\r\n\r\n\r\n   "), 2, 9, 12);
    }

    @Test
    public void testCrLfLfLfCrLfEof() throws Exception {
        assertPosition(findSection("abc\r\nxyz\r\n\n\n\r\n"), 2, 9, 11);
    }

    @Test
    public void testCrLfLfCrLfCrLfEof() throws Exception {
        assertPosition(findSection("abc\r\nxyz\r\n\n\r\n\r\n"), 2, 9, 11);
    }

    @Test
    public void testCrLfCrCrLfCrCrEof() throws Exception {
        assertPosition(findSection("abc\r\nxyz\r\n\r\r\n\r"), 2, 9, 11);
    }

    @Test
    public void testCrLfCrCrCrCrEof() throws Exception {
        assertPosition(findSection("abc\r\nxyz\r\n\r\r\r"), 2, 9, 11);
    }

    @Test
    public void testCrLfCrCrLfLfEof() throws Exception {
        assertPosition(findSection("abc\r\nxyz\r\n\r\r\n\n"), 2, 9, 11);
    }

    @Test
    public void testCrLfCrCrLfCrLfEof() throws Exception {
        assertPosition(findSection("abc\r\nxyz\r\n\r\r\n\r\n"), 2, 9, 11);
    }

    @Test
    public void testCrLfCrCrCrLfEof() throws Exception {
        assertPosition(findSection("abc\r\nxyz\r\n\r\r\r\n"), 2, 9, 11);
    }

    /*
     * endOfFirstLine is the same regardless of the line break delimiter
     */
    @Test
    public void testEndOfFirstLineVsLineBreak() throws Exception {
        for (String lb : new String[] { "\r", "\n", "\r\n" }) {
            Position p = findSection("abc" + lb + "xyz" + lb + lb + " ");

            // main assertion showing endOfFirstLine independent of line break
            assertEquals(p.endOfFirstLine, 2 + offset);

            // assert remaining positions as well just for completeness
            assertPosition(p, 2, 5 + 2 * lb.length(), 6 + 3 * lb.length());
        }
    }

    /*
     * '\r' at the end of the bytes causes index out of bounds exception
     */
    @Test
    public void testCrLastCausesIndexOutOfBounds() throws Exception {
        catchCrCausesIndexOutOfBoundsException(
                () -> findSection("\r"),
                p -> assertPosition(p,
                        -1, actualEndOfSection8217375(-1, true, 1), 1)
        );
    }

    /*
     * endOfSection includes second line break if at end of bytes only
     */
    @Test
    public void testEndOfSectionWithLineBreakVsEof() throws Exception {
        AssertionError errors = new AssertionError("offset = " + offset);
        for (String lb : new String[] { "\r", "\n", "\r\n" }) {
            for (boolean eof : new boolean[] { false, true }) {
                Position p;
                try {
                    p = findSection("abc" + lb + lb + (eof ? "" : "xyz"));
                } catch (RuntimeException | ReflectiveOperationException e) {
                    if ((e instanceof IndexOutOfBoundsException ||
                         e.getCause() instanceof IndexOutOfBoundsException)
                          && eof && "\r".equals(lb) && !FIXED_8217375) continue;
                    throw e;
                }

                AssertionError a = new AssertionError("offset = " + offset
                        + ", lb = " + Utils.escapeStringWithNumbers(lb) + ", "
                        + "eof = " + eof);

                // main assertion showing endOfSection including second line
                // break when at end of file
                a = collectErrors(a, () -> assertEquals(
                        p.endOfSection,
                        actualEndOfSection8217375(
                                2 + lb.length() + offset, eof, lb.length()) ));

                // assert remaining positions as well just for completeness
                a = collectErrors(a, () -> assertPosition(p,
                        2,
                        actualEndOfSection8217375(
                                2 + lb.length(), eof, lb.length()),
                        3 + lb.length() * 2));

                if (a.getSuppressed().length > 0) errors.addSuppressed(a);
            }
        }
        if (errors.getSuppressed().length > 0) throw errors;
    }

    /*
     * returns position even if only one line break before end of bytes.
     * because no name will be found the result will be skipped and no entry
     * will be created.
     */
    @Test
    public void testReturnPosVsEof() throws Exception {
        for (String lb : new String[] { "\r", "\n", "\r\n" }) {
            for (boolean eof : new boolean[] { false, true }) {
                try {
                    Position p = findSection("abc" + lb + (eof ? "" : "xyz"));
                    assertTrue(p != null == eof);
                } catch (RuntimeException | ReflectiveOperationException e) {
                    if ((e instanceof IndexOutOfBoundsException ||
                         e.getCause() instanceof IndexOutOfBoundsException)
                          && eof && "\r".equals(lb) && !FIXED_8217375) continue;
                    throw e;
                }
            }
        }
    }

    /*
     * it could be normally be expected that startOfNext would point to the
     * start of the next section after a blank line but that is not the case
     * if a section ends with only one line break and no blank line immediately
     * before eof of the manifest.
     * such an entry will be digested without the trailing blank line which is
     * only fine until another section should be added afterwards.
     */
    @Test
    public void testStartOfNextPointsToEofWithNoBlankLine() throws Exception {
        for (String lb : new String[] { "\r", "\n", "\r\n" }) {
            for (boolean blank : new boolean[] { false, true }) {
                String manifest = "abc" + lb + "xyz" + lb + (blank ? lb : "");
                try {
                    Position p = findSection(manifest);

                    // assert that startOfNext points to eof in all cases
                    // whether with or without a blank line before eof
                    assertEquals(p.startOfNext, manifest.length() + offset);

                    // assert remaining positions as well just for completeness
                    assertPosition(p,
                            2,
                            actualEndOfSection8217375(
                                    5 + lb.length() * 2,
                                    true,
                                    blank ? lb.length() : 0),
                            manifest.length());
                } catch (RuntimeException | ReflectiveOperationException e) {
                    if ((e instanceof IndexOutOfBoundsException ||
                         e.getCause() instanceof IndexOutOfBoundsException)
                          && "\r".equals(lb) && !FIXED_8217375) continue;
                    throw e;
                }
            }
        }
    }

}
