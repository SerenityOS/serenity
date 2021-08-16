/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8265227 8266753
 * @summary Test Proc
 * @library /test/lib
 */

import jdk.test.lib.process.Proc;

import java.util.Random;
import java.util.List;

public class ProcTest {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            // Test launcher
            Proc p1 = Proc.create("ProcTest")
                    .args("1")
                    .debug("p1")
                    .start();
            Proc p2 = Proc.create("ProcTest")
                    .args("2")
                    .debug("p2")
                    .start();
            while (true) {
                String s1 = p1.readData(); // p1 shows to p2
                if (s1 != null) p2.println(s1);
                String s2 = p2.readData(); // p2 shows to p1
                if (s2 != null) p1.println(s2);
                if (s1 == null && s2 == null) break;
            }
            p1.waitFor();
            p2.waitFor();
        } else {
            // Sub process, args[0] is random seed.
            List<String> gestures = List.of("Rock", "Paper", "Scissors");
            int wins = 0;
            Random r = new Random(Long.parseLong(args[0]));
            while (true) {
                String my = gestures.get(r.nextInt(3));
                Proc.textOut(my); // show first, next line might block
                String peer = Proc.textIn();
                if (!my.equals(peer)) {
                    if (my.equals("Paper") && peer.equals("Rock")
                            || my.equals("Rock") && peer.equals("Scissors")
                            || my.equals("Scissors") && peer.equals("Paper")) {
                        wins++;
                    } else {
                        wins--;
                    }
                }
                // Message not from textOut() will be ignored by readData().
                System.out.println(my + " vs " + peer + ", I win " + wins + " times");
                if (wins > 2 || wins < -2) {
                    break;
                }
            }
        }
    }
}
