/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

package jdk.test.lib;

import java.util.Objects;

/**
 * Class which runs another Runnable in infinite loop with certain pauses
 * between cycles.
 */
public class InfiniteLoop implements Runnable {
    private final Runnable target;
    private final long mills;


    /**
     * @param target a target to run in a loop
     * @param mills  the length of pause time in milliseconds
     * @throws NullPointerException if target is null
     * @throws IllegalArgumentException if the value of millis is negative
     */
    public InfiniteLoop(Runnable target, long mills) {
        Objects.requireNonNull(target);
        if (mills < 0) {
            throw new IllegalArgumentException("mills < 0");
        }
        this.target = target;
        this.mills = mills;
    }

    @Override
    public void run() {
        try {
            while (true) {
                target.run();
                if (mills > 0) {
                    Thread.sleep(mills);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new Error(e);
        }
    }
}
