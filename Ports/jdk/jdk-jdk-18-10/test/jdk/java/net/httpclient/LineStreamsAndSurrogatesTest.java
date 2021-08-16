/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.http.HttpResponse.BodySubscriber;
import java.net.http.HttpResponse.BodySubscribers;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.testng.annotations.Test;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.charset.StandardCharsets.UTF_16;
import static org.testng.Assert.assertEquals;

/*
 * @test
 * @summary tests for BodySubscribers returned by asLines.
 *       In particular tests that surrogate characters are handled
 *       correctly.
 * @modules java.net.http java.logging
 * @run testng/othervm LineStreamsAndSurrogatesTest
 */

public class LineStreamsAndSurrogatesTest {


    static final Class<NullPointerException> NPE = NullPointerException.class;

    private static final List<String> lines(String text) {
        return new BufferedReader(new StringReader(text)).lines().collect(Collectors.toList());
    }

    @Test
    public void testUncomplete() throws Exception {
        // Uses U+10400 which is encoded as the surrogate pair U+D801 U+DC00
        String text = "Bient\u00f4t\r\n nous plongerons\r\n dans\r les\n\n" +
                " fr\u00f4\ud801\udc00des\r\n t\u00e9n\u00e8bres\ud801\udc00";
        Charset charset = UTF_8;

        BodySubscriber<Stream<String>> bodySubscriber = BodySubscribers.ofLines(charset);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        Runnable run = () -> {
            try {
                SubmissionPublisher<List<ByteBuffer>> publisher = new SubmissionPublisher<>();
                byte[] sbytes = text.getBytes(charset);
                byte[] bytes = Arrays.copyOfRange(sbytes, 0, sbytes.length - 1);
                publisher.subscribe(bodySubscriber);
                System.out.println("Publishing " + bytes.length + " bytes");
                for (int i = 0; i < bytes.length; i++) {
                    // ensure that surrogates are split over several buffers.
                    publisher.submit(List.of(ByteBuffer.wrap(bytes, i, 1)));
                }
                publisher.close();
            } catch(Throwable t) {
                errorRef.set(t);
            }
        };
        Thread thread = new Thread(run,"Publishing");
        thread.start();
        try {
            Stream<String> stream = bodySubscriber.getBody().toCompletableFuture().get();
            List<String> list = stream.collect(Collectors.toList());
            String resp = list.stream().collect(Collectors.joining(""));
            System.out.println("***** Got: " + resp);

            byte[] sbytes = text.getBytes(UTF_8);
            byte[] bytes = Arrays.copyOfRange(sbytes, 0, sbytes.length - 1);
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            BufferedReader reader = new BufferedReader(new InputStreamReader(bais, charset));
            String resp2 = reader.lines().collect(Collectors.joining(""));
            System.out.println("***** Got2: " + resp2);

            assertEquals(resp, resp2);
            assertEquals(list, List.of("Bient\u00f4t",
                                       " nous plongerons",
                                       " dans",
                                       " les",
                                       "",
                                       " fr\u00f4\ud801\udc00des",
                                       " t\u00e9n\u00e8bres\ufffd"));
        } catch (ExecutionException x) {
            Throwable cause = x.getCause();
            if (cause instanceof MalformedInputException) {
                throw new RuntimeException("Unexpected MalformedInputException", cause);
            }
            throw x;
        }
        if (errorRef.get() != null) {
            throw new RuntimeException("Unexpected exception", errorRef.get());
        }
    }

    @Test
    public void testStream1() throws Exception {
        // Uses U+10400 which is encoded as the surrogate pair U+D801 U+DC00
        String text = "Bient\u00f4t\r\n nous plongerons\r\n dans\r\r les\n\n" +
                " fr\u00f4\ud801\udc00des\r\n t\u00e9n\u00e8bres";
        Charset charset = UTF_8;

        BodySubscriber<Stream<String>> bodySubscriber = BodySubscribers.ofLines(charset);
        SubmissionPublisher<List<ByteBuffer>> publisher = new SubmissionPublisher<>();
        byte[] bytes = text.getBytes(charset);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        Runnable run = () -> {
            try {
                publisher.subscribe(bodySubscriber);
                System.out.println("Publishing " + bytes.length + " bytes");
                for (int i = 0; i < bytes.length; i++) {
                    // ensure that surrogates are split over several buffers.
                    publisher.submit(List.of(ByteBuffer.wrap(bytes, i, 1)));
                }
                publisher.close();
            } catch(Throwable t) {
                errorRef.set(t);
            }
        };

        Stream<String> stream = bodySubscriber.getBody().toCompletableFuture().get();
        Thread thread = new Thread(run,"Publishing");
        thread.start();
        List<String> list = stream.collect(Collectors.toList());
        String resp = list.stream().collect(Collectors.joining("|"));
        System.out.println("***** Got: " + resp);
        assertEquals(resp, text.replace("\r\n", "|")
                               .replace("\n","|")
                               .replace("\r","|"));
        assertEquals(list, List.of("Bient\u00f4t",
                " nous plongerons",
                " dans",
                "",
                " les",
                "",
                " fr\u00f4\ud801\udc00des",
                " t\u00e9n\u00e8bres"));
        assertEquals(list, lines(text));
        if (errorRef.get() != null) {
            throw new RuntimeException("Unexpected exception", errorRef.get());
        }
    }


    @Test
    public void testStream2() throws Exception {
        String text = "Bient\u00f4t\r\n nous plongerons\r\n dans\r\r" +
                " les fr\u00f4\ud801\udc00des\r\n t\u00e9n\u00e8bres\r\r";
        Charset charset = UTF_8;

        BodySubscriber<Stream<String>> bodySubscriber = BodySubscribers.ofLines(charset);
        SubmissionPublisher<List<ByteBuffer>> publisher = new SubmissionPublisher<>();
        byte[] bytes = text.getBytes(charset);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        Runnable run = () -> {
            try {
                publisher.subscribe(bodySubscriber);
                System.out.println("Publishing " + bytes.length + " bytes");
                for (int i = 0; i < bytes.length; i++) {
                    // ensure that surrogates are split over several buffers.
                    publisher.submit(List.of(ByteBuffer.wrap(bytes, i, 1)));
                }
                publisher.close();
            } catch(Throwable t) {
                errorRef.set(t);
            }
        };

        Stream<String> stream = bodySubscriber.getBody().toCompletableFuture().get();
        Thread thread = new Thread(run,"Publishing");
        thread.start();
        List<String> list = stream.collect(Collectors.toList());
        String resp = list.stream().collect(Collectors.joining(""));
        System.out.println("***** Got: " + resp);
        String expected = Stream.of(text.split("\r\n|\r|\n"))
                .collect(Collectors.joining(""));
        assertEquals(resp, expected);
        assertEquals(list, List.of("Bient\u00f4t",
                " nous plongerons",
                " dans",
                "",
                " les fr\u00f4\ud801\udc00des",
                " t\u00e9n\u00e8bres",
                ""));
        assertEquals(list, lines(text));
        if (errorRef.get() != null) {
            throw new RuntimeException("Unexpected exception", errorRef.get());
        }
    }

    @Test
    public void testStream3_UTF16() throws Exception {
        // Uses U+10400 which is encoded as the surrogate pair U+D801 U+DC00
        String text = "Bient\u00f4t\r\n nous plongerons\r\n dans\r\r" +
                " les\n\n fr\u00f4\ud801\udc00des\r\n t\u00e9n\u00e8bres";
        Charset charset = UTF_16;

        BodySubscriber<Stream<String>> bodySubscriber = BodySubscribers.ofLines(charset);
        SubmissionPublisher<List<ByteBuffer>> publisher = new SubmissionPublisher<>();
        byte[] bytes = text.getBytes(charset);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        Runnable run = () -> {
            try {
                publisher.subscribe(bodySubscriber);
                System.out.println("Publishing " + bytes.length + " bytes");
                for (int i = 0; i < bytes.length; i++) {
                    // ensure that surrogates are split over several buffers.
                    publisher.submit(List.of(ByteBuffer.wrap(bytes, i, 1)));
                }
                publisher.close();
            } catch(Throwable t) {
                errorRef.set(t);
            }
        };

        Stream<String> stream = bodySubscriber.getBody().toCompletableFuture().get();
        Thread thread = new Thread(run,"Publishing");
        thread.start();
        List<String> list = stream.collect(Collectors.toList());
        String resp = list.stream().collect(Collectors.joining(""));
        System.out.println("***** Got: " + resp);
        assertEquals(resp, text.replace("\n","").replace("\r",""));
        assertEquals(list, List.of("Bient\u00f4t",
                " nous plongerons",
                " dans",
                "",
                " les",
                "",
                " fr\u00f4\ud801\udc00des",
                " t\u00e9n\u00e8bres"));
        assertEquals(list, lines(text));
        if (errorRef.get() != null) {
            throw new RuntimeException("Unexpected exception", errorRef.get());
        }
    }


    @Test
    public void testStream4_UTF16() throws Exception {
        String text = "Bient\u00f4t\r\n nous plongerons\r\n dans\r\r" +
                " les fr\u00f4\ud801\udc00des\r\n t\u00e9n\u00e8bres\r\r";
        Charset charset = UTF_16;

        BodySubscriber<Stream<String>> bodySubscriber = BodySubscribers.ofLines(charset);
        SubmissionPublisher<List<ByteBuffer>> publisher = new SubmissionPublisher<>();
        byte[] bytes = text.getBytes(charset);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        Runnable run = () -> {
            try {
                publisher.subscribe(bodySubscriber);
                System.out.println("Publishing " + bytes.length + " bytes");
                for (int i = 0; i < bytes.length; i++) {
                    // ensure that surrogates are split over several buffers.
                    publisher.submit(List.of(ByteBuffer.wrap(bytes, i, 1)));
                }
                publisher.close();
            } catch(Throwable t) {
                errorRef.set(t);
            }
        };

        Stream<String> stream = bodySubscriber.getBody().toCompletableFuture().get();
        Thread thread = new Thread(run,"Publishing");
        thread.start();
        List<String> list = stream.collect(Collectors.toList());
        String resp = list.stream().collect(Collectors.joining(""));
        System.out.println("***** Got: " + resp);
        String expected = Stream.of(text.split("\r\n|\r|\n"))
                .collect(Collectors.joining(""));
        assertEquals(resp, expected);
        assertEquals(list, List.of("Bient\u00f4t",
                " nous plongerons",
                " dans",
                "",
                " les fr\u00f4\ud801\udc00des",
                " t\u00e9n\u00e8bres",
                ""));
        assertEquals(list, lines(text));
        if (errorRef.get() != null) {
            throw new RuntimeException("Unexpected exception", errorRef.get());
        }
    }

}
