/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @summary tests for class-specific values
 * @compile ClassValueTest.java
 * @run testng/othervm test.java.lang.invoke.ClassValueTest
 */

package test.java.lang.invoke;

import org.testng.*;
import static org.testng.AssertJUnit.*;
import org.testng.annotations.*;

/**
 * @author jrose
 */
public class ClassValueTest {
    static String nameForCV1(Class<?> type) {
        return "CV1:" + type.getName();
    }
    int countForCV1;
    final ClassValue<String> CV1 = new CV1();
    private class CV1 extends ClassValue<String> {
        protected String computeValue(Class<?> type) {
            countForCV1++;
            return nameForCV1(type);
        }
    }

    static final Class<?>[] CLASSES = {
        String.class,
        Integer.class,
        int.class,
        boolean[].class,
        char[][].class,
        ClassValueTest.class
    };

    @Test
    public void testGet() {
        countForCV1 = 0;
        for (Class<?> c : CLASSES) {
            assertEquals(nameForCV1(c), CV1.get(c));
        }
        assertEquals(CLASSES.length, countForCV1);
        for (Class<?> c : CLASSES) {
            assertEquals(nameForCV1(c), CV1.get(c));
        }
        assertEquals(CLASSES.length, countForCV1);
    }

    @Test
    public void testRemove() {
        for (Class<?> c : CLASSES) {
            CV1.get(c);
        }
        countForCV1 = 0;
        int REMCOUNT = 3;
        for (int i = 0; i < REMCOUNT; i++) {
            CV1.remove(CLASSES[i]);
        }
        assertEquals(0, countForCV1);  // no change
        for (Class<?> c : CLASSES) {
            assertEquals(nameForCV1(c), CV1.get(c));
        }
        assertEquals(REMCOUNT, countForCV1);
    }

    static String nameForCVN(Class<?> type, int n) {
        return "CV[" + n + "]" + type.getName();
    }
    int countForCVN;
    class CVN extends ClassValue<String> {
        final int n;
        CVN(int n) { this.n = n; }
        protected String computeValue(Class<?> type) {
            countForCVN++;
            return nameForCVN(type, n);
        }
    };

    @Test
    public void testGetMany() {
        int CVN_COUNT1 = 100, CVN_COUNT2 = 100;
        CVN cvns[] = new CVN[CVN_COUNT1 * CVN_COUNT2];
        for (int n = 0; n < cvns.length; n++) {
            cvns[n] = new CVN(n);
        }
        countForCVN = 0;
        for (int pass = 0; pass <= 2; pass++) {
            for (int i1 = 0; i1 < CVN_COUNT1; i1++) {
                eachClass:
                for (Class<?> c : CLASSES) {
                    for (int i2 = 0; i2 < CVN_COUNT2; i2++) {
                        int n = i1*CVN_COUNT2 + i2;
                        assertEquals(0, countForCVN);
                        assertEquals(nameForCVN(c, n), cvns[n].get(c));
                        cvns[n].get(c);  //get it again
                        //System.out.println("getting "+n+":"+cvns[n].get(c));
                        boolean doremove = (((i1 + i2) & 3) == 0);
                        switch (pass) {
                        case 0:
                            assertEquals(1, countForCVN);
                            break;
                        case 1:
                            // remove on middle pass
                            assertEquals(0, countForCVN);
                            if (doremove) {
                                //System.out.println("removing "+n+":"+cvns[n].get(c));
                                cvns[n].remove(c);
                                assertEquals(0, countForCVN);
                            }
                            break;
                        case 2:
                            assertEquals(doremove ? 1 : 0, countForCVN);
                            break;
                        }
                        countForCVN = 0;
                        if (i1 > i2 && i1 < i2+5)  continue eachClass;  // leave diagonal gap
                    }
                }
            }
        }
        assertEquals(countForCVN, 0);
        System.out.println("[rechecking values]");
        for (int i = 0; i < cvns.length * 10; i++) {
            int n = i % cvns.length;
            for (Class<?> c : CLASSES) {
                assertEquals(nameForCVN(c, n), cvns[n].get(c));
            }
        }
    }
}
