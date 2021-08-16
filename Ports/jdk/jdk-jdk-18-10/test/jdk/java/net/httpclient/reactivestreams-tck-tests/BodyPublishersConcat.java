/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.flow.FlowPublisherVerification;

import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Flow.Publisher;

/* See TckDriver.java for more information */
public class BodyPublishersConcat
        extends FlowPublisherVerification<ByteBuffer> {

    private static final int ELEMENT_SIZE = 16 * 1024;

    public BodyPublishersConcat() {
        super(new TestEnvironment(450L));
    }

    private static BodyPublisher ofByteArrays(int n, byte[] bytes) {
        return BodyPublishers.ofByteArrays(Collections.nCopies((int) n, bytes));
    }

    @Override
    public Publisher<ByteBuffer> createFlowPublisher(long nElements) {
        System.out.println("BodyPublishersConcat: %d elements requested"
                .formatted(nElements));
        byte[] bytes = S.arrayOfNRandomBytes(ELEMENT_SIZE);
        if (nElements == 0) {
            System.out.println("BodyPublishersConcat: empty publisher");
            return BodyPublishers.concat();
        } else if (nElements == 1) {
            System.out.println("BodyPublishersConcat: singleton publisher");
            return BodyPublishers.concat(ofByteArrays(1, bytes));
        } else if (nElements < 4) {
            int left = (int)nElements/2;
            int right = (int)nElements - left;
            System.out.println("BodyPublishersConcat: dual publisher (%d, %d)".formatted(left, right));
            return BodyPublishers.concat(ofByteArrays(left, bytes),
                    ofByteArrays(right, bytes));
        } else {
            List<BodyPublisher> publishers = new ArrayList<>();
            List<Integer> sizes = new ArrayList<>();
            long remaining = nElements;
            int max = (int) Math.min((long)Integer.MAX_VALUE, nElements/2L);
            while (remaining > 0) {
                int length = S.randomIntUpTo(max);
                if (length == 0) length = 1;
                sizes.add(length);
                if (remaining > length) {
                    publishers.add(ofByteArrays(length, bytes));
                    remaining = remaining - length;
                } else {
                    publishers.add(ofByteArrays((int)remaining, bytes));
                    remaining = 0;
                }
            }
            System.out.println("BodyPublishersConcat: multi publisher " + sizes);
            return BodyPublishers.concat(publishers.toArray(BodyPublisher[]::new));
        }
    }

    @Override
    public Publisher<ByteBuffer> createFailedFlowPublisher() {
        return null;
    }
}
