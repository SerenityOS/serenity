/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

/**
 * A logger designed specifically to allow collecting ordered log messages
 * in a multi-threaded environment without involving any kind of locking.
 * <p>
 * It is particularly useful in situations when one needs to assert various
 * details about the tested thread state or the locks it hold while also wanting
 * to produce diagnostic log messages.
 */
public class LockFreeLogger {
    /**
     * ConcurrentLinkedQueue implements non-blocking algorithm.
     */
    private final Queue<String> records = new ConcurrentLinkedQueue<>();

    public LockFreeLogger() {
    }

    /**
     * Logs a message.
     * @param format Message format
     * @param params Message parameters
     */
    public void log(String format, Object ... params) {
        records.add(String.format(format, params));
    }

    /**
     * Generates an aggregated log of chronologically ordered messages.
     *
     * @return An aggregated log of chronologically ordered messages
     */
    @Override
    public String toString() {
        return records.stream().collect(Collectors.joining());
    }
}
