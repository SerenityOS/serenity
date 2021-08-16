/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.share.runner;

import nsk.share.test.ExecutionController;

/**
 * Interface that defines a way to run several tasks.
 *
 * Several implementations are provided.
 *
 * @see nsk.share.runner.ThreadsRunner
 */
public interface MultiRunner extends Runnable {
        /**
         * Add a task.
         *
         * @param runnable task
         */
        public void add(Runnable runnable);

        /**
         * Remove a task.
         *
         * @param runnable task
         */
        public void remove(Runnable runnable);

        /**
         * Remove all tasks.
         */
        public void removeAll();

        /**
         * Run tasks.
         */
        public void run();

        /**
         * Get status of run.
         *
         * @return true if everything is alright, false if there are any errors
         */
        public boolean isSuccessful();

        /**
         * Get execution controller for current thread.
         *
         * @returns execution controller for current thread
         */
        public ExecutionController getExecutionController();
}
