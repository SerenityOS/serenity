/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8143394
 * @summary Check for NullPointerException in race between read() and close().
 */
import java.io.CharArrayReader;
import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class ReadCloseRaceNPE {

    private static final int BUF_SIZE = 1000;
    private static final long TIMEOUT_MS = 3000;

    private static final List<Exception> failures = new ArrayList<>();

    private static void testReader(final Supplier<Reader> readerSupplier)
            throws InterruptedException {
        AtomicReference<Reader> readerRef =
            new AtomicReference<>(readerSupplier.get());

        AtomicBoolean isFinished = new AtomicBoolean();

        Runnable readTask = () -> {
            long startTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime < TIMEOUT_MS) {
                try {
                    readerRef.get().read();
                } catch (Exception e) {
                    if (!(e instanceof IOException)) {
                        failures.add(e);
                        break;
                    }
                    readerRef.set(readerSupplier.get());
                }
            }
            isFinished.set(true);
        };

        Runnable closeTask = () -> {
            while (!isFinished.get()) {
                try {
                    readerRef.get().close();
                } catch (Exception e) {
                    if (!(e instanceof IOException)) {
                        e.printStackTrace();
                    }
                }
            }
        };

        Thread readThread = new Thread(readTask);
        Thread closeThread = new Thread(closeTask);

        readThread.start();
        closeThread.start();
        readThread.join();
        closeThread.join();
    }

    public static void main(String[] args) throws Throwable {
        final String s = "Two riders were approaching.\\n";

        Supplier<Reader> charPushbackReaderSupplier = () -> {
            char buf[] = new char[s.length()];
            s.getChars(0, s.length(), buf, 0);
            CharArrayReader in = new CharArrayReader(buf);
            return new PushbackReader(in, BUF_SIZE);
        };

        testReader(charPushbackReaderSupplier);

        Supplier<Reader> stringPushbackReaderSupplier = () -> {
            StringReader in = new StringReader(s);
            return new PushbackReader(in, BUF_SIZE);
        };

        testReader(stringPushbackReaderSupplier);

        if (!failures.isEmpty()) {
            failures.stream().forEach((x) -> ((Exception) x).printStackTrace());
            throw new RuntimeException("PushbackReaderNPE failed");
        }
    }
}
