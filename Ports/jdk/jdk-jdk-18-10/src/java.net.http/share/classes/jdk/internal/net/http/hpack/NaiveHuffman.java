/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
package jdk.internal.net.http.hpack;

import java.io.IOException;
import java.nio.ByteBuffer;

import static java.lang.String.format;

/**
 * Huffman coding table.
 *
 * <p> Instances of this class are safe for use by multiple threads.
 *
 * @since 9
 */
public final class NaiveHuffman {

    // TODO: check if reset is done in both reader and writer

    static final class Reader implements Huffman.Reader {

        private Node curr; // position in the trie
        private int len;   // length of the path from the root to 'curr'
        private int p;     // byte probe

        {
            reset();
        }

        @Override
        public void read(ByteBuffer source,
                         Appendable destination,
                         boolean isLast) throws IOException {
            read(source, destination, true, isLast);
        }

        // Takes 'isLast' rather than returns whether the reading is done or
        // not, for more informative exceptions.
        void read(ByteBuffer source,
                  Appendable destination,
                  boolean reportEOS, /* reportEOS is exposed for tests */
                  boolean isLast) throws IOException {
            Node c = curr;
            int l = len;
            /*
               Since ByteBuffer is itself stateful, its position is
               remembered here NOT as a part of Reader's state,
               but to set it back in the case of a failure
             */
            int pos = source.position();

            while (source.hasRemaining()) {
                int d = source.get();
                for (; p != 0; p >>= 1) {
                    c = c.getChild(p & d);
                    l++;
                    if (c.isLeaf()) {
                        if (reportEOS && c.isEOSPath) {
                            throw new IOException("Encountered EOS");
                        }
                        char ch;
                        try {
                            ch = c.getChar();
                        } catch (IllegalStateException e) {
                            source.position(pos); // do we need this?
                            throw new IOException(e);
                        }
                        try {
                            destination.append(ch);
                        } catch (IOException e) {
                            source.position(pos); // do we need this?
                            throw e;
                        }
                        c = INSTANCE.root;
                        l = 0;
                    }
                    curr = c;
                    len = l;
                }
                resetProbe();
                pos++;
            }
            if (!isLast) {
                return; // it's too early to jump to any conclusions, let's wait
            }
            if (c.isLeaf()) {
                return; // it's perfectly ok, no extra padding bits
            }
            if (c.isEOSPath && len <= 7) {
                return; // it's ok, some extra padding bits
            }
            if (c.isEOSPath) {
                throw new IOException(
                        "Padding is too long (len=" + len + ") " +
                                "or unexpected end of data");
            }
            throw new IOException(
                    "Not a EOS prefix padding or unexpected end of data");
        }

        @Override
        public void reset() {
            curr = INSTANCE.root;
            len = 0;
            resetProbe();
        }

        private void resetProbe() {
            p = 0x80;
        }
    }

    static final class Writer implements Huffman.Writer {

        private int pos;       // position in 'source'
        private int avail = 8; // number of least significant bits available in 'curr'
        private int curr;      // next byte to put to the destination
        private int rem;       // number of least significant bits in 'code' yet to be processed
        private int code;      // current code being written

        private CharSequence source;
        private int end;

        @Override
        public Writer from(CharSequence input, int start, int end) {
            if (start < 0 || end < 0 || end > input.length() || start > end) {
                throw new IndexOutOfBoundsException(
                        String.format("input.length()=%s, start=%s, end=%s",
                                      input.length(), start, end));
            }
            pos = start;
            this.end = end;
            this.source = input;
            return this;
        }

        @Override
        public boolean write(ByteBuffer destination) {
            for (; pos < end; pos++) {
                if (rem == 0) {
                    Code desc = INSTANCE.codeOf(source.charAt(pos));
                    rem = desc.length;
                    code = desc.code;
                }
                while (rem > 0) {
                    if (rem < avail) {
                        curr |= (code << (avail - rem));
                        avail -= rem;
                        rem = 0;
                    } else {
                        int c = (curr | (code >>> (rem - avail)));
                        if (destination.hasRemaining()) {
                            destination.put((byte) c);
                        } else {
                            return false;
                        }
                        curr = c;
                        code <<= (32 - rem + avail);  // throw written bits off the cliff (is this Sparta?)
                        code >>>= (32 - rem + avail); // return to the position
                        rem -= avail;
                        curr = 0;
                        avail = 8;
                    }
                }
            }

            if (avail < 8) { // have to pad
                if (destination.hasRemaining()) {
                    destination.put((byte) (curr | (INSTANCE.EOS.code >>> (INSTANCE.EOS.length - avail))));
                    avail = 8;
                } else {
                    return false;
                }
            }

            return true;
        }

        @Override
        public Writer reset() {
            source = null;
            end = -1;
            pos = -1;
            avail = 8;
            curr = 0;
            code = 0;
            return this;
        }

        @Override
        public int lengthOf(CharSequence value, int start, int end) {
            return INSTANCE.lengthOf(value, start, end);
        }
    }

    /**
     * Shared instance.
     */
    public static final NaiveHuffman INSTANCE = new NaiveHuffman();

    private final Code EOS = new Code(0x3fffffff, 30);
    private final Code[] codes = new Code[257];
    private final Node root = new Node() {
        @Override
        public String toString() { return "root"; }
    };

    // TODO: consider builder and immutable trie
    private NaiveHuffman() {
        // @formatter:off
        addChar(0,   0x1ff8,     13);
        addChar(1,   0x7fffd8,   23);
        addChar(2,   0xfffffe2,  28);
        addChar(3,   0xfffffe3,  28);
        addChar(4,   0xfffffe4,  28);
        addChar(5,   0xfffffe5,  28);
        addChar(6,   0xfffffe6,  28);
        addChar(7,   0xfffffe7,  28);
        addChar(8,   0xfffffe8,  28);
        addChar(9,   0xffffea,   24);
        addChar(10,  0x3ffffffc, 30);
        addChar(11,  0xfffffe9,  28);
        addChar(12,  0xfffffea,  28);
        addChar(13,  0x3ffffffd, 30);
        addChar(14,  0xfffffeb,  28);
        addChar(15,  0xfffffec,  28);
        addChar(16,  0xfffffed,  28);
        addChar(17,  0xfffffee,  28);
        addChar(18,  0xfffffef,  28);
        addChar(19,  0xffffff0,  28);
        addChar(20,  0xffffff1,  28);
        addChar(21,  0xffffff2,  28);
        addChar(22,  0x3ffffffe, 30);
        addChar(23,  0xffffff3,  28);
        addChar(24,  0xffffff4,  28);
        addChar(25,  0xffffff5,  28);
        addChar(26,  0xffffff6,  28);
        addChar(27,  0xffffff7,  28);
        addChar(28,  0xffffff8,  28);
        addChar(29,  0xffffff9,  28);
        addChar(30,  0xffffffa,  28);
        addChar(31,  0xffffffb,  28);
        addChar(32,  0x14,        6);
        addChar(33,  0x3f8,      10);
        addChar(34,  0x3f9,      10);
        addChar(35,  0xffa,      12);
        addChar(36,  0x1ff9,     13);
        addChar(37,  0x15,        6);
        addChar(38,  0xf8,        8);
        addChar(39,  0x7fa,      11);
        addChar(40,  0x3fa,      10);
        addChar(41,  0x3fb,      10);
        addChar(42,  0xf9,        8);
        addChar(43,  0x7fb,      11);
        addChar(44,  0xfa,        8);
        addChar(45,  0x16,        6);
        addChar(46,  0x17,        6);
        addChar(47,  0x18,        6);
        addChar(48,  0x0,         5);
        addChar(49,  0x1,         5);
        addChar(50,  0x2,         5);
        addChar(51,  0x19,        6);
        addChar(52,  0x1a,        6);
        addChar(53,  0x1b,        6);
        addChar(54,  0x1c,        6);
        addChar(55,  0x1d,        6);
        addChar(56,  0x1e,        6);
        addChar(57,  0x1f,        6);
        addChar(58,  0x5c,        7);
        addChar(59,  0xfb,        8);
        addChar(60,  0x7ffc,     15);
        addChar(61,  0x20,        6);
        addChar(62,  0xffb,      12);
        addChar(63,  0x3fc,      10);
        addChar(64,  0x1ffa,     13);
        addChar(65,  0x21,        6);
        addChar(66,  0x5d,        7);
        addChar(67,  0x5e,        7);
        addChar(68,  0x5f,        7);
        addChar(69,  0x60,        7);
        addChar(70,  0x61,        7);
        addChar(71,  0x62,        7);
        addChar(72,  0x63,        7);
        addChar(73,  0x64,        7);
        addChar(74,  0x65,        7);
        addChar(75,  0x66,        7);
        addChar(76,  0x67,        7);
        addChar(77,  0x68,        7);
        addChar(78,  0x69,        7);
        addChar(79,  0x6a,        7);
        addChar(80,  0x6b,        7);
        addChar(81,  0x6c,        7);
        addChar(82,  0x6d,        7);
        addChar(83,  0x6e,        7);
        addChar(84,  0x6f,        7);
        addChar(85,  0x70,        7);
        addChar(86,  0x71,        7);
        addChar(87,  0x72,        7);
        addChar(88,  0xfc,        8);
        addChar(89,  0x73,        7);
        addChar(90,  0xfd,        8);
        addChar(91,  0x1ffb,     13);
        addChar(92,  0x7fff0,    19);
        addChar(93,  0x1ffc,     13);
        addChar(94,  0x3ffc,     14);
        addChar(95,  0x22,        6);
        addChar(96,  0x7ffd,     15);
        addChar(97,  0x3,         5);
        addChar(98,  0x23,        6);
        addChar(99,  0x4,         5);
        addChar(100, 0x24,        6);
        addChar(101, 0x5,         5);
        addChar(102, 0x25,        6);
        addChar(103, 0x26,        6);
        addChar(104, 0x27,        6);
        addChar(105, 0x6,         5);
        addChar(106, 0x74,        7);
        addChar(107, 0x75,        7);
        addChar(108, 0x28,        6);
        addChar(109, 0x29,        6);
        addChar(110, 0x2a,        6);
        addChar(111, 0x7,         5);
        addChar(112, 0x2b,        6);
        addChar(113, 0x76,        7);
        addChar(114, 0x2c,        6);
        addChar(115, 0x8,         5);
        addChar(116, 0x9,         5);
        addChar(117, 0x2d,        6);
        addChar(118, 0x77,        7);
        addChar(119, 0x78,        7);
        addChar(120, 0x79,        7);
        addChar(121, 0x7a,        7);
        addChar(122, 0x7b,        7);
        addChar(123, 0x7ffe,     15);
        addChar(124, 0x7fc,      11);
        addChar(125, 0x3ffd,     14);
        addChar(126, 0x1ffd,     13);
        addChar(127, 0xffffffc,  28);
        addChar(128, 0xfffe6,    20);
        addChar(129, 0x3fffd2,   22);
        addChar(130, 0xfffe7,    20);
        addChar(131, 0xfffe8,    20);
        addChar(132, 0x3fffd3,   22);
        addChar(133, 0x3fffd4,   22);
        addChar(134, 0x3fffd5,   22);
        addChar(135, 0x7fffd9,   23);
        addChar(136, 0x3fffd6,   22);
        addChar(137, 0x7fffda,   23);
        addChar(138, 0x7fffdb,   23);
        addChar(139, 0x7fffdc,   23);
        addChar(140, 0x7fffdd,   23);
        addChar(141, 0x7fffde,   23);
        addChar(142, 0xffffeb,   24);
        addChar(143, 0x7fffdf,   23);
        addChar(144, 0xffffec,   24);
        addChar(145, 0xffffed,   24);
        addChar(146, 0x3fffd7,   22);
        addChar(147, 0x7fffe0,   23);
        addChar(148, 0xffffee,   24);
        addChar(149, 0x7fffe1,   23);
        addChar(150, 0x7fffe2,   23);
        addChar(151, 0x7fffe3,   23);
        addChar(152, 0x7fffe4,   23);
        addChar(153, 0x1fffdc,   21);
        addChar(154, 0x3fffd8,   22);
        addChar(155, 0x7fffe5,   23);
        addChar(156, 0x3fffd9,   22);
        addChar(157, 0x7fffe6,   23);
        addChar(158, 0x7fffe7,   23);
        addChar(159, 0xffffef,   24);
        addChar(160, 0x3fffda,   22);
        addChar(161, 0x1fffdd,   21);
        addChar(162, 0xfffe9,    20);
        addChar(163, 0x3fffdb,   22);
        addChar(164, 0x3fffdc,   22);
        addChar(165, 0x7fffe8,   23);
        addChar(166, 0x7fffe9,   23);
        addChar(167, 0x1fffde,   21);
        addChar(168, 0x7fffea,   23);
        addChar(169, 0x3fffdd,   22);
        addChar(170, 0x3fffde,   22);
        addChar(171, 0xfffff0,   24);
        addChar(172, 0x1fffdf,   21);
        addChar(173, 0x3fffdf,   22);
        addChar(174, 0x7fffeb,   23);
        addChar(175, 0x7fffec,   23);
        addChar(176, 0x1fffe0,   21);
        addChar(177, 0x1fffe1,   21);
        addChar(178, 0x3fffe0,   22);
        addChar(179, 0x1fffe2,   21);
        addChar(180, 0x7fffed,   23);
        addChar(181, 0x3fffe1,   22);
        addChar(182, 0x7fffee,   23);
        addChar(183, 0x7fffef,   23);
        addChar(184, 0xfffea,    20);
        addChar(185, 0x3fffe2,   22);
        addChar(186, 0x3fffe3,   22);
        addChar(187, 0x3fffe4,   22);
        addChar(188, 0x7ffff0,   23);
        addChar(189, 0x3fffe5,   22);
        addChar(190, 0x3fffe6,   22);
        addChar(191, 0x7ffff1,   23);
        addChar(192, 0x3ffffe0,  26);
        addChar(193, 0x3ffffe1,  26);
        addChar(194, 0xfffeb,    20);
        addChar(195, 0x7fff1,    19);
        addChar(196, 0x3fffe7,   22);
        addChar(197, 0x7ffff2,   23);
        addChar(198, 0x3fffe8,   22);
        addChar(199, 0x1ffffec,  25);
        addChar(200, 0x3ffffe2,  26);
        addChar(201, 0x3ffffe3,  26);
        addChar(202, 0x3ffffe4,  26);
        addChar(203, 0x7ffffde,  27);
        addChar(204, 0x7ffffdf,  27);
        addChar(205, 0x3ffffe5,  26);
        addChar(206, 0xfffff1,   24);
        addChar(207, 0x1ffffed,  25);
        addChar(208, 0x7fff2,    19);
        addChar(209, 0x1fffe3,   21);
        addChar(210, 0x3ffffe6,  26);
        addChar(211, 0x7ffffe0,  27);
        addChar(212, 0x7ffffe1,  27);
        addChar(213, 0x3ffffe7,  26);
        addChar(214, 0x7ffffe2,  27);
        addChar(215, 0xfffff2,   24);
        addChar(216, 0x1fffe4,   21);
        addChar(217, 0x1fffe5,   21);
        addChar(218, 0x3ffffe8,  26);
        addChar(219, 0x3ffffe9,  26);
        addChar(220, 0xffffffd,  28);
        addChar(221, 0x7ffffe3,  27);
        addChar(222, 0x7ffffe4,  27);
        addChar(223, 0x7ffffe5,  27);
        addChar(224, 0xfffec,    20);
        addChar(225, 0xfffff3,   24);
        addChar(226, 0xfffed,    20);
        addChar(227, 0x1fffe6,   21);
        addChar(228, 0x3fffe9,   22);
        addChar(229, 0x1fffe7,   21);
        addChar(230, 0x1fffe8,   21);
        addChar(231, 0x7ffff3,   23);
        addChar(232, 0x3fffea,   22);
        addChar(233, 0x3fffeb,   22);
        addChar(234, 0x1ffffee,  25);
        addChar(235, 0x1ffffef,  25);
        addChar(236, 0xfffff4,   24);
        addChar(237, 0xfffff5,   24);
        addChar(238, 0x3ffffea,  26);
        addChar(239, 0x7ffff4,   23);
        addChar(240, 0x3ffffeb,  26);
        addChar(241, 0x7ffffe6,  27);
        addChar(242, 0x3ffffec,  26);
        addChar(243, 0x3ffffed,  26);
        addChar(244, 0x7ffffe7,  27);
        addChar(245, 0x7ffffe8,  27);
        addChar(246, 0x7ffffe9,  27);
        addChar(247, 0x7ffffea,  27);
        addChar(248, 0x7ffffeb,  27);
        addChar(249, 0xffffffe,  28);
        addChar(250, 0x7ffffec,  27);
        addChar(251, 0x7ffffed,  27);
        addChar(252, 0x7ffffee,  27);
        addChar(253, 0x7ffffef,  27);
        addChar(254, 0x7fffff0,  27);
        addChar(255, 0x3ffffee,  26);
        addEOS (256, EOS.code,   EOS.length);
        // @formatter:on
    }


    /**
     * Calculates the number of bytes required to represent the given {@code
     * CharSequence} with the Huffman coding.
     *
     * @param value
     *         characters
     *
     * @return number of bytes
     *
     * @throws NullPointerException
     *         if the value is null
     */
    public int lengthOf(CharSequence value) {
        return lengthOf(value, 0, value.length());
    }

    /**
     * Calculates the number of bytes required to represent a subsequence of the
     * given {@code CharSequence} with the Huffman coding.
     *
     * @param value
     *         characters
     * @param start
     *         the start index, inclusive
     * @param end
     *         the end index, exclusive
     *
     * @return number of bytes
     *
     * @throws NullPointerException
     *         if the value is null
     * @throws IndexOutOfBoundsException
     *         if any invocation of {@code value.charAt(i)}, where
     *         {@code start <= i < end} would throw an IndexOutOfBoundsException
     */
    public int lengthOf(CharSequence value, int start, int end) {
        int len = 0;
        for (int i = start; i < end; i++) {
            char c = value.charAt(i);
            len += INSTANCE.codeOf(c).length;
        }
        // Integer division with ceiling, assumption:
        assert (len / 8 + (len % 8 != 0 ? 1 : 0)) == (len + 7) / 8 : len;
        return (len + 7) / 8;
    }

    private void addChar(int c, int code, int bitLength) {
        addLeaf(c, code, bitLength, false);
        codes[c] = new Code(code, bitLength);
    }

    private void addEOS(int c, int code, int bitLength) {
        addLeaf(c, code, bitLength, true);
        codes[c] = new Code(code, bitLength);
    }

    private void addLeaf(int c, int code, int bitLength, boolean isEOS) {
        if (bitLength < 1) {
            throw new IllegalArgumentException("bitLength < 1");
        }
        Node curr = root;
        for (int p = 1 << bitLength - 1; p != 0 && !curr.isLeaf(); p = p >> 1) {
            curr.isEOSPath |= isEOS; // If it's already true, it can't become false
            curr = curr.addChildIfAbsent(p & code);
        }
        curr.isEOSPath |= isEOS; // The last one needs to have this property as well
        if (curr.isLeaf()) {
            throw new IllegalStateException("Specified code is already taken");
        }
        curr.setChar((char) c);
    }

    private Code codeOf(char c) {
        if (c > 255) {
            throw new IllegalArgumentException("char=" + ((int) c));
        }
        return codes[c];
    }

    //
    // For debugging/testing purposes
    //
    Node getRoot() {
        return root;
    }

    //
    // Guarantees:
    //
    //  if (isLeaf() == true) => getChar() is a legal call
    //  if (isLeaf() == false) => getChild(i) is a legal call (though it can
    //                                                           return null)
    //
    static class Node {

        Node left;
        Node right;
        boolean isEOSPath;

        boolean charIsSet;
        char c;

        Node getChild(int selector) {
            if (isLeaf()) {
                throw new IllegalStateException("This is a leaf node");
            }
            Node result = selector == 0 ? left : right;
            if (result == null) {
                throw new IllegalStateException(format(
                        "Node doesn't have a child (selector=%s)", selector));
            }
            return result;
        }

        boolean isLeaf() {
            return charIsSet;
        }

        char getChar() {
            if (!isLeaf()) {
                throw new IllegalStateException("This node is not a leaf node");
            }
            return c;
        }

        void setChar(char c) {
            if (charIsSet) {
                throw new IllegalStateException(
                        "This node has been taken already");
            }
            if (left != null || right != null) {
                throw new IllegalStateException("The node cannot be made "
                                                        + "a leaf as it's already has a child");
            }
            this.c = c;
            charIsSet = true;
        }

        Node addChildIfAbsent(int i) {
            if (charIsSet) {
                throw new IllegalStateException("The node cannot have a child "
                                                        + "as it's already a leaf node");
            }
            Node child;
            if (i == 0) {
                if ((child = left) == null) {
                    child = left = new Node();
                }
            } else {
                if ((child = right) == null) {
                    child = right = new Node();
                }
            }
            return child;
        }

        @Override
        public String toString() {
            if (isLeaf()) {
                if (isEOSPath) {
                    return "EOS";
                } else {
                    return format("char: (%3s) '%s'", (int) c, c);
                }
            }
            return "/\\";
        }
    }

    // TODO: value-based class?
    // FIXME: can we re-use Node instead of this class?
    private static final class Code {

        final int code;
        final int length;

        private Code(int code, int length) {
            this.code = code;
            this.length = length;
        }

        public int getCode() {
            return code;
        }

        public int getLength() {
            return length;
        }

        @Override
        public String toString() {
            long p = 1 << length;
            return Long.toBinaryString(code + p).substring(1)
                    + ", length=" + length;
        }
    }
}
