/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4221795 8032446 8174270
 * @summary Confirm that java.text.Normalizer and sun.text.Normalizer are
 * thread-safe.
 * @modules java.base/sun.text java.base/jdk.internal.icu.text
 * @compile -XDignore.symbol.file ThreadSafeTest.java
 * @run main/othervm -esa ThreadSafeTest 5 10
 */

// Usage: java ThreadSafeTest [threadsFactor [duration]]
public class ThreadSafeTest {

    static volatile boolean runrun = true;
    static volatile boolean error = false;

    public static void main(String[] args) throws Exception {
        int threadsFactor = 5;
        if (args.length > 0) {
            threadsFactor = Math.max(Integer.parseInt(args[0]), 5);
        }
        int duration = 180;
        if (args.length > 1) {
            duration = Math.max(5, Integer.parseInt(args[1]));
        }
        int nProcessors = Runtime.getRuntime().availableProcessors();
        int nTasks = nProcessors * threadsFactor;
        Thread[] tasks = new Thread[nTasks];

        System.out.println("Testing with " + nTasks + " threads on " +
                           nProcessors + " processors for " + duration +
                           " seconds.");

        for (int i = 0; i < nTasks; i++) {
            tasks[i] = new Thread(new Worker());
        }
        for (int i = 0; i < nTasks; i++) {
            tasks[i].start();
        }

        try {
            for (int i = 0; runrun && i < duration; i++) {
                Thread.sleep(1000); // 1 second
            }
            runrun = false;
            for (int i = 0; i < nTasks; i++) {
                tasks[i].join();
            }
        }
        catch (InterruptedException e) {
        }

        if (error) {
            throw new RuntimeException("Normalizer is not thread-safe.");
        }
    }

    static void testJavaNormalize(int num, java.text.Normalizer.Form form) {
        String got = java.text.Normalizer.normalize(data[num][0], form);
        if (!got.equals(data[num][1])) {
            System.err.println("java.text.Normalizer.normalize(" +
                               form.toString() + ") failed.");
            error = true;
        }
    }

    static void testSunNormalize(int num, java.text.Normalizer.Form form,
                                 int option) {
        String got = sun.text.Normalizer.normalize(data[num][0], form, option);
        if (!got.equals(data[num][1])) {
            System.err.println("sun.text.Normalizer.normalize(" +
                               form.toString() + ", " +
                               Integer.toHexString(option) + ") failed.");
            error = true;
        }
    }

    static void testIsNormalized(int num, java.text.Normalizer.Form form) {
        boolean normalized = java.text.Normalizer.isNormalized(data[num][1], form);
        if (!normalized) {
            System.err.println("java.text.Normalizer.isNormalized(" +
                               form.toString() + ") failed.");
            error = true;
        }
    }

    static class Worker implements Runnable {
        public void run() {
            while (runrun) {
                testJavaNormalize(0, java.text.Normalizer.Form.NFKC);
                testSunNormalize(1, java.text.Normalizer.Form.NFC,
                                 sun.text.Normalizer.UNICODE_3_2);
                testJavaNormalize(2, java.text.Normalizer.Form.NFKD);
                testSunNormalize(3, java.text.Normalizer.Form.NFC,
                            jdk.internal.icu.text.NormalizerBase.UNICODE_LATEST);
                testJavaNormalize(4, java.text.Normalizer.Form.NFD);

                testIsNormalized(0, java.text.Normalizer.Form.NFKC);
                testIsNormalized(2, java.text.Normalizer.Form.NFKD);
                testIsNormalized(4, java.text.Normalizer.Form.NFD);

                if (error) {
                    runrun = false;
                    return;
                }
            }
        }
    }

    static final String[][] data = {
     /*      From:                             To:           */
      {"A\u0300\u0316",                  "\u00C0\u0316"},
      {"\u0071\u0307\u0323\u0072",       "\u0071\u0323\u0307\u0072"},
      {"\u0f77",                         "\u0fb2\u0f71\u0f80"},
      {"D\u0307\u0328\u0323",            "\u1e0c\u0328\u0307"},
      {"\u0f71\u0f72\u0f73\u0f74\u0f75",
                 "\u0F71\u0F71\u0F71\u0F72\u0F72\u0F74\u0F74"},
    };
}
