/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2016 SAP SE. All rights reserved.
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
 * @bug 8145336
 * @summary PPC64: fix string intrinsics after CompactStrings change
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 *
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 *
 * @run main/othervm
 *        -Xbootclasspath/a:.
 *        -Xmixed
 *        -XX:+UnlockDiagnosticVMOptions
 *        -XX:+WhiteBoxAPI
 *        -XX:+IgnoreUnrecognizedVMOptions
 *        -XX:MaxInlineSize=70
 *        -XX:MinInliningThreshold=0
 *        compiler.intrinsics.string.TestStringIntrinsics2
 */

package compiler.intrinsics.string;

import sun.hotspot.WhiteBox;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.function.Consumer;

import static jdk.test.lib.Asserts.assertEquals;
import static jdk.test.lib.Asserts.assertFalse;
import static jdk.test.lib.Asserts.assertTrue;

public class TestStringIntrinsics2 {
    // ------------------------------------------------------------------------
    //
    // We test the following cases:
    // - no match in string.  Do we miss the end condition? Will crash if we read
    //   past the string.
    // - no match in string, but after the string there is a match.
    //   Do we incorrectly report this match?  We had a case where we stepped
    //   a few chars past the string, this test would report that error. The
    //   one above would not.
    // - The needle is exactly at the end of the string.
    // - The needle spans the end of the string
    //
    // A special case are needles of length 1. For these we test:
    // - needle is first char
    // - needle is last char
    // - no match
    // - match behind string.
    //
    // We test all these for an unknown needle, and needles known to the compiler
    // of lengths 5, 2 and 1.


    private static final WhiteBox WB = WhiteBox.getWhiteBox();

    public enum Role {
        TEST_ENTRY,
        TEST_HELPER
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface Test {
        Role role();
        int compileAt() default 0;
        int warmup() default 0;
        String[] warmupArgs() default {};
    }

    // All this mess is needed to avoid try/catch inside the lambdas below.
    // See: http://stackoverflow.com/questions/27644361/how-can-i-throw-checked-exceptions-from-inside-java-8-streams
    @SuppressWarnings ("unchecked")
    private static <E extends Throwable> void throwAsUnchecked(Exception exception) throws E {
        throw (E)exception;
    }
    @FunctionalInterface
    public interface Consumer_WithExceptions<T, E extends Exception> {
        void accept(T t) throws E;
    }
    public static <T, E extends Exception> Consumer<T> rethrowConsumer(Consumer_WithExceptions<T, E> consumer) {
        return t -> {
            try { consumer.accept(t); }
            catch (Exception exception) { throwAsUnchecked(exception); }
        };
    }

    public static void main(String[] args) throws Exception {

        // Warmup helper methods
        Arrays.stream(TestStringIntrinsics2.class.getDeclaredMethods())
            .filter(m -> m.isAnnotationPresent(Test.class))
            .filter(m -> m.getAnnotation(Test.class).warmup() > 0)
            .forEach(rethrowConsumer(m -> {
                        Test a = m.getAnnotation(Test.class);
                        System.out.println("Warming up " + m + " " + a.warmup() + " time(s) ");
                        for (int i=0; i < a.warmup(); i++) {
                            m.invoke(null, (Object[])a.warmupArgs());
                        }
                    }));

        // Compile helper methods
        Arrays.stream(TestStringIntrinsics2.class.getDeclaredMethods())
            .filter(m -> m.isAnnotationPresent(Test.class))
            .filter(m -> m.getAnnotation(Test.class).compileAt() > 0)
            .forEach(rethrowConsumer(m -> {
                        Test a = m.getAnnotation(Test.class);
                        if (WB.isMethodCompilable(m, a.compileAt())) {
                            WB.enqueueMethodForCompilation(m, a.compileAt());
                            while (WB.isMethodQueuedForCompilation(m)) Thread.sleep(10);
                            System.out.println(m + " compiled at " + WB.getMethodCompilationLevel(m));
                        } else {
                            System.out.println("Can't compile " + m + " at level " + a.compileAt());
                        }
                    }));

        // Run test methods
        Arrays.stream(TestStringIntrinsics2.class.getDeclaredMethods())
            .filter(m -> m.isAnnotationPresent(Test.class))
            .filter(m -> m.getAnnotation(Test.class).role() == Role.TEST_ENTRY)
            .forEach(rethrowConsumer(m -> {
                        System.out.print("Executing " + m);
                        m.invoke(null, (Object[])null);
                        System.out.println(" - OK");
                    }));
    }

    static String text = "<t><t><t><t><t><t>\n" + "<hit>";
    static String text2 = "<t><t><t><t><t><t><t>\n" + "<hit>";
    static String[] ss = text.split("\n");
    static String[] ss2 = null;
    static String needle = "<miss>";

    @Test(role = Role.TEST_ENTRY)
    public static void test_indexOf_no_match() {
        int res = indexOf_no_match_unknown_needle(ss[0], "<miss>");
        assertEquals(res, -1, "test_indexOf_no_match_unknown_needle matched at: " + res);
        res = indexOf_no_match_imm_needle(ss[0]);
        assertEquals(res, -1, "test_indexOf_no_match_imm_needle matched at: " + res);
        res = indexOf_no_match_imm2_needle(ss[0]);
        assertEquals(res, -1, "test_indexOf_no_match_imm2_needle matched at: " + res);

        if (ss2 == null) ss2 = text.split("\n");
        res = indexOf_no_match_unknown_needle(ss2[0], "<miss>");
        assertEquals(res, -1, "test_indexOf_no_match_unknown_needle matched at: " + res);
        res = indexOf_no_match_imm_needle(ss2[0]);
        assertEquals(res, -1, "test_indexOf_no_match_imm_needle matched at: " + res);
        res = indexOf_no_match_imm2_needle(ss2[0]);
        assertEquals(res, -1, "test_indexOf_no_match_imm2_needle matched at: " + res);
        res = indexOf_no_match_imm1_needle(ss2[0]);
        assertEquals(res, -1, "test_indexOf_no_match_imm1_needle matched at: " + res);
    }

    @Test(role = Role.TEST_HELPER, compileAt = 4, warmup = 1, warmupArgs = { "<t><t><t>", "<miss>" })
    static int indexOf_no_match_unknown_needle(String s, String needle) {
        int index = s.indexOf(needle);
        return index;
    }

    @Test(role = Role.TEST_HELPER, compileAt = 4, warmup = 1, warmupArgs = { "<t><t><t>" })
    static int indexOf_no_match_imm_needle(String s) {
        int index = s.indexOf("<hitt>");
        return index;
    }

    @Test(role = Role.TEST_HELPER, compileAt = 4, warmup = 1, warmupArgs = { "<t><t><t>" })
    static int indexOf_no_match_imm2_needle(String s) {
        int index = s.indexOf("<m");
        return index;
    }

    @Test(role = Role.TEST_HELPER, compileAt = 4, warmup = 1, warmupArgs = { "<t><t><t>" })
    static int indexOf_no_match_imm1_needle(String s) {
        int index = s.indexOf("m");
        return index;
    }

    @Test(role = Role.TEST_ENTRY)
    public static void test_indexOf_reads_past_string() {
        if (ss == null) ss = text.split("\n");
        String res = indexOf_reads_past_string_unknown_needle(ss[0], "<hit>");
        assertEquals(res, null, "test_indexOf_reads_past_string_unknown_needle " + res);
        res = indexOf_reads_past_string_imm_needle(ss[0]);
        assertEquals(res, null, "test_indexOf_reads_past_string_imm_needle " + res);
        res = indexOf_reads_past_string_imm2_needle(ss[0]);
        assertEquals(res, null, "test_indexOf_reads_past_string_imm2_needle " + res);
        res = indexOf_reads_past_string_imm1_needle(ss[0]);
        assertEquals(res, null, "test_indexOf_reads_past_string_imm1_needle " + res);
    }

    @Test(role = Role.TEST_HELPER, compileAt = 4, warmup = 1, warmupArgs = { "<t><t><t>", "<hit>" })
    static String indexOf_reads_past_string_unknown_needle(String s, String needle) {
        int index = s.indexOf(needle);
        if (index > s.length()) {
            return "Found needle \"" + needle + "\" behind string of length " + s.length()
                + " at position " + index + ".";
        }
        return null;
    }

    @Test(role = Role.TEST_HELPER, compileAt = 4, warmup = 1, warmupArgs = { "<t><t><t>" })
    static String indexOf_reads_past_string_imm_needle(String s) {
        int index = s.indexOf("<hit>");
        if (index > s.length()) {
            return "Found needle \"<hit>\" behind string of length " + s.length() + " at position " + index + ".";
        }
        return null;
    }

    @Test(role = Role.TEST_HELPER, compileAt = 4, warmup = 1, warmupArgs = { "<t><t><t>" })
    static String indexOf_reads_past_string_imm2_needle(String s) {
        int index = s.indexOf("<h");
        if (index > s.length()) {
            return "Found needle \"<h\" behind string of length " + s.length() + " at position " + index + ".";
        }
        return null;
    }

    @Test(role = Role.TEST_HELPER, compileAt = 4, warmup = 1, warmupArgs = { "<t><t><t>" })
    static String indexOf_reads_past_string_imm1_needle(String s) {
        int index = s.indexOf("h");
        if (index > s.length()) {
            return "Found needle \"<h\" behind string of length " + s.length() + " at position " + index + ".";
        }
        return null;
    }

    static String text3 =    "<t><hi><t><h><hit<t><hit>";
    static String text4 =   "a<t><hi><t><h><hit<t><hit>";
    static String text5 =  "gg<t><hi><t><h><hit<t><hit>";
    static String text6 = "ccc<t><hi><t><h><hit<t><hit>";
    static int len3 = text3.length();
    static int len4 = text4.length();
    static int len5 = text5.length();
    static int len6 = text6.length();

    static String text7  =    "<t><t><t><t><t<t><h";
    static String text8  =   "a<t><t><t><t><t<t><h";
    static String text9  =  "gg<t><t><t><t><t<t><h";
    static String text10 = "ccc<t><t><t><t><t<t><h";

    @Test(role = Role.TEST_ENTRY)
    public static void test_indexOf_match_at_end_of_string() {
        String testname = "test_indexOf_match_at_end_of_string";
        int res = 0;
        res = indexOf_match_at_end_of_string_unknown_needle(text3, "<hit>");
        assertEquals(len3, res + 5, testname);
        res = indexOf_match_at_end_of_string_unknown_needle(text4, "<hit>");
        assertEquals(len4, res + 5, testname);
        res = indexOf_match_at_end_of_string_unknown_needle(text5, "<hit>");
        assertEquals(len5, res + 5, testname);
        res = indexOf_match_at_end_of_string_unknown_needle(text6, "<hit>");
        assertEquals(len6, res + 5, testname);

        res = indexOf_match_at_end_of_string_imm_needle(text3);
        assertEquals(len3, res + 5, testname);
        res = indexOf_match_at_end_of_string_imm_needle(text4);
        assertEquals(len4, res + 5, testname);
        res = indexOf_match_at_end_of_string_imm_needle(text5);
        assertEquals(len5, res + 5, testname);
        res = indexOf_match_at_end_of_string_imm_needle(text6);
        assertEquals(len6, res + 5, testname);

        res = indexOf_match_at_end_of_string_imm2_needle(text7);
        assertEquals(text7.length(),  res + 2, testname);
        res = indexOf_match_at_end_of_string_imm2_needle(text8);
        assertEquals(text8.length(),  res + 2, testname);
        res = indexOf_match_at_end_of_string_imm2_needle(text9);
        assertEquals(text9.length(),  res + 2, testname);
        res = indexOf_match_at_end_of_string_imm2_needle(text10);
        assertEquals(text10.length(), res + 2, testname);

        res = indexOf_match_at_end_of_string_imm1_needle(text7);
        assertEquals(text7.length(),  res + 1, testname);
        res = indexOf_match_at_end_of_string_imm1_needle(text8);
        assertEquals(text8.length(),  res + 1, testname);
        res = indexOf_match_at_end_of_string_imm1_needle(text9);
        assertEquals(text9.length(),  res + 1, testname);
        res = indexOf_match_at_end_of_string_imm1_needle(text10);
        assertEquals(text10.length(), res + 1, testname);
    }

    @Test(role = Role.TEST_HELPER, compileAt = 4, warmup = 1, warmupArgs = { "<t><hi><t><h><hit<t><hit>", "<hit>" })
    static int indexOf_match_at_end_of_string_unknown_needle(String s, String needle) {
        int index = s.indexOf(needle);
        return index;
    }

    @Test(role = Role.TEST_HELPER, compileAt = 4, warmup = 1, warmupArgs = { "<t><hi><t><h><hit<t><hit>" })
    static int indexOf_match_at_end_of_string_imm_needle(String s) {
        int index = s.indexOf("<hit>");
        return index;
    }

    @Test(role = Role.TEST_HELPER, compileAt = 4, warmup = 1, warmupArgs = { "<t><hi><t><h><hit<t><hit>" })
    static int indexOf_match_at_end_of_string_imm2_needle(String s) {
        int index = s.indexOf("<h");
        return index;
    }

    @Test(role = Role.TEST_HELPER, compileAt = 4, warmup = 1, warmupArgs = { "<t><hi><t><h><hit<t><hit>" })
    static int indexOf_match_at_end_of_string_imm1_needle(String s) {
        int index = s.indexOf("h");
        return index;
    }

    static String s0_1 = text3.substring(0, len3-1);
    static String s0_2 = text3.substring(0, len3-2);
    static String s0_3 = text3.substring(0, len3-3);
    static String s0_4 = text3.substring(0, len3-4);
    static String s1_1 = text4.substring(0, len4-1);
    static String s1_2 = text4.substring(0, len4-2);
    static String s1_3 = text4.substring(0, len4-3);
    static String s1_4 = text4.substring(0, len4-4);
    static String s2_1 = text5.substring(0, len5-1);
    static String s2_2 = text5.substring(0, len5-2);
    static String s2_3 = text5.substring(0, len5-3);
    static String s2_4 = text5.substring(0, len5-4);
    static String s3_1 = text6.substring(0, len6-1);
    static String s3_2 = text6.substring(0, len6-2);
    static String s3_3 = text6.substring(0, len6-3);
    static String s3_4 = text6.substring(0, len6-4);

    static String s0_1x = text7 .substring(0, text7 .length()-1);
    static String s1_1x = text8 .substring(0, text8 .length()-1);
    static String s2_1x = text9 .substring(0, text9 .length()-1);
    static String s3_1x = text10.substring(0, text10.length()-1);

    @Test(role = Role.TEST_ENTRY)
    public static void test_indexOf_match_spans_end_of_string() {
        String res = null;
        res = indexOf_match_spans_end_of_string_unknown_needle(s0_1, "<hit>");
        assertEquals(res, null, "test_indexOf_match_spans_end_of_string_unknown_needle s0_1 " + res);
        res = indexOf_match_spans_end_of_string_unknown_needle(s0_2, "<hit>");
        assertEquals(res, null, "test_indexOf_match_spans_end_of_string_unknown_needle s0_2 " + res);
        res = indexOf_match_spans_end_of_string_unknown_needle(s0_3, "<hit>");
        assertEquals(res, null, "test_indexOf_match_spans_end_of_string_unknown_needle s0_3 " + res);
        res = indexOf_match_spans_end_of_string_unknown_needle(s0_4, "<hit>");
        assertEquals(res, null, "test_indexOf_match_spans_end_of_string_unknown_needle s0_4 " + res);
        res = indexOf_match_spans_end_of_string_unknown_needle(s1_1, "<hit>");
        assertEquals(res, null, "test_indexOf_match_spans_end_of_string_unknown_needle s1_1 " + res);
        res = indexOf_match_spans_end_of_string_unknown_needle(s1_2, "<hit>");
        assertEquals(res, null, "test_indexOf_match_spans_end_of_string_unknown_needle s1_2 " + res);
        res = indexOf_match_spans_end_of_string_unknown_needle(s1_3, "<hit>");
        assertEquals(res, null, "test_indexOf_match_spans_end_of_string_unknown_needle s1_3 " + res);
        res = indexOf_match_spans_end_of_string_unknown_needle(s1_4, "<hit>");
        assertEquals(res, null, "test_indexOf_match_spans_end_of_string_unknown_needle s1_4 " + res);
        res = indexOf_match_spans_end_of_string_unknown_needle(s2_1, "<hit>");
        assertEquals(res, null, "test_indexOf_match_spans_end_of_string_unknown_needle s2_1 " + res);
        res = indexOf_match_spans_end_of_string_unknown_needle(s2_2, "<hit>");
        assertEquals(res, null, "test_indexOf_match_spans_end_of_string_unknown_needle s2_2 " + res);
        res = indexOf_match_spans_end_of_string_unknown_needle(s2_3, "<hit>");
        assertEquals(res, null, "test_indexOf_match_spans_end_of_string_unknown_needle s2_3 " + res);
        res = indexOf_match_spans_end_of_string_unknown_needle(s2_4, "<hit>");
        assertEquals(res, null, "test_indexOf_match_spans_end_of_string_unknown_needle s2_4 " + res);
        res = indexOf_match_spans_end_of_string_unknown_needle(s3_1, "<hit>");
        assertEquals(res, null, "test_indexOf_match_spans_end_of_string_unknown_needle s3_1 " + res);
        res = indexOf_match_spans_end_of_string_unknown_needle(s3_2, "<hit>");
        assertEquals(res, null, "test_indexOf_match_spans_end_of_string_unknown_needle s3_2 " + res);
        res = indexOf_match_spans_end_of_string_unknown_needle(s3_3, "<hit>");
        assertEquals(res, null, "test_indexOf_match_spans_end_of_string_unknown_needle s3_3 " + res);
        res = indexOf_match_spans_end_of_string_unknown_needle(s3_4, "<hit>");
        assertEquals(res, null, "test_indexOf_match_spans_end_of_string_unknown_needle s3_4 " + res);

        res = indexOf_match_spans_end_of_string_imm_needle(s0_1);
        assertEquals(res, null, "test_indexOf_match_spans_end_of_string_imm_needle s0_1 " + res);
        res = indexOf_match_spans_end_of_string_imm_needle(s0_2);
        assertEquals(res, null, "test_indexOf_match_spans_end_of_string_imm_needle s0_2 " + res);
        res = indexOf_match_spans_end_of_string_imm_needle(s0_3);
        assertEquals(res, null, "test_indexOf_match_spans_end_of_string_imm_needle s0_3 " + res);
        res = indexOf_match_spans_end_of_string_imm_needle(s0_4);
        assertEquals(res, null, "test_indexOf_match_spans_end_of_string_imm_needle s0_4 " + res);
        res = indexOf_match_spans_end_of_string_imm_needle(s1_1);
        assertEquals(res, null, "test_indexOf_match_spans_end_of_string_imm_needle s1_1 " + res);
        res = indexOf_match_spans_end_of_string_imm_needle(s1_2);
        assertEquals(res, null, "test_indexOf_match_spans_end_of_string_imm_needle s1_2 " + res);
        res = indexOf_match_spans_end_of_string_imm_needle(s1_3);
        assertEquals(res, null, "test_indexOf_match_spans_end_of_string_imm_needle s1_3 " + res);
        res = indexOf_match_spans_end_of_string_imm_needle(s1_4);
        assertEquals(res, null, "test_indexOf_match_spans_end_of_string_imm_needle s1_4 " + res);
        res = indexOf_match_spans_end_of_string_imm_needle(s2_1);
        assertEquals(res, null, "test_indexOf_match_spans_end_of_string_imm_needle s2_1 " + res);
        res = indexOf_match_spans_end_of_string_imm_needle(s2_2);
        assertEquals(res, null, "test_indexOf_match_spans_end_of_string_imm_needle s2_2 " + res);
        res = indexOf_match_spans_end_of_string_imm_needle(s2_3);
        assertEquals(res, null, "test_indexOf_match_spans_end_of_string_imm_needle s2_3 " + res);
        res = indexOf_match_spans_end_of_string_imm_needle(s2_4);
        assertEquals(res, null, "test_indexOf_match_spans_end_of_string_imm_needle s2_4 " + res);
        res = indexOf_match_spans_end_of_string_imm_needle(s3_1);
        assertEquals(res, null, "test_indexOf_match_spans_end_of_string_imm_needle s3_1 " + res);
        res = indexOf_match_spans_end_of_string_imm_needle(s3_2);
        assertEquals(res, null, "test_indexOf_match_spans_end_of_string_imm_needle s3_2 " + res);
        res = indexOf_match_spans_end_of_string_imm_needle(s3_3);
        assertEquals(res, null, "test_indexOf_match_spans_end_of_string_imm_needle s3_3 " + res);
        res = indexOf_match_spans_end_of_string_imm_needle(s3_4);
        assertEquals(res, null, "test_indexOf_match_spans_end_of_string_imm_needle s3_4 " + res);

        res = indexOf_match_spans_end_of_string_imm2_needle(s0_1x);
        assertEquals(res, null, "test_indexOf_match_spans_end_of_string_imm2_needle s0_1x " + res);
        res = indexOf_match_spans_end_of_string_imm2_needle(s1_1x);
        assertEquals(res, null, "test_indexOf_match_spans_end_of_string_imm2_needle s1_1x " + res);
        res = indexOf_match_spans_end_of_string_imm2_needle(s2_1x);
        assertEquals(res, null, "test_indexOf_match_spans_end_of_string_imm2_needle s2_1x " + res);
        res = indexOf_match_spans_end_of_string_imm2_needle(s3_1x);
        assertEquals(res, null, "test_indexOf_match_spans_end_of_string_imm2_needle s3_1x " + res);
    }

    @Test(role = Role.TEST_HELPER, compileAt = 4, warmup = 1, warmupArgs = { "<t><hi><t><h><hit<t><hit", "<hit>" })
    static String indexOf_match_spans_end_of_string_unknown_needle(String s, String needle) {
        int index = s.indexOf(needle);
        if (index > -1) {
            return "Found needle \"" + needle + "\" that is spanning the end of the string: " + s + ".";
        }
        return null;
    }

    @Test(role = Role.TEST_HELPER, compileAt = 4, warmup = 1, warmupArgs = { "<t><hi><t><h><hit<t><hit" })
    static String indexOf_match_spans_end_of_string_imm_needle(String s) {
        int index = s.indexOf("<hit>");
        if (index > -1) {
            return "Found needle \"<hit>\" that is spanning the end of the string: " + s + ".";
        }
        return null;
    }

    @Test(role = Role.TEST_HELPER, compileAt = 4, warmup = 1, warmupArgs = { "<t><t><t><t><t<t><" })
    static String indexOf_match_spans_end_of_string_imm2_needle(String s) {
        int index = s.indexOf("<h");
        if (index > -1) {
            return "Found needle \"<h\" that is spanning the end of the string: " + s + ".";
        }
        return null;
    }

    static String text16 = "ooooooo";
    static String text11 = "1ooooooo";
    static String text12 = "ooooooo1";
    static String text13 = "oooooooo1";
    static String text14 = "ooooooooo1";
    static String text15 = "oooooooooo1";
    static int len12 = text12.length();
    static int len13 = text13.length();
    static int len14 = text14.length();
    static int len15 = text15.length();

    static String text12_1 = text12.substring(0, len12-1);
    static String text13_1 = text13.substring(0, len13-1);
    static String text14_1 = text14.substring(0, len14-1);
    static String text15_1 = text15.substring(0, len15-1);

    @Test(role = Role.TEST_ENTRY)
    public static void test_indexOf_imm1_needle() {
        assertEquals(     -1, indexOf_imm1_needle(text16), "test_indexOf_imm1_needle no_match");

        assertEquals(      0, indexOf_imm1_needle(text11), "test_indexOf_imm1_needle first_matches");

        assertEquals(len12-1, indexOf_imm1_needle(text12), "test_indexOf_imm1_needle last_matches");
        assertEquals(len13-1, indexOf_imm1_needle(text13), "test_indexOf_imm1_needle last_matches");
        assertEquals(len14-1, indexOf_imm1_needle(text14), "test_indexOf_imm1_needle last_matches");
        assertEquals(len15-1, indexOf_imm1_needle(text15), "test_indexOf_imm1_needle last_matches");

        assertEquals(     -1, indexOf_imm1_needle(text12_1), "test_indexOf_imm1_needle walked_past");
        assertEquals(     -1, indexOf_imm1_needle(text13_1), "test_indexOf_imm1_needle walked_past");
        assertEquals(     -1, indexOf_imm1_needle(text14_1), "test_indexOf_imm1_needle walked_past");
        assertEquals(     -1, indexOf_imm1_needle(text15_1), "test_indexOf_imm1_needle walked_past");
    }

    @Test(role = Role.TEST_HELPER, compileAt = 4, warmup = 1, warmupArgs = { "ooooooo1" })
    static int indexOf_imm1_needle(String s) {
        return s.indexOf("1");
    }

    static String text1UTF16 = "A" + "\u05d0" + "\u05d1" + "B";

    @Test(role = Role.TEST_ENTRY)
    public static void test_indexOf_immUTF16() {
        assertEquals(      3, indexOf_imm1Latin1_needle(text1UTF16), "test_indexOf_immUTF16");
        assertEquals(      1, indexOf_imm1UTF16_needle(text1UTF16), "test_indexOf_immUTF16");
        assertEquals(      1, indexOf_immUTF16_needle(text1UTF16), "test_indexOf_immUTF16");
    }

    @Test(role = Role.TEST_HELPER, compileAt = 4, warmup = 1, warmupArgs = { "A" + "\u05d0" + "\u05d1" + "B" })
    static int indexOf_imm1Latin1_needle(String s) {
        return s.indexOf("B");
    }

    @Test(role = Role.TEST_HELPER, compileAt = 4, warmup = 1, warmupArgs = { "A" + "\u05d0" + "\u05d1" + "B" })
    static int indexOf_imm1UTF16_needle(String s) {
        return s.indexOf("\u05d0");
    }

    @Test(role = Role.TEST_HELPER, compileAt = 4, warmup = 1, warmupArgs = { "A" + "\u05d0" + "\u05d1" + "B" })
    static int indexOf_immUTF16_needle(String s) {
        return s.indexOf("\u05d0" + "\u05d1");
    }

    @Test(role = Role.TEST_HELPER, compileAt = 4, warmup = 1, warmupArgs = { "abc", "abcd" })
    public static int asmStringCompareTo(String a, String b) {
        return a.compareTo(b);
    }

    @Test(role = Role.TEST_ENTRY)
    public static void test_asmStringCompareTo() {
        // null
        try {
            asmStringCompareTo("not null", null);
            assertTrue(false,
                       "TestOther.asmStringCompareTo(\"not null\", null) doesn't throw exception");
        } catch (NullPointerException e) {
            assertEquals("java.lang.String.compareTo",
                         e.getStackTrace()[0].getClassName() + "." +
                         e.getStackTrace()[0].getMethodName(),
                         "TestOther.asmStringCompareTo(\"not null\", null) throws exception");
        }

        // ==0
        {
            // check length 0 optimization
            assertEquals(0, asmStringCompareTo("", ""),
                         "TestOther.asmStringCompareTo(\"\", \"\")");

            // check first character optimization
            assertEquals(0, asmStringCompareTo("A", "A"),
                         "TestOther.asmStringCompareTo(\"A\", \"A\")");

            // check real comparisons
            assertEquals(0, asmStringCompareTo(new String("eq") + new String("ual"), "equal"),
                         "TestOther.asmStringCompareTo(\"equal\", \"equal\")");
            assertEquals(0, asmStringCompareTo("textABC", "textABC"),
                         "TestOther.asmStringCompareTo(\"textABC\", \"textABC\")");
            assertEquals(0,
                         asmStringCompareTo(new String("abcdefgh01234") +
                                            new String("56abcdefgh0123456abcdefgh0123456"),
                                            "abcdefgh0123456abcdefgh0123456abcdefgh0123456"),
                         "TestOther.asmStringCompareTo(\"abcdefgh0123456abcdefgh0123456abcdefgh0123456\", " +
                         "\"abcdefgh0123456abcdefgh0123456abcdefgh0123456\")");
        }

        // <0
        {
            // check first character optimization
            assertEquals(-1, asmStringCompareTo("4", "5"),
                         "TestOther.asmStringCompareTo(\"4\", \"5\")");

            // check real comparisons
            assertEquals(-1, asmStringCompareTo("diff4", "diff5"),
                         "TestOther.asmStringCompareTo(\"diff4\", \"diff5\")");
            assertEquals(-10, asmStringCompareTo("", "123456789A"),
                         "TestOther.asmStringCompareTo(\"\", \"123456789A\")");
            assertEquals(-10, asmStringCompareTo("ZYX", "ZYX123456789A"),
                         "TestOther.asmStringCompareTo(\"ZYX\", \"ZYX123456789A\")");
        }

        // >0
        {
            // check first character optimization
            assertEquals(1, asmStringCompareTo("5", "4"),
                         "TestOther.asmStringCompareTo(\"5\", \"4\")");

            // check real comparisons
            assertEquals(1, asmStringCompareTo("diff5", "diff4"),
                         "TestOther.asmStringCompareTo(\"diff5\", \"diff4\")");
            assertEquals(10, asmStringCompareTo("123456789A", ""),
                         "TestOther.asmStringCompareTo(\"123456789A\", \"\")");
            assertEquals(10, asmStringCompareTo("ZYX123456789A", "ZYX"),
                         "TestOther.asmStringCompareTo(\"ZYX123456789A\", \"ZYX\")");
        }

        // very long strings (100k)
        {
            char[] ac = new char[(100 * 1024)];
            for (int i = 0; i < (100 * 1024); i += 315)
                ac[i] = (char) ((i % 12) + 'a');
            char[] bc = new char[(100 * 1024)];
            for (int i = 0; i < (100 * 1024); i += 315)
                bc[i] = (char) ((i % 12) + 'a');

            ac[(100 * 1024) - 1] = '2';
            bc[(100 * 1024) - 1] = '2';
            String a1 = new String(ac);
            String b1 = new String(bc);
            assertEquals(0, asmStringCompareTo(a1, b1),
                         "TestOther.asmStringCompareTo(very_long_strings_1)");

            ac[(100 * 1024) - 1] = 'X';
            bc[(100 * 1024) - 1] = 'Z';
            String a2 = new String(ac);
            String b2 = new String(bc);
            assertEquals(-2, asmStringCompareTo(a2, b2),
                         "TestOther.asmStringCompareTo(very_long_strings_2)");
        }

        // very very long strings (2M)
        {
            char[] ac = new char[(2 * 1024 * 1024)];
            for (int i = 0; i < (2 * 1024 * 1024); i += 315)
                ac[i] = (char) ((i % 12) + 'a');
            char[] bc = new char[(2 * 1024 * 1024)];
            for (int i = 0; i < (2 * 1024 * 1024); i += 315)
                bc[i] = (char) ((i % 12) + 'a');

            ac[(2 * 1024 * 1024) - 1] = '3';
            bc[(2 * 1024 * 1024) - 1] = '3';
            String a1 = new String(ac);
            String b1 = new String(bc);
            assertEquals(0, asmStringCompareTo(a1, b1),
                         "TestOther.asmStringCompareTo(very_very_long_strings_1)");

            ac[(2 * 1024 * 1024) - 1] = 'W';
            bc[(2 * 1024 * 1024) - 1] = 'Z';
            String a2 = new String(ac);
            String b2 = new String(bc);
            assertEquals(-3, asmStringCompareTo(a2, b2),
                         "TestOther.asmStringCompareTo(very_very_long_strings_2)");
        }

        // See bug 8215100
        {
            assertEquals(-20, asmStringCompareTo("e.\u0259.", "y.e."));
            assertEquals(20, asmStringCompareTo("y.e.", "e.\u0259."));
        }
    }


    @Test(role = Role.TEST_HELPER, compileAt = 4, warmup = 1, warmupArgs = { "abc", "abcd" })
    public static boolean asmStringEquals(String a, String b) {
        return a.equals(b);
    }

    static String a1 = "abcd";
    static String b1 = "abcd";
    static final String a2 = "1234";
    static final String b2 = "1234";

    @Test(role = Role.TEST_HELPER, compileAt = 4, warmup = 1)
    public static boolean asmStringEqualsConst() {
        boolean ret = a1.equals(b1);
        ret &= a2.equals(b2);
        ret &= !a2.equals(b1);
        ret &= "ABCD".equals("ABCD");
        return ret;
    }


    @Test(role = Role.TEST_ENTRY)
    public static void test_asmStringEquals() {
        // null
        {
            assertFalse(asmStringEquals("not null", null),
                        "TestOther.asmStringEquals(\"not null\", null)");
        }

        // true
        {
            // check constant optimization
            assertTrue(asmStringEqualsConst(),
                       "TestOther.asmStringEqualsConst(\"\", \"\")");

            // check length 0 optimization
            assertTrue(asmStringEquals("", ""),
                       "TestOther.asmStringEquals(\"\", \"\")");

            // check first character optimization
            assertTrue(asmStringEquals("A", "A"),
                       "TestOther.asmStringEquals(\"A\", \"A\")");

            // check real comparisons
            assertTrue(asmStringEquals(new String("eq") + new String("ual"), "equal"),
                       "TestOther.asmStringEquals(\"equal\", \"equal\")");
            assertTrue(asmStringEquals("textABC", "textABC"),
                       "TestOther.asmStringEquals(\"textABC\", \"textABC\")");
            assertTrue(asmStringEquals(new String("abcdefgh01234") +
                                       new String("56abcdefgh0123456abcdefgh0123456"),
                                       "abcdefgh0123456abcdefgh0123456abcdefgh0123456"),
                       "TestOther.asmStringEquals(\"abcdefgh0123456abcdefgh0123456abcdefgh0123456\", " +
                       "\"abcdefgh0123456abcdefgh0123456abcdefgh0123456\")");
        }
    }

}
