/*
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6304542
 * @summary Verifies that various Area methods throw NPE for null arguments
 */

import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public class NPETests {
    static boolean verbose;
    static int numfailures = 0;

    public static void main(String argv[]) {
        verbose = (argv.length > 0);
        test(new Runnable() {
            public void run() { new Area(null); }
        });
        test(new Runnable() {
            public void run() { new Area().add(null); }
        });
        test(new Runnable() {
            public void run() { new Area().subtract(null); }
        });
        test(new Runnable() {
            public void run() { new Area().intersect(null); }
        });
        test(new Runnable() {
            public void run() { new Area().exclusiveOr(null); }
        });
        test(new Runnable() {
            public void run() { new Area().transform(null); }
        });
        test(new Runnable() {
            public void run() { new Area().createTransformedArea(null); }
        });
        test(new Runnable() {
            public void run() { new Area().contains((Point2D) null); }
        });
        test(new Runnable() {
            public void run() { new Area().contains((Rectangle2D) null); }
        });
        test(new Runnable() {
            public void run() { new Area().intersects((Rectangle2D) null); }
        });
        if (numfailures > 0) {
            throw new RuntimeException(numfailures+
                                       " methods failed to throw NPE");
        }
    }

    public static void test(Runnable r) {
        try {
            r.run();
            numfailures++;
            if (verbose) {
                new RuntimeException(r+" failed to throw NPE")
                    .printStackTrace();
            }
        } catch (NullPointerException e) {
        }
    }
}
