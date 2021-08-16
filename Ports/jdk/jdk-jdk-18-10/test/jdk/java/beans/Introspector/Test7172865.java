/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.beans.IndexedPropertyDescriptor;
import java.beans.MethodDescriptor;
import java.beans.PropertyDescriptor;

/*
 * @test
 * @bug 7172854 7172865
 * @summary Tests that cached methods are not lost
 * @author Sergey Malenkov
 * @run main/othervm -Xmx128m Test7172865
 */

public class Test7172865 {
    public static void main(String[] args) throws Exception {
        int errors = 0;

        MethodDescriptor md = new MethodDescriptor(Test7172865.class.getMethod("getGood"));

        errors += test(PropertyDescriptor.class, "good", true);
        PropertyDescriptor pdGoodString = new PropertyDescriptor("good", Test7172865.class, "getGood", "setGood");
        PropertyDescriptor pdGoodMethod = new PropertyDescriptor("good",
                Test7172865.class.getMethod("getGood"),
                Test7172865.class.getMethod("setGood", args.getClass()));

        errors += test(PropertyDescriptor.class, "bad", false);
        PropertyDescriptor pdBadString = new PropertyDescriptor("bad", Test7172865.class, "getBad", null);
        PropertyDescriptor pdBadMethod = new PropertyDescriptor("bad",
                Test7172865.class.getMethod("getBad"),
                Test7172865.class.getMethod("setBad", args.getClass()));

        errors += test(IndexedPropertyDescriptor.class, "good", true);
        IndexedPropertyDescriptor ipdGoodString = new IndexedPropertyDescriptor("good", Test7172865.class, "getGood", "setGood", "getGood", "setGood");
        IndexedPropertyDescriptor ipdGoodMethod = new IndexedPropertyDescriptor("good",
                Test7172865.class.getMethod("getGood"),
                Test7172865.class.getMethod("setGood", args.getClass()),
                Test7172865.class.getMethod("getGood", Integer.TYPE),
                Test7172865.class.getMethod("setGood", Integer.TYPE, String.class));

        errors += test(IndexedPropertyDescriptor.class, "bad", false);
        IndexedPropertyDescriptor ipdBadString = new IndexedPropertyDescriptor("bad", Test7172865.class, "getBad", null, "getBad", null);
        IndexedPropertyDescriptor ipdBadMethod = new IndexedPropertyDescriptor("bad",
                Test7172865.class.getMethod("getBad"),
                Test7172865.class.getMethod("setBad", args.getClass()),
                Test7172865.class.getMethod("getBad", Integer.TYPE),
                Test7172865.class.getMethod("setBad", Integer.TYPE, String.class));

        for (int i = 1; i <= 2; i++) {
            System.out.println("STEP: " + i);
            errors += test("md", null != md.getMethod());

            errors += test("pdGoodString", pdGoodString, true, true);
            errors += test("pdGoodMethod", pdGoodMethod, true, true);

            errors += test("pdBadString", pdBadString, true, false);
            errors += test("pdBadMethod", pdBadMethod, true, true);

            errors += test("ipdGoodString", ipdGoodString, true, true, true, true);
            errors += test("ipdGoodMethod", ipdGoodMethod, true, true, true, true);

            errors += test("ipdBadString", ipdBadString, true, false, true, false);
            errors += test("ipdBadMethod", ipdBadMethod, true, true, true, true);

            try {
                int[] array = new int[1024];
                while (true) {
                    array = new int[array.length << 1];
                }
            }
            catch (OutOfMemoryError error) {
                System.gc();
            }
        }
        if (errors > 0) {
            throw new Error("found " + errors + " errors");
        }
    }

    private static int test(Class<?> type, String property, boolean value) {
        String message = type.getSimpleName() + "(" + property + ") ";
        try {
            type.getConstructor(String.class, Class.class).newInstance(property, Test7172865.class);
            message += "passed";
        }
        catch (Exception exception) {
            message += "failed";
            value = !value;
        }
        if (value) {
            message += " as expected";
        }
        System.out.println(message);
        return value ? 0 : 1;
    }

    private static int test(String message, boolean value) {
        System.out.println(message + ": " + (value ? "passed" : "failed"));
        return value ? 0 : 1;
    }

    private static int test(String message, PropertyDescriptor pd, boolean rm, boolean wm) {
        return test(message + ".Read", rm == (null != pd.getReadMethod()))
             + test(message + ".Write", wm == (null != pd.getWriteMethod()));
    }

    private static int test(String message, IndexedPropertyDescriptor ipd, boolean rm, boolean wm, boolean irm, boolean iwm) {
        return test(message, ipd, rm, wm)
             + test(message + ".IndexedRead", irm == (null != ipd.getIndexedReadMethod()))
             + test(message + ".IndexedWrite", iwm == (null != ipd.getIndexedWriteMethod()));
    }

    public String[] getGood() {
        return null;
    }

    public String getGood(int index) {
        return null;
    }

    public void setGood(String[] good) {
    }

    public void setGood(int index, String value) {
    }

    public String[] getBad() {
        return null;
    }

    public String getBad(int index) {
        return null;
    }

    public Test7172865 setBad(String[] bad) {
        return null;
    }

    public Test7172865 setBad(int index, String value) {
        return null;
    }
}
