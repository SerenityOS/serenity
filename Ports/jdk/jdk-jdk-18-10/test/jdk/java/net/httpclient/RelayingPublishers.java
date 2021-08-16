/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

import jdk.test.lib.util.FileUtils;
import org.testng.annotations.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;

import static org.testng.Assert.assertEquals;

/*
 * @test
 * @summary Verifies that some of the standard BodyPublishers relay exception
 *          rather than throw it
 * @bug 8226303
 * @library /test/lib
 * @run testng/othervm RelayingPublishers
 */
public class RelayingPublishers {

    @Test
    public void ofFile0() throws IOException {
        Path directory = Files.createDirectory(Path.of("d"));
        // Even though the path exists, the publisher should not be able
        // to read from it, as that path denotes a directory, not a file
        BodyPublisher pub = BodyPublishers.ofFile(directory);
        CompletableSubscriber<ByteBuffer> s = new CompletableSubscriber<>();
        pub.subscribe(s);
        s.future().join();
        // Interestingly enough, it's FileNotFoundException if a file
        // is a directory
        assertEquals(s.future().join().getClass(), FileNotFoundException.class);
    }

    @Test
    public void ofFile1() throws IOException {
        Path file = Files.createFile(Path.of("f"));
        BodyPublisher pub = BodyPublishers.ofFile(file);
        FileUtils.deleteFileWithRetry(file);
        CompletableSubscriber<ByteBuffer> s = new CompletableSubscriber<>();
        pub.subscribe(s);
        assertEquals(s.future().join().getClass(), FileNotFoundException.class);
    }

    @Test
    public void ofByteArrays() {
        List<byte[]> bytes = new ArrayList<>();
        bytes.add(null);
        BodyPublisher pub = BodyPublishers.ofByteArrays(bytes);
        CompletableSubscriber<ByteBuffer> s = new CompletableSubscriber<>();
        pub.subscribe(s);
        assertEquals(s.future().join().getClass(), NullPointerException.class);
    }

    static class CompletableSubscriber<T> implements Flow.Subscriber<T> {

        final CompletableFuture<Throwable> f = new CompletableFuture<>();

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            subscription.request(1);
        }

        @Override
        public void onNext(T item) {
            f.completeExceptionally(new RuntimeException("Unexpected onNext"));
        }

        @Override
        public void onError(Throwable throwable) {
            f.complete(throwable);
        }

        @Override
        public void onComplete() {
            f.completeExceptionally(new RuntimeException("Unexpected onNext"));
        }

        CompletableFuture<Throwable> future() {
            return f.copy();
        }
    }
}
