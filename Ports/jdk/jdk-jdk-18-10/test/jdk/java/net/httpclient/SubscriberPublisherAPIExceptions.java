/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.io.FileNotFoundException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.OpenOption;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.ResponseInfo;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpResponse.BodySubscriber;
import java.net.http.HttpResponse.BodySubscribers;
import java.util.function.Function;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.DELETE_ON_CLOSE;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.nio.file.StandardOpenOption.READ;
import static org.testng.Assert.assertThrows;

/*
 * @test
 * @summary Basic tests for API specified exceptions from Publisher, Handler,
 *          and Subscriber convenience static factory methods.
 * @run testng SubscriberPublisherAPIExceptions
 */

public class SubscriberPublisherAPIExceptions {

    static final Class<NullPointerException> NPE = NullPointerException.class;
    static final Class<IllegalArgumentException> IAE = IllegalArgumentException.class;
    static final Class<IndexOutOfBoundsException> IOB = IndexOutOfBoundsException.class;

    @Test
    public void publisherAPIExceptions() {
        assertThrows(NPE, () -> BodyPublishers.ofByteArray(null));
        assertThrows(NPE, () -> BodyPublishers.ofByteArray(null, 0, 1));
        assertThrows(IOB, () -> BodyPublishers.ofByteArray(new byte[100],    0, 101));
        assertThrows(IOB, () -> BodyPublishers.ofByteArray(new byte[100],    1, 100));
        assertThrows(IOB, () -> BodyPublishers.ofByteArray(new byte[100],   -1,  10));
        assertThrows(IOB, () -> BodyPublishers.ofByteArray(new byte[100],   99,   2));
        assertThrows(IOB, () -> BodyPublishers.ofByteArray(new byte[1],   -100,   1));
        assertThrows(NPE, () -> BodyPublishers.ofByteArray(null));
        assertThrows(NPE, () -> BodyPublishers.ofFile(null));
        assertThrows(NPE, () -> BodyPublishers.ofInputStream(null));
        assertThrows(NPE, () -> BodyPublishers.ofString(null));
        assertThrows(NPE, () -> BodyPublishers.ofString("A", null));
        assertThrows(NPE, () -> BodyPublishers.ofString(null, UTF_8));
        assertThrows(NPE, () -> BodyPublishers.ofString(null, null));
    }

    @DataProvider(name = "nonExistentFiles")
    public Object[][] nonExistentFiles() {
        List<Path> paths = List.of(Paths.get("doesNotExist"),
                                   Paths.get("tsixEtoNseod"),
                                   Paths.get("doesNotExist2"));
        paths.forEach(p -> {
            if (Files.exists(p))
                throw new AssertionError("Unexpected " + p);
        });

        return paths.stream().map(p -> new Object[] { p }).toArray(Object[][]::new);
    }

    @Test(dataProvider = "nonExistentFiles", expectedExceptions = FileNotFoundException.class)
    public void fromFileCheck(Path path) throws Exception {
        BodyPublishers.ofFile(path);
    }

    @Test
    public void handlerAPIExceptions() throws Exception {
        Path path = Paths.get(".").resolve("tt");
        Path file = Files.createFile(Paths.get(".").resolve("aFile"));
        Path doesNotExist = Paths.get(".").resolve("doneNotExist");
        if (Files.exists(doesNotExist))
            throw new AssertionError("Unexpected " + doesNotExist);

        assertThrows(NPE, () -> BodyHandlers.ofByteArrayConsumer(null));
        assertThrows(NPE, () -> BodyHandlers.ofFile(null));
        assertThrows(NPE, () -> BodyHandlers.ofFile(null, CREATE, WRITE));
        assertThrows(NPE, () -> BodyHandlers.ofFile(path, (OpenOption[])null));
        assertThrows(NPE, () -> BodyHandlers.ofFile(path, new OpenOption[] {null}));
        assertThrows(NPE, () -> BodyHandlers.ofFile(path, new OpenOption[] {CREATE, null}));
        assertThrows(NPE, () -> BodyHandlers.ofFile(path, new OpenOption[] {null, CREATE}));
        assertThrows(NPE, () -> BodyHandlers.ofFile(null, (OpenOption[])null));
        assertThrows(NPE, () -> BodyHandlers.ofFileDownload(null, CREATE, WRITE));
        assertThrows(NPE, () -> BodyHandlers.ofFileDownload(path, (OpenOption[])null));
        assertThrows(NPE, () -> BodyHandlers.ofFileDownload(path, new OpenOption[] {null}));
        assertThrows(NPE, () -> BodyHandlers.ofFileDownload(path, new OpenOption[] {CREATE, null}));
        assertThrows(NPE, () -> BodyHandlers.ofFileDownload(path, new OpenOption[] {null, CREATE}));
        assertThrows(NPE, () -> BodyHandlers.ofFileDownload(null, (OpenOption[])null));
        assertThrows(IAE, () -> BodyHandlers.ofFileDownload(file, CREATE, WRITE));
        assertThrows(IAE, () -> BodyHandlers.ofFileDownload(doesNotExist, CREATE, WRITE));
        assertThrows(NPE, () -> BodyHandlers.ofString(null));
        assertThrows(NPE, () -> BodyHandlers.buffering(null, 1));
        assertThrows(IAE, () -> BodyHandlers.buffering(new NoOpHandler(), 0));
        assertThrows(IAE, () -> BodyHandlers.buffering(new NoOpHandler(), -1));
        assertThrows(IAE, () -> BodyHandlers.buffering(new NoOpHandler(), Integer.MIN_VALUE));

        // implementation specific exceptions
        assertThrows(IAE, () -> BodyHandlers.ofFile(path, READ));
        assertThrows(IAE, () -> BodyHandlers.ofFile(path, DELETE_ON_CLOSE));
        assertThrows(IAE, () -> BodyHandlers.ofFile(path, READ, DELETE_ON_CLOSE));
        assertThrows(IAE, () -> BodyHandlers.ofFileDownload(path, DELETE_ON_CLOSE));
    }

    @Test
    public void subscriberAPIExceptions() {
        Path path = Paths.get(".").resolve("tt");
        assertThrows(NPE, () -> BodySubscribers.ofByteArrayConsumer(null));
        assertThrows(NPE, () -> BodySubscribers.ofFile(null));
        assertThrows(NPE, () -> BodySubscribers.ofFile(null, CREATE, WRITE));
        assertThrows(NPE, () -> BodySubscribers.ofFile(path, (OpenOption[])null));
        assertThrows(NPE, () -> BodySubscribers.ofFile(path, new OpenOption[] {null}));
        assertThrows(NPE, () -> BodySubscribers.ofFile(path, new OpenOption[] {CREATE, null}));
        assertThrows(NPE, () -> BodySubscribers.ofFile(path, new OpenOption[] {null, CREATE}));
        assertThrows(NPE, () -> BodySubscribers.ofFile(null, (OpenOption[])null));
        assertThrows(NPE, () -> BodySubscribers.ofString(null));
        assertThrows(NPE, () -> BodySubscribers.buffering(null, 1));
        assertThrows(IAE, () -> BodySubscribers.buffering(new NoOpSubscriber(), 0));
        assertThrows(IAE, () -> BodySubscribers.buffering(new NoOpSubscriber(), -1));
        assertThrows(IAE, () -> BodySubscribers.buffering(new NoOpSubscriber(), Integer.MIN_VALUE));
        assertThrows(NPE, () -> BodySubscribers.mapping(null, Function.identity()));
        assertThrows(NPE, () -> BodySubscribers.mapping(BodySubscribers.ofByteArray(), null));
        assertThrows(NPE, () -> BodySubscribers.mapping(null, null));

        // implementation specific exceptions
        assertThrows(IAE, () -> BodySubscribers.ofFile(path, READ));
        assertThrows(IAE, () -> BodySubscribers.ofFile(path, DELETE_ON_CLOSE));
        assertThrows(IAE, () -> BodySubscribers.ofFile(path, READ, DELETE_ON_CLOSE));
    }

    static class NoOpHandler implements BodyHandler<Void> {
        @Override public BodySubscriber<Void> apply(ResponseInfo rinfo) { return null; }
    }

    static class NoOpSubscriber implements BodySubscriber<Void> {
        @Override public void onSubscribe(Flow.Subscription subscription) { }
        @Override public void onNext(List<ByteBuffer> item) { }
        @Override public void onError(Throwable throwable) { }
        @Override public void onComplete() { }
        @Override public CompletableFuture<Void> getBody() { return null; }
    }
}
