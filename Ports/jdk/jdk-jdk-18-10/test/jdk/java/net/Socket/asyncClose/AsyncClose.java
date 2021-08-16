/*
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.util.concurrent.CompletableFuture;
import jdk.test.lib.net.IPSupport;
import static java.util.concurrent.CompletableFuture.*;

/*
 * @test
 * @bug 4344135
 * @library /test/lib
 * @summary Check that {Socket,ServerSocket,DatagramSocket}.close will
 *          cause any thread blocked on the socket to throw a SocketException.
 * @run main AsyncClose
 * @run main/othervm -Djava.net.preferIPv4Stack=true AsyncClose
 */

public class AsyncClose {

    public static void main(String args[]) throws Exception {
        IPSupport.throwSkippedExceptionIfNonOperational();

        AsyncCloseTest tests[] = {
            new Socket_getInputStream_read(),
            new Socket_getInputStream_read(20000),
            new Socket_getOutputStream_write(),
            new DatagramSocket_receive(),
            new DatagramSocket_receive(20000),
            new ServerSocket_accept(),
            new ServerSocket_accept(20000),
        };

        int failures = 0;

        List<CompletableFuture<AsyncCloseTest>> cfs = new ArrayList<>();
        for (AsyncCloseTest test : tests)
            cfs.add( supplyAsync(() -> test.go()));

        for (CompletableFuture<AsyncCloseTest> cf : cfs) {
            AsyncCloseTest test = cf.get();

            System.out.println("******************************");
            System.out.println("Test: " + test.description());
            if (test.hasPassed()) {
                System.out.println("Passed.");
            } else {
                System.out.println("Failed: " + test.failureReason());
                failures++;
            }
            System.out.println("");
        }

        if (failures > 0)
            throw new Exception(failures + " sub-tests failed - see log.");
    }
}
