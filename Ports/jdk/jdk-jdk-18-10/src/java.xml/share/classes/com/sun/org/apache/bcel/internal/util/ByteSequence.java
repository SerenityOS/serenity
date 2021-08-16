/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sun.org.apache.bcel.internal.util;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

/**
 * Utility class that implements a sequence of bytes which can be read
 * via the `readByte()' method. This is used to implement a wrapper for the
 * Java byte code stream to gain some more readability.
 *
 */
public final class ByteSequence extends DataInputStream {

    private final ByteArrayStream byteStream;


    public ByteSequence(final byte[] bytes) {
        super(new ByteArrayStream(bytes));
        byteStream = (ByteArrayStream) in;
    }


    public int getIndex() {
        return byteStream.getPosition();
    }


    void unreadByte() {
        byteStream.unreadByte();
    }

    private static final class ByteArrayStream extends ByteArrayInputStream {

        ByteArrayStream(final byte[] bytes) {
            super(bytes);
        }

        int getPosition() {
            // pos is protected in ByteArrayInputStream
            return pos;
        }

        void unreadByte() {
            if (pos > 0) {
                pos--;
            }
        }
    }
}
