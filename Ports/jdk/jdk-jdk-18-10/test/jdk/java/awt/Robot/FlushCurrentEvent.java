/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.EventQueue;
import java.awt.Robot;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @test
 * @key headful
 * @bug 8196100
 * @summary Checks that current event is flushed by the Robot.waitForIdle()
 */
public final class FlushCurrentEvent {

    public static void main(String[] args) throws Exception {
        Robot robot = new Robot();
        AtomicBoolean done = new AtomicBoolean();
        EventQueue.invokeLater(() -> {
            robot.delay(15000);
            done.set(true);
        });
        robot.waitForIdle();
        if (!done.get()) {
            throw new RuntimeException("Current event was not flushed");
        }
    }
}
