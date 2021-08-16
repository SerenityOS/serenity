/*
 * Copyright (c) 2002, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4369826 8078582 8190884
 * @run main/othervm LotsOfOutput
 * @summary Process with lots of output should not crash VM
 * @author kladko
 */

public class LotsOfOutput {
    static final Runtime runtime = Runtime.getRuntime();

    // Allow memory to grow by up to 1Mb total
    static final int THRESHOLD = 1048576;

    // Compute used memory
    static long usedMemory() {
        return runtime.totalMemory() - runtime.freeMemory();
    }

    public static void main(String[] args) throws Exception {
        if (! UnixCommands.isUnix) {
            System.out.println("For UNIX only");
            return;
        }
        UnixCommands.ensureCommandsAvailable("cat");

        Process p = runtime.exec(UnixCommands.cat() + " /dev/zero");
        long prev = usedMemory();
        int growing = 0;
        for (int i = 1; i < 10; i++) {
            Thread.sleep(100);
            long used = usedMemory();
            if (used != prev) {
                System.out.printf("consuming memory: i: %d, prev: %d, used: %d, delta: %d%n",
                        i, prev, used, used - prev);
            }
            if (used > prev + THRESHOLD)
                growing += 1;
            prev = used;
        }
        if (growing > 2)
            throw new Exception("Process consumes memory: growing " + growing);

    }
}
