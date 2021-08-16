/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6241823
 * @summary Repeated timed tryAcquire shouldn't hang.
 */

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class TimedAcquire {
    public static void main(String[] args) throws Exception {
        for (Semaphore sem : new Semaphore[]{ new Semaphore(0),
                                              new Semaphore(0, false),
                                              new Semaphore(0, true)})
            for (int delay : new int[] {0, 1})
                for (int i = 0; i < 3; i++)
                    if (sem.tryAcquire(delay, TimeUnit.MILLISECONDS))
                        throw new Error("Acquired Semaphore with no permits!");
        System.out.println("Done!");
    }
}
