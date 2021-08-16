/*
 * Copyright (c) 2004, 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package sun.jvmstat.perfdata.monitor;

import java.util.*;

/**
 * Utility methods for use with {@link CountedTimerTask} instances.
 *
 * @author Brian Doherty
 * @since 1.5
 */
public class CountedTimerTaskUtils {

    // 8028357 removed old, inefficient debug logging

    /**
     * Reschedule a CountedTimeTask at a different interval. Probably not
     * named correctly. This method cancels the old task and computes the
     * delay for starting the new task based on the new interval and the
     * time at which the old task was last executed.
     *
     * @param timer the Timer for the task
     * @param oldTask the old Task
     * @param newTask the new Task
     * @param oldInterval the old interval; use for debugging output
     *                    purposes only.
     * @param newInterval scheduling interval in milliseconds
     */
    public static void reschedule(Timer timer, CountedTimerTask oldTask,
                                  CountedTimerTask newTask, int oldInterval,
                                  int newInterval) {

        long now = System.currentTimeMillis();
        long lastRun = oldTask.scheduledExecutionTime();
        long expired = now - lastRun;

        /*
         * check if original task ever ran - if not, then lastRun is
         * undefined and we simply set the delay to 0.
         */
        long delay = 0;
        if (oldTask.executionCount() > 0) {
            long remainder = newInterval - expired;
            delay = remainder >= 0 ? remainder : 0;
        }

        timer.schedule(newTask, delay, newInterval);
    }
}
