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

/*
 * @test
 * @bug 8226602
 * @summary Tests convenience reactive primitives with RS TCK
 *
 * @library ../reactivestreams-tck
 * @build S
 *
 * @compile -encoding UTF-8 SPublisherOfStream.java
 *
 * @compile -encoding UTF-8 BodyPublishersFromPublisher.java
 * @compile -encoding UTF-8 BodyPublishersNoBody.java
 * @compile -encoding UTF-8 BodyPublishersOfByteArray.java
 * @compile -encoding UTF-8 BodyPublishersOfByteArrays.java
 * @compile -encoding UTF-8 BodyPublishersOfFile.java
 * @compile -encoding UTF-8 BodyPublishersOfInputStream.java
 * @compile -encoding UTF-8 BodyPublishersOfSubByteArray.java
 * @compile -encoding UTF-8 BodyPublishersConcat.java
 *
 * @compile -encoding UTF-8 BodySubscribersBuffering.java
 * @compile -encoding UTF-8 BodySubscribersDiscarding.java
 * @compile -encoding UTF-8 BodySubscribersFromLineSubscriber.java
 * @compile -encoding UTF-8 BodySubscribersFromSubscriber.java
 * @compile -encoding UTF-8 BodySubscribersMapping.java
 * @compile -encoding UTF-8 BodySubscribersOfByteArray.java
 * @compile -encoding UTF-8 BodySubscribersOfByteArrayConsumer.java
 * @compile -encoding UTF-8 BodySubscribersOfFile.java
 * @compile -encoding UTF-8 BodySubscribersOfInputStream.java
 * @compile -encoding UTF-8 BodySubscribersOfLines.java
 * @compile -encoding UTF-8 BodySubscribersOfPublisher.java
 * @compile -encoding UTF-8 BodySubscribersOfPublisher1.java
 * @compile -encoding UTF-8 BodySubscribersOfPublisherPublisher.java
 * @compile -encoding UTF-8 BodySubscribersOfString.java
 * @compile -encoding UTF-8 BodySubscribersReplacing.java
 *
 * @run testng/othervm STest
 * @run testng/othervm SPublisherOfStream
 *
 * @run testng/othervm BodyPublishersFromPublisher
 * @run testng/othervm BodyPublishersNoBody
 * @run testng/othervm BodyPublishersOfByteArray
 * @run testng/othervm BodyPublishersOfByteArrays
 * @run testng/othervm BodyPublishersOfFile
 * @run testng/othervm BodyPublishersOfInputStream
 * @run testng/othervm BodyPublishersOfSubByteArray
 * @run testng/othervm BodyPublishersConcat
 *
 * @run testng/othervm BodySubscribersBuffering
 * @run testng/othervm BodySubscribersDiscarding
 * @run testng/othervm BodySubscribersFromLineSubscriber
 * @run testng/othervm BodySubscribersFromSubscriber
 * @run testng/othervm BodySubscribersMapping
 * @run testng/othervm BodySubscribersOfByteArray
 * @run testng/othervm BodySubscribersOfByteArrayConsumer
 * @run testng/othervm BodySubscribersOfFile
 * @run testng/othervm BodySubscribersOfInputStream
 * @run testng/othervm BodySubscribersOfLines
 * @run testng/othervm BodySubscribersOfPublisher
 * @run testng/othervm BodySubscribersOfPublisher1
 * @run testng/othervm BodySubscribersOfPublisherPublisher
 * @run testng/othervm BodySubscribersOfString
 * @run testng/othervm BodySubscribersReplacing
 *
 * @key randomness
 */
public class TckDriver {
   /*
        #### General Information

        1. This JTREG test aggregates multiple TestNG tests. This is because
        these tests share a common library (reactivestreams-tck), and we don't
        want this library to be compiled separately for each of those tests.

        2. Tests that use RS TCK are compiled with the UTF-8 encoding. This is
        performed for the sake of reactivestreams-tck. We don't want to patch
        the TCK because of the extra merging work in the future, should we bring
        update(s) from the RS repo.

        #### Tests

        1. The purpose of each test should be easily digestible. The name of the
        test is derived from the very entity the test exercises. For example,

            the BodyPublishersOfFile test exercises the BodyPublisher obtained
            by calling BodyPublishers.ofFile(Path)

            the BodySubscribersOfFile test exercises the BodySubscriber obtained
            by calling BodySubscribers.ofFile(Path)

        2. RS TCK requires PublisherVerification tests to produce publishers
        capable of emitting a certain number of elements. In order to achieve
        this, we use some knowledge of the internal workings of our publishers.
        An example would be a chunk size a publisher uses to deliver a portion
        of data. Without knowing that it is not possible to guarantee that the
        publisher will emit a particular number of elements.

        3. Typically our publishers cannot be created in a known failed state.
        In this case the corresponding `createFailedFlowPublisher` method
        returns `null`.

        4. SubscriberBlackBoxVerification uses the `createElement(int element)`
        method. Our implementations usually cap the amount of data created by
        this method, because it's not known beforehand how big the `element`
        value is. Hence, sometimes there's code like as follows:

            @Override
            public List<ByteBuffer> createElement(int element) {
                return scatterBuffer(
                        bufferOfNRandomASCIIBytes(element % 17));
            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~^
            }

        5. The amount of testing RS TCK performs on a publisher seems to depend
        on the number of elements this publisher reports it can emit. Sometimes
        a code like the following can be seen in the tests:

                @Override public long maxElementsFromPublisher() {
                    return 21;
                ~~~~~~~~~~~^
                }

        This magic number is a result of trial and error and seems to unlock
        most of the tests. Reporting big values (e.g. Long.MAX_VALUE - 1) is
        not an option for most of our publishers because they require to have
        all the elements upfront.

        6. It doesn't seem currently feasible to provide SubscriberWhiteboxVerification
        tests as a) it's not clear how much better the coverage is and b) it's
        significantly harder to code that.

        #### S (Support)

        Support utilities are being tested (STest) too.
    */
}
