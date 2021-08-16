/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug     8145780
 * @author  a.stepanov
 * @summary Some checks for TIFFTag, TIFFTagSet
 * @run     main TIFFTagSetTest
 */


import javax.imageio.plugins.tiff.*;

import java.lang.reflect.Method;
import java.util.*;


public class TIFFTagSetTest {

    public static class TestSet extends TIFFTagSet {

        public static final TIFFTagSet SOME_SET =
            new TIFFTagSet(new ArrayList<TIFFTag>());

        private static TestSet theInstance = null;

        public static final int TAG_NUM_1  = 0;
        public static final int TAG_NUM_2  = 666;
        public static final int TAG_NUM_3  = Integer.MAX_VALUE;

        public static final String TAG_NAME_1  = "tag-1";
        public static final String TAG_NAME_2  = "tag-2";
        public static final String TAG_NAME_3  = "tag-3";

        public static final int VALUE_1 = 123;
        public static final int VALUE_2 = 321;

        public static final String VALUE_NAME_1 = "value-1";
        public static final String VALUE_NAME_2 = "value-2";

        public static final int VALUE_COUNT = 500;


        static class Tag1 extends TIFFTag {
            public Tag1() {
                super(TAG_NAME_1,
                      TAG_NUM_1,
                      1 << TIFF_SHORT | 1 << TIFF_LONG,
                      VALUE_COUNT);
            }
        }

        static class Tag2 extends TIFFTag {
            public Tag2() {
                super(TAG_NAME_2,
                      TAG_NUM_2,
                      1 << TIFF_DOUBLE);

                addValueName(VALUE_1, VALUE_NAME_1);
                addValueName(VALUE_2, VALUE_NAME_2);
            }
        }

        static class Tag3 extends TIFFTag {
            public Tag3() {
                super(TAG_NAME_3,
                      TAG_NUM_3,
                      SOME_SET);
            }
        }

        private static List<TIFFTag> tags;

        private static void initTags() {

            tags = new ArrayList<TIFFTag>();

            tags.add(new TestSet.Tag1());
            tags.add(new TestSet.Tag2());
            tags.add(new TestSet.Tag3());
        }

        private TestSet() { super(tags); }

        public synchronized static TestSet getInstance() {

            if (theInstance == null) {
                initTags();
                theInstance = new TestSet();
                tags = null;
            }
            return theInstance;
        }
    }


    private static void checkEq(String what, Object v, Object ref) {
        if (v == null) {
            throw new RuntimeException(what + " is null");
        } else if (!v.equals(ref)) {
            throw new RuntimeException("invalid " + what +
                ", expected: " + ref + ", got: " + v);
        }
    }




    private final String className;
    public TIFFTagSetTest(String cName) { className = cName; }

    public void testNamesNumbers() throws ReflectiveOperationException {

        Class<?> c = Class.forName(className);

        Method getInstance = c.getMethod("getInstance", new Class[]{});
        Object o = getInstance.invoke(new Object[]{});

        TIFFTagSet tagSet = (TIFFTagSet) o;
        SortedSet tagNames   = tagSet.getTagNames();
        SortedSet tagNumbers = tagSet.getTagNumbers();

        int nTagNames = tagNames.size();
        if (nTagNames != tagNumbers.size()) {
            throw new RuntimeException("Error: unequal sizes for tag names set "
                    + "and tag numbers set");
        }
        System.out.println("\n" + nTagNames + " tag names/numbers\n");

        for (final Iterator itName = tagNames.iterator(); itName.hasNext(); ) {

            String tagName = (String) itName.next();
            // just in case
            if (tagName == null) {
                throw new RuntimeException("null tag name");
            }

            TIFFTag tagByName = tagSet.getTag(tagName);
            System.out.println(
                "name/number: \t" + tagName + "/" + tagByName.getNumber());
            checkEq("tag name", tagByName.getName(), tagName);
            TIFFTag tagByNum = tagSet.getTag(tagByName.getNumber());
            checkEq("tag name", tagByNum.getName(), tagName);

            if (tagByName.isIFDPointer() &&
               !tagByName.isDataTypeOK(TIFFTag.TIFF_IFD_POINTER)) {
                throw new RuntimeException("Error: " + tagName +
                    "must be an IFD pointer");
            }
        }
        System.out.println("");

        for (final Iterator itNum = tagNumbers.iterator(); itNum.hasNext(); ) {

            int tagNum = (int) itNum.next();
            // just in case
            if (tagNum < 0) {
                throw new RuntimeException("negative tag number");
            }
            TIFFTag tagByNum = tagSet.getTag(tagNum);

            System.out.println(
                "number/name: \t" + tagNum + "/" + tagByNum.getName());
            checkEq("tag number", tagByNum.getNumber(), tagNum);
            TIFFTag tagByName = tagSet.getTag(tagByNum.getName());
            checkEq("tag number", tagByName.getNumber(), tagNum);
        }

        System.out.println("");
    }


    private static void testUserDefTagSet() {

        TIFFTagSet set = TestSet.getInstance();

        SortedSet tagNames = set.getTagNames();
        checkEq("tagNames set size", tagNames.size(), 3);
        if (! (tagNames.contains(TestSet.TAG_NAME_1) &&
               tagNames.contains(TestSet.TAG_NAME_2) &&
               tagNames.contains(TestSet.TAG_NAME_3)) ) {
            throw new RuntimeException("invalid tag names");
        }

        SortedSet tagNumbers = set.getTagNumbers();
        checkEq("tagNumbers set size", tagNumbers.size(), 3);
        if (! (tagNumbers.contains(TestSet.TAG_NUM_1) &&
               tagNumbers.contains(TestSet.TAG_NUM_2) &&
               tagNumbers.contains(TestSet.TAG_NUM_3)) ) {
            throw new RuntimeException("invalid tag numbers");
        }

        TIFFTag t1 = set.getTag(TestSet.TAG_NUM_1),
                t2 = set.getTag(TestSet.TAG_NUM_2),
                t3 = set.getTag(TestSet.TAG_NUM_3);

        checkEq(TestSet.TAG_NAME_1 + " name", t1.getName(), TestSet.TAG_NAME_1);
        checkEq(TestSet.TAG_NAME_2 + " name", t2.getName(), TestSet.TAG_NAME_2);
        checkEq(TestSet.TAG_NAME_3 + " name", t3.getName(), TestSet.TAG_NAME_3);

        // check count
        // was set
        checkEq(TestSet.TAG_NAME_1 + " count",
            t1.getCount(),  TestSet.VALUE_COUNT);
        // undefined
        checkEq(TestSet.TAG_NAME_2 + " count", t2.getCount(), -1);
        // see docs for constructor TIFFTag(String, int, TIFFTagSet)
        checkEq(TestSet.TAG_NAME_3 + " count", t3.getCount(),  1);

        // check datatypes
        checkEq(TestSet.TAG_NAME_1 + " datatypes", t1.getDataTypes(),
            1 << TIFFTag.TIFF_SHORT | 1 << TIFFTag.TIFF_LONG);
        boolean ok =  t1.isDataTypeOK(TIFFTag.TIFF_SHORT)  &&
                      t1.isDataTypeOK(TIFFTag.TIFF_LONG)   &&
                     !t1.isDataTypeOK(TIFFTag.TIFF_DOUBLE) &&
                     !t1.isDataTypeOK(TIFFTag.TIFF_IFD_POINTER);
        if (!ok) { throw new RuntimeException(TestSet.TAG_NAME_1 + ": " +
            "isDataTypeOK check failed"); }
        checkEq(TestSet.TAG_NAME_1 + ".isIFDPointer()", t1.isIFDPointer(), false);

        checkEq(TestSet.TAG_NAME_2 + " datatypes", t2.getDataTypes(),
            1 << TIFFTag.TIFF_DOUBLE);
        ok = !t2.isDataTypeOK(TIFFTag.TIFF_SHORT)  &&
             !t2.isDataTypeOK(TIFFTag.TIFF_LONG)   &&
              t2.isDataTypeOK(TIFFTag.TIFF_DOUBLE) &&
             !t2.isDataTypeOK(TIFFTag.TIFF_IFD_POINTER);
        if (!ok) { throw new RuntimeException(TestSet.TAG_NAME_2 + ": " +
            "isDataTypeOK check failed"); }
        checkEq(TestSet.TAG_NAME_2 + ".isIFDPointer()", t2.isIFDPointer(), false);

        // see docs for constructor TIFFTag(String, int, TIFFTagSet)
        checkEq(TestSet.TAG_NAME_3 + " datatypes", t3.getDataTypes(),
            1 << TIFFTag.TIFF_LONG | 1 << TIFFTag.TIFF_IFD_POINTER);
        ok = !t3.isDataTypeOK(TIFFTag.TIFF_SHORT)  &&
              t3.isDataTypeOK(TIFFTag.TIFF_LONG)   &&
             !t3.isDataTypeOK(TIFFTag.TIFF_DOUBLE) &&
              t3.isDataTypeOK(TIFFTag.TIFF_IFD_POINTER);
        if (!ok) { throw new RuntimeException(TestSet.TAG_NAME_3 + ": " +
            "isDataTypeOK check failed"); }
        checkEq(TestSet.TAG_NAME_3 + ".isIFDPointer()", t3.isIFDPointer(), true);

        // check value names
        checkEq(TestSet.TAG_NAME_1 + ".hasValueNames()",
            t1.hasValueNames(), false);
        checkEq(TestSet.TAG_NAME_2 + ".hasValueNames()",
            t2.hasValueNames(), true);
        checkEq(TestSet.TAG_NAME_3 + ".hasValueNames()",
            t3.hasValueNames(), false);

        if (t1.getNamedValues() != null && t3.getNamedValues() != null) {
            throw new RuntimeException(TestSet.TAG_NAME_1 + " and " +
                TestSet.TAG_NAME_3 + " must have null value names arrays");
        }

        checkEq("number of " + TestSet.TAG_NAME_2 + " values",
            t2.getNamedValues().length, 2);
        checkEq("name of value " + TestSet.VALUE_1,
            t2.getValueName(TestSet.VALUE_1), TestSet.VALUE_NAME_1);
        checkEq("name of value " + TestSet.VALUE_2,
            t2.getValueName(TestSet.VALUE_2), TestSet.VALUE_NAME_2);

        // check tag sets
        if (!(t1.getTagSet() == null && t2.getTagSet() == null) &&
              t3.getTagSet().equals(TestSet.SOME_SET)) {
            throw new RuntimeException("invalid containing tag set");
        }
    }

    private static void checkArgs() {

        boolean ok = false;
        try {
            TIFFTag t = new TIFFTag(null, 0, 1 << TIFFTag.TIFF_LONG);
        } catch (Exception e) {
            ok = true;
        }
        if (!ok) {
            throw new RuntimeException("null names should not be allowed");
        }

        ok = false;
        try {
            TIFFTag t = new TIFFTag("abc", -1, 1 << TIFFTag.TIFF_LONG);
        } catch (Exception e) {
            ok = true;
        }
        if (!ok) {
            throw new RuntimeException("negative numbers should not be allowed");
        }

        ok = false;
        try {
            TIFFTag t = new TIFFTag("abc", 666, ~0x3fff);
        } catch (Exception e) {
            ok = true;
        }
        if (!ok) {
            throw new RuntimeException("disallowed data types were set");
        }

        ok = false;
        try {
            TIFFTag.getSizeOfType(TIFFTag.MIN_DATATYPE - 1);
        } catch (Exception e) {
            ok = true;
        }
        if (!ok) { throw new RuntimeException(
            "missing data types check for getSizeOfType()"); }

        ok = false;
        try {
            TIFFTag.getSizeOfType(TIFFTag.MAX_DATATYPE + 1);
        } catch (Exception e) {
            ok = true;
        }
        if (!ok) { throw new RuntimeException(
            "missing data types check for getSizeOfType()"); }

    }


    public static void main(String[] args) throws ReflectiveOperationException {

        String classNames[] = {"BaselineTIFFTagSet",
                               "ExifGPSTagSet",
                               "ExifInteroperabilityTagSet",
                               "ExifParentTIFFTagSet",
                               "ExifTIFFTagSet",
                               "FaxTIFFTagSet",
                               "GeoTIFFTagSet"};

        for (String cn: classNames) {
            System.out.println("Testing " + cn + ":");
            (new TIFFTagSetTest(
                "javax.imageio.plugins.tiff." + cn)).testNamesNumbers();
        }

        System.out.println("\nTesting user-defined tag set:");
        testUserDefTagSet();
        (new TIFFTagSetTest("TIFFTagSetTest$TestSet")).testNamesNumbers();

        System.out.println("\nSome additional argument checks...");
        checkArgs();
        System.out.println("done");
    }
}
