/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6843181 6943963
 * @summary Confirm that NumericShaper is thread-safe.
 * @run main/timeout=300/othervm MTTest
 */

import java.awt.font.NumericShaper;
import java.util.Arrays;
import java.util.EnumSet;
import static java.awt.font.NumericShaper.*;

public class MTTest {
    static volatile boolean runrun = true;
    static volatile boolean err = false;

    final static String text = "-123 (English) 456.00 (Arabic) \u0641\u0642\u0643 -456 (Thai) \u0e01\u0e33 01.23";
    final static char[] expected1 = "-123 (English) 456.00 (Arabic) \u0641\u0642\u0643 -\u06f4\u06f5\u06f6 (Thai) \u0e01\u0e33 \u0e50\u0e51.\u0e52\u0e53".toCharArray(); // for EASTERN_ARABIC
    final static char[] expected2 = "-123 (English) 456.00 (Arabic) \u0641\u0642\u0643 -\u0664\u0665\u0666 (Thai) \u0e01\u0e33 \u0e50\u0e51.\u0e52\u0e53".toCharArray(); // for ARABIC

    static NumericShaper ns1, ns2, ns3, ns4;

    public static void main(String[] args) {
        System.out.println("original: " + text);
        ns1 = getContextualShaper(EnumSet.of(Range.EASTERN_ARABIC, Range.THAI),
                                  Range.EUROPEAN);
        ns2 = getContextualShaper(EnumSet.of(Range.ARABIC, Range.THAI),
                                  Range.EUROPEAN);
        System.out.println("expected for Eastern-Arabic & Thai: " +
                           String.valueOf(expected1));
        System.out.println("expected for Arabic & Thai: " +
                           String.valueOf(expected2));

        ns3 = getContextualShaper(EASTERN_ARABIC|THAI, EUROPEAN);
        ns4 = getContextualShaper(ARABIC|THAI, EUROPEAN);

        Thread th1 = new Thread(new Work(ns1, expected1));
        Thread th2 = new Thread(new Work(ns2, expected2));
        Thread th3 = new Thread(new Work(ns1, expected1));
        Thread th4 = new Thread(new Work(ns2, expected2));
        Thread th5 = new Thread(new Work(ns3, expected1));
        Thread th6 = new Thread(new Work(ns4, expected2));
        Thread th7 = new Thread(new Work(ns3, expected1));
        Thread th8 = new Thread(new Work(ns4, expected2));

        th1.start();
        th2.start();
        th3.start();
        th4.start();
        th5.start();
        th6.start();
        th7.start();
        th8.start();

        try {
            for (int i = 0; runrun && i < 180; i++) {
                Thread.sleep(1000); // 1 seconds
            }
            runrun = false;
            th1.join();
            th2.join();
            th3.join();
            th4.join();
            th5.join();
            th6.join();
            th7.join();
            th8.join();
        }
        catch (InterruptedException e) {
        }

        if (err) {
            throw new RuntimeException("Thread-safe test failed.");
        }
    }

    private static class Work implements Runnable {
        NumericShaper ns;
        char[] expectedText;

        Work(NumericShaper ns, char[] expectedText) {
            this.ns = ns;
            this.expectedText = expectedText;

        }

        public void run() {
            int count = 0;
            while (runrun) {
                char[] t = text.toCharArray();
                count++;
                try {
                    ns.shape(t, 0, t.length);
                } catch (Exception e) {
                    System.err.println("Error: Unexpected exception: " + e);
                    runrun = false;
                    err = true;
                    return;
                }
                if (!Arrays.equals(t, expectedText)) {
                    System.err.println("Error: shape() returned unexpected value: ");
                    System.err.println("count = " + count);
                    System.err.println("   expected: " + String.valueOf(expectedText));
                    System.err.println("        got: " + String.valueOf(t));
                    runrun = false;
                    err = true;
                    return;
                }
            }
        }
    }
}
