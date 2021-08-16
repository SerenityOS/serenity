/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4708197 6497629
 * @summary Test for max priority setting that matches spec
 * @author Pete Soper
 */

public class SetMaxPriority {

    public static void main(String args[]) throws Exception {
        ThreadGroup tg = new ThreadGroup("foo");
        ThreadGroup ptg = tg.getParent();
        int currentMaxPriority = tg.getMaxPriority();
        int halfMaxPriority = ptg.getMaxPriority() / 2;
        if (halfMaxPriority - Thread.MIN_PRIORITY < 2) {
            throw new RuntimeException("SetMaxPriority test no longer valid: starting parent max priority too close to Thread.MIN_PRIORITY");
        }
        tg.setMaxPriority(halfMaxPriority - 2);
        currentMaxPriority = tg.getMaxPriority();
        if (currentMaxPriority != halfMaxPriority - 2) {
            throw new RuntimeException("SetMaxPriority failed: max priority not changed");
        }

        // This will fail if bug 6497629 is present because the min tests is
        // being made with the (just lowered) max instead of the parent max,
        // preventing the priority from being moved back up.
        tg.setMaxPriority(currentMaxPriority + 1);
        int newMaxPriority = tg.getMaxPriority();
        if (newMaxPriority != currentMaxPriority + 1) {
            throw new RuntimeException("SetMaxPriority failed: defect 6497629 present");
        }

        // Confirm that max priorities out of range on both ends have no
        // effect.
        for (int badPriority : new int[] {Thread.MIN_PRIORITY - 1,
                                          Thread.MAX_PRIORITY + 1}) {
            int oldPriority = tg.getMaxPriority();
            tg.setMaxPriority(badPriority);
            if (oldPriority != tg.getMaxPriority())
                throw new RuntimeException(
                    "setMaxPriority bad arg not ignored as specified");
        }

        System.out.println("SetMaxPriority passed");
    }
}
