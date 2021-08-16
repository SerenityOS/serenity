/*
 * Copyright (c) 2005, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6293769 6294527
 * @summary test that the isCardPresent()/waitForX() APIs work correctly
 * @author Andreas Sterbenz
 * @modules java.smartcardio/javax.smartcardio
 * @run main/manual TestPresent
 */

// This test requires special hardware.

import java.util.List;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.TerminalFactory;

public class TestPresent extends Utils {

    private static class Timer {
        private long time = System.currentTimeMillis();
        long update() {
            long t = System.currentTimeMillis();
            long diff = t - time;
            time = t;
            return diff;
        }
        long print() {
            long t = update();
            System.out.println("Elapsed time: " + t + " ms.");
            return t;
        }
    }

    private static boolean isFalse(boolean b) throws Exception {
        if (b) {
            throw new Exception("not false");
        }
        return b;
    }

    private static boolean isTrue(boolean b) throws Exception {
        if (!b) {
            throw new Exception("not true");
        }
        return b;
    }

    public static void main(String[] args) throws Exception {
        CardTerminal terminal = getTerminal(args);
        if (terminal == null) {
            System.out.println("Skipping the test: " +
                    "no card terminals available");
            return;
        }

        while (terminal.isCardPresent()) {
            System.out.println("*** Remove card!");
            Thread.sleep(1000);
        }

        Timer timer = new Timer();

        System.out.println("Testing waitForCardAbsent() with card already absent...");
        isTrue(terminal.waitForCardAbsent(10));
        timer.print();
        isTrue(terminal.waitForCardAbsent(100));
        timer.print();
        isTrue(terminal.waitForCardAbsent(10000));
        timer.print();
        isTrue(terminal.waitForCardAbsent(0));
        timer.print();

        System.out.println("Testing waitForCardPresent() timeout...");
        isFalse(terminal.waitForCardPresent(10));
        timer.print();
        isFalse(terminal.waitForCardPresent(100));
        timer.print();
        isFalse(terminal.waitForCardPresent(1000));
        timer.print();

        isFalse(terminal.isCardPresent());
        isFalse(terminal.isCardPresent());

        System.out.println("*** Insert card!");
        isTrue(terminal.waitForCardPresent(0));
        timer.print();

        isTrue(terminal.isCardPresent());
        isTrue(terminal.isCardPresent());

        System.out.println("Testing waitForCardPresent() with card already present...");
        isTrue(terminal.waitForCardPresent(0));
        timer.print();
        isTrue(terminal.waitForCardPresent(10000));
        timer.print();
        isTrue(terminal.waitForCardPresent(100));
        timer.print();
        isTrue(terminal.waitForCardPresent(10));
        timer.print();

        System.out.println("Testing waitForCardAbsent() timeout...");
        isFalse(terminal.waitForCardAbsent(1000));
        timer.print();
        isFalse(terminal.waitForCardAbsent(100));
        timer.print();
        isFalse(terminal.waitForCardAbsent(10));
        timer.print();

        System.out.println("*** Remove card!");
        isTrue(terminal.waitForCardAbsent(0));
        timer.print();

        isFalse(terminal.isCardPresent());
        isFalse(terminal.isCardPresent());

        System.out.println("OK.");
    }

}
