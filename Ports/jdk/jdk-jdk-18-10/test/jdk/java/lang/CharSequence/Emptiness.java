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

import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @test
 * @bug 8215401
 * @summary Test to verify the isEmpty method is behaviorally consistent
 *          with length
 * @run testng Emptiness
 */
public class Emptiness {

    @Test
    public void isEmpty() {
        checkEmpty(new StringBuilder());
        checkEmpty(new StringBuffer());
        checkEmpty("");
        checkEmpty(new CharSequence() {
            @Override public int length() { return 0; }
            @Override public char charAt(int index) { return 'f'; }
            @Override public CharSequence subSequence(int start, int end) {
                throw new UnsupportedOperationException();
            }
        });
        checkEmpty(CharBuffer.wrap(new char[0]));
        checkEmpty(CharBuffer.wrap(""));

        // A CharBuffer being filled is empty when there's no room remaining
        // - or there's nothing in the buffer after a flip
        checkEmpty(ByteBuffer.allocate(0).asCharBuffer());
        checkEmpty(ByteBuffer.allocate(2).asCharBuffer().append('f'));
        checkEmpty(ByteBuffer.allocate(2).asCharBuffer().flip());

        checkEmpty(ByteBuffer.allocateDirect(0).asCharBuffer());
        checkEmpty(ByteBuffer.allocateDirect(2).asCharBuffer().append('f'));
        checkEmpty(ByteBuffer.allocateDirect(2).asCharBuffer().flip());
    }

    @Test
    public void isNotEmpty() {
        checkNotEmpty(new StringBuilder().append("foo"));
        checkNotEmpty(new StringBuffer().append("bar"));
        checkNotEmpty("baz");
        checkNotEmpty(new CharSequence() {
            @Override public int length() { return 1; }
            @Override public char charAt(int index) { return 'f'; }
            @Override public CharSequence subSequence(int start, int end) {
                throw new UnsupportedOperationException();
            }
        });
        checkNotEmpty(CharBuffer.wrap(new char[] { 'f' }));
        checkNotEmpty(CharBuffer.wrap("foo"));

        // A CharBuffer being filled is non-empty when there's room remaining
        // - or when there's something in the buffer after a flip
        checkNotEmpty(ByteBuffer.allocate(2).asCharBuffer());
        checkNotEmpty(ByteBuffer.allocate(4).asCharBuffer().append('f'));
        checkNotEmpty(ByteBuffer.allocate(2).asCharBuffer().append('f').flip());

        checkNotEmpty(ByteBuffer.allocateDirect(2).asCharBuffer());
        checkNotEmpty(ByteBuffer.allocateDirect(4).asCharBuffer().append('f'));
        checkNotEmpty(ByteBuffer.allocateDirect(2).asCharBuffer().append('f').flip());
    }

    public void checkEmpty(CharSequence cs) {
        Assert.assertTrue(cs.isEmpty());
        Assert.assertTrue(consistentWithLength(cs));
    }

    public void checkNotEmpty(CharSequence cs) {
        Assert.assertTrue(!cs.isEmpty());
        Assert.assertTrue(consistentWithLength(cs));
    }

    public boolean consistentWithLength(CharSequence cs) {
        return cs.isEmpty() == (cs.length() == 0);
    }
}
