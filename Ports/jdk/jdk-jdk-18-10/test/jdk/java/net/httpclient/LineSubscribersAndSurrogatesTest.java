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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.net.http.HttpResponse.BodySubscriber;
import java.net.http.HttpResponse.BodySubscribers;
import java.nio.ByteBuffer;
import java.nio.charset.MalformedInputException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.testng.annotations.Test;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.charset.StandardCharsets.UTF_16;
import static org.testng.Assert.assertEquals;

/*
 * @test
 * @summary tests for BodySubscribers returned by fromLineSubscriber.
 *       In particular tests that surrogate characters are handled
 *       correctly.
 * @modules java.net.http java.logging
 * @run testng/othervm LineSubscribersAndSurrogatesTest
 */

public class LineSubscribersAndSurrogatesTest {


    static final Class<NullPointerException> NPE = NullPointerException.class;

    private static final List<String> lines(String text, String eol) {
        if (eol == null) {
            return new BufferedReader(new StringReader(text)).lines().collect(Collectors.toList());
        } else {
            String replaced = text.replace(eol, "|");
            int i=0;
            while(replaced.endsWith("||")) {
                replaced = replaced.substring(0,replaced.length()-1);
                i++;
            }
            List<String> res = List.of(replaced.split("\\|"));
            if (i > 0) {
                res = new ArrayList<>(res);
                for (int j=0; j<i; j++) res.add("");
            }
            return res;
        }
    }

    @Test
    public void testIncomplete() throws Exception {
        // Uses U+10400 which is encoded as the surrogate pair U+D801 U+DC00
        String text = "Bient\u00f4t\r\n nous plongerons\r\n dans\r" +
                " les\n\n fr\u00f4\ud801\udc00des\r\n t\u00e9n\u00e8bres\ud801\udc00";
        ObjectSubscriber subscriber = new ObjectSubscriber();
        BodySubscriber<String> bodySubscriber = BodySubscribers.fromLineSubscriber(
                subscriber, Supplier::get, UTF_8, null);
        SubmissionPublisher<List<ByteBuffer>> publisher = new SubmissionPublisher<>();
        byte[] sbytes = text.getBytes(UTF_8);
        byte[] bytes = Arrays.copyOfRange(sbytes,0, sbytes.length - 1);
        publisher.subscribe(bodySubscriber);
        System.out.println("Publishing " + bytes.length + " bytes");
        for (int i=0; i<bytes.length; i++) {
            // ensure that surrogates are split over several buffers.
            publisher.submit(List.of(ByteBuffer.wrap(bytes, i, 1)));
        }
        publisher.close();
        try {
            String resp = bodySubscriber.getBody().toCompletableFuture().get();
            System.out.println("***** Got: " + resp);
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            BufferedReader reader = new BufferedReader(new InputStreamReader(bais, UTF_8));
            String resp2 = reader.lines().collect(Collectors.joining(""));
            assertEquals(resp, resp2);
            assertEquals(subscriber.list, List.of("Bient\u00f4t",
                    " nous plongerons",
                    " dans",
                    " les",
                    "",
                    " fr\u00f4\ud801\udc00des",
                    " t\u00e9n\u00e8bres\ufffd"));
        } catch (ExecutionException x) {
            Throwable cause = x.getCause();
            if (cause instanceof MalformedInputException) {
                throw new RuntimeException("Unexpected MalformedInputException thrown", cause);
            }
            throw x;
        }
    }


    @Test
    public void testStringWithFinisherLF() throws Exception {
        // Uses U+10400 which is encoded as the surrogate pair U+D801 U+DC00
        String text = "Bient\u00f4t\r\n nous plongerons\r\n dans\r" +
                " les\n\n fr\u00f4\ud801\udc00des\r\n t\u00e9n\u00e8bres\r";
        ObjectSubscriber subscriber = new ObjectSubscriber();
        BodySubscriber<String> bodySubscriber = BodySubscribers.fromLineSubscriber(
                subscriber, Supplier::get, UTF_8, "\n");
        SubmissionPublisher<List<ByteBuffer>> publisher = new SubmissionPublisher<>();
        byte[] bytes = text.getBytes(UTF_8);
        publisher.subscribe(bodySubscriber);
        System.out.println("Publishing " + bytes.length + " bytes");
        for (int i=0; i<bytes.length; i++) {
            // ensure that surrogates are split over several buffers.
            publisher.submit(List.of(ByteBuffer.wrap(bytes, i, 1)));
        }
        publisher.close();
        String resp = bodySubscriber.getBody().toCompletableFuture().get();
        System.out.println("***** Got: " + resp);
        List<String> expected = List.of("Bient\u00f4t\r",
                " nous plongerons\r",
                " dans\r les",
                "",
                " fr\u00f4\ud801\udc00des\r",
                " t\u00e9n\u00e8bres\r");
        assertEquals(subscriber.list, expected);
        assertEquals(resp, Stream.of(text.split("\n")).collect(Collectors.joining("")));
        assertEquals(resp, expected.stream().collect(Collectors.joining("")));
        assertEquals(subscriber.list, lines(text, "\n"));
    }


    @Test
    public void testStringWithFinisherCR() throws Exception {
        String text = "Bient\u00f4t\r\n nous plongerons\r\n dans\r" +
                " les fr\u00f4\ud801\udc00des\r\n t\u00e9n\u00e8bres\r\r";
        ObjectSubscriber subscriber = new ObjectSubscriber();
        BodySubscriber<String> bodySubscriber = BodySubscribers.fromLineSubscriber(
                subscriber, Supplier::get, UTF_8, "\r");
        SubmissionPublisher<List<ByteBuffer>> publisher = new SubmissionPublisher<>();
        byte[] bytes = text.getBytes(UTF_8);
        publisher.subscribe(bodySubscriber);
        System.out.println("Publishing " + bytes.length + " bytes");
        for (int i=0; i<bytes.length; i++) {
            // ensure that surrogates are split over several buffers.
            publisher.submit(List.of(ByteBuffer.wrap(bytes, i, 1)));
        }
        publisher.close();
        String resp = bodySubscriber.getBody().toCompletableFuture().get();
        System.out.println("***** Got: " + resp);
        assertEquals(resp, text.replace("\r", ""));
        assertEquals(subscriber.list, List.of("Bient\u00f4t",
                "\n nous plongerons",
                "\n dans",
                " les fr\u00f4\ud801\udc00des",
                "\n t\u00e9n\u00e8bres",
                ""));
        assertEquals(subscriber.list, lines(text, "\r"));
    }

    @Test
    public void testStringWithFinisherCRLF() throws Exception {
        String text = "Bient\u00f4t\r\n nous plongerons\r\n dans\r" +
                " les fr\u00f4\ud801\udc00des\r\n t\u00e9n\u00e8bres";
        ObjectSubscriber subscriber = new ObjectSubscriber();
        BodySubscriber<String> bodySubscriber = BodySubscribers.fromLineSubscriber(
                subscriber, Supplier::get, UTF_8, "\r\n");
        SubmissionPublisher<List<ByteBuffer>> publisher = new SubmissionPublisher<>();
        byte[] bytes = text.getBytes(UTF_8);
        publisher.subscribe(bodySubscriber);
        System.out.println("Publishing " + bytes.length + " bytes");
        for (int i=0; i<bytes.length; i++) {
            // ensure that surrogates are split over several buffers.
            publisher.submit(List.of(ByteBuffer.wrap(bytes, i, 1)));
        }
        publisher.close();
        String resp = bodySubscriber.getBody().toCompletableFuture().get();
        System.out.println("***** Got: " + resp);
        assertEquals(resp, text.replace("\r\n",""));
        assertEquals(subscriber.list, List.of("Bient\u00f4t",
                " nous plongerons",
                " dans\r les fr\u00f4\ud801\udc00des",
                " t\u00e9n\u00e8bres"));
        assertEquals(subscriber.list, lines(text, "\r\n"));
    }


    @Test
    public void testStringWithFinisherBR() throws Exception {
        String text = "Bient\u00f4t\r\n nous plongerons\r\n dans\r" +
                " les\r\r fr\u00f4\ud801\udc00des\r\n t\u00e9n\u00e8bres";
        ObjectSubscriber subscriber = new ObjectSubscriber();
        BodySubscriber<String> bodySubscriber = BodySubscribers.fromLineSubscriber(
                subscriber, Supplier::get, UTF_8, null);
        SubmissionPublisher<List<ByteBuffer>> publisher = new SubmissionPublisher<>();
        byte[] bytes = text.getBytes(UTF_8);
        publisher.subscribe(bodySubscriber);
        System.out.println("Publishing " + bytes.length + " bytes");
        for (int i=0; i<bytes.length; i++) {
            // ensure that surrogates are split over several buffers.
            publisher.submit(List.of(ByteBuffer.wrap(bytes, i, 1)));
        }
        publisher.close();
        String resp = bodySubscriber.getBody().toCompletableFuture().get();
        System.out.println("***** Got: " + resp);
        List<String> expected = List.of("Bient\u00f4t",
                " nous plongerons",
                " dans",
                " les",
                "",
                " fr\u00f4\ud801\udc00des",
                " t\u00e9n\u00e8bres");
        assertEquals(subscriber.list, expected);
        assertEquals(resp, expected.stream().collect(Collectors.joining("")));
        assertEquals(subscriber.list, lines(text, null));
    }

    @Test
    public void testStringWithFinisherBR_UTF_16() throws Exception {
        String text = "Bient\u00f4t\r\n nous plongerons\r\n dans\r" +
                " les\r\r fr\u00f4\ud801\udc00des\r\n t\u00e9n\u00e8bres\r\r";
        ObjectSubscriber subscriber = new ObjectSubscriber();
        BodySubscriber<String> bodySubscriber = BodySubscribers.fromLineSubscriber(
                subscriber, Supplier::get, UTF_16, null);
        SubmissionPublisher<List<ByteBuffer>> publisher = new SubmissionPublisher<>();
        byte[] bytes = text.getBytes(UTF_16);
        publisher.subscribe(bodySubscriber);
        System.out.println("Publishing " + bytes.length + " bytes");
        for (int i=0; i<bytes.length; i++) {
            // ensure that surrogates are split over several buffers.
            publisher.submit(List.of(ByteBuffer.wrap(bytes, i, 1)));
        }
        publisher.close();
        String resp = bodySubscriber.getBody().toCompletableFuture().get();
        System.out.println("***** Got: " + resp);
        List<String> expected = List.of("Bient\u00f4t",
                " nous plongerons",
                " dans",
                " les",
                "",
                " fr\u00f4\ud801\udc00des",
                " t\u00e9n\u00e8bres",
                "");
        assertEquals(resp, expected.stream().collect(Collectors.joining("")));
        assertEquals(subscriber.list, expected);
        assertEquals(subscriber.list, lines(text, null));
    }

    void testStringWithoutFinisherBR() throws Exception {
        String text = "Bient\u00f4t\r\n nous plongerons\r\n dans\r" +
                " les\r\r fr\u00f4\ud801\udc00des\r\n t\u00e9n\u00e8bres";
        ObjectSubscriber subscriber = new ObjectSubscriber();
        BodySubscriber<Void> bodySubscriber = BodySubscribers.fromLineSubscriber(subscriber);
        SubmissionPublisher<List<ByteBuffer>> publisher = new SubmissionPublisher<>();
        byte[] bytes = text.getBytes(UTF_8);
        publisher.subscribe(bodySubscriber);
        System.out.println("Publishing " + bytes.length + " bytes");
        for (int i = 0; i < bytes.length; i++) {
            // ensure that surrogates are split over several buffers.
            publisher.submit(List.of(ByteBuffer.wrap(bytes, i, 1)));
        }
        publisher.close();
        Void resp = bodySubscriber.getBody().toCompletableFuture().get();
        System.out.println("***** Got: " + resp);
        List<String> expected = List.of("Bient\u00f4t",
                " nous plongerons",
                " dans",
                " les",
                "",
                " fr\u00f4\ud801\udc00des",
                " t\u00e9n\u00e8bres");
        assertEquals(subscriber.text, expected.stream().collect(Collectors.joining("")));
        assertEquals(subscriber.list, expected);
        assertEquals(subscriber.list, lines(text, null));
    }


    /** An abstract Subscriber that converts all received data into a String. */
    static abstract class AbstractSubscriber implements Supplier<String> {
        protected final List<Object> list = new CopyOnWriteArrayList<>();
        protected volatile Flow.Subscription subscription;
        protected final StringBuilder baos = new StringBuilder();
        protected volatile String text;
        protected volatile RuntimeException error;

        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            subscription.request(Long.MAX_VALUE);
        }
        public void onError(Throwable throwable) {
            System.out.println(this + " onError: " + throwable);
            error = new RuntimeException(throwable);
        }
        public void onComplete() {
            System.out.println(this + " onComplete");
            text = baos.toString();
        }
        @Override public String get() {
            if (error != null) throw error;
            return text;
        }
        public final List<?> list() {
            return list;
        }
    }

    static class StringSubscriber extends AbstractSubscriber
            implements Flow.Subscriber<String>, Supplier<String>
    {
        @Override public void onNext(String item) {
            System.out.println(this + " onNext: \""
                    + item.replace("\n","\\n")
                          .replace("\r", "\\r")
                    + "\"");
            baos.append(item);
            list.add(item);
        }
    }

    static class CharSequenceSubscriber extends AbstractSubscriber
            implements Flow.Subscriber<CharSequence>, Supplier<String>
    {
        @Override public void onNext(CharSequence item) {
            System.out.println(this + " onNext: \""
                    + item.toString().replace("\n","\\n")
                    .replace("\r", "\\r")
                    + "\"");
            baos.append(item);
            list.add(item);
        }
    }

    static class ObjectSubscriber extends AbstractSubscriber
            implements Flow.Subscriber<Object>, Supplier<String>
    {
        @Override public void onNext(Object item) {
            System.out.println(this + " onNext: \""
                    + item.toString().replace("\n","\\n")
                    .replace("\r", "\\r")
                    + "\"");
            baos.append(item);
            list.add(item);
        }
    }


    static void uncheckedWrite(ByteArrayOutputStream baos, byte[] ba) {
        try {
            baos.write(ba);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
