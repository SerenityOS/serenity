/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Bug8066652 {

    public static void main(String args[]) {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        Callable<TimeZone> calTimeZone = () -> TimeZone.getDefault();
        List<Callable<TimeZone>> tasks = new ArrayList<>();
        for (int j = 1; j < 10; j++) {
            tasks.add(calTimeZone);
        }
        try {
            List<Future<TimeZone>> results = executor.invokeAll(tasks);
            for (Future<TimeZone> f : results) {
                TimeZone tz = f.get();
                if (! tz.getID().equals("GMT")) {
                    throw new RuntimeException("wrong Time zone ID: " + tz.getID()
                            + ", It should be GMT");
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Execution interrupted or Execution Exception occurred", e);
        } finally {
            executor.shutdown();
        }
    }
}
