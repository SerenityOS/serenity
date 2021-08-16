/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

import jdk.internal.net.http.hpack.HPACK.BufferUpdateConsumer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;

import static jdk.internal.net.http.hpack.HPACK.bytesForBits;

public final class QuickHuffman {

    /*
     * Mapping of characters to their code information. Code information
     * consists of a code value and a code length. Both components are packed in
     * a single long as follows:
     *
     *     MSB                             LSB
     *     +----------------+----------------+
     *     |~code           |         length~|
     *     +----------------+----------------+
     *     |<----- 32 ----->|<----- 32 ----->|
     *     |<------------- 64 -------------->|
     *
     * The leftmost 32 bits hold the code value. This value is aligned left (or
     * MSB). The rightmost 32 bits hold the code length. This length is aligned
     * right (or LSB). Such structure is possible thanks to codes being up to 30
     * bits long and their lengths being up to 5 bits long (length = 5..30).
     * This allows both components to fit into long and, thus, better locality.
     *
     * Input strings never contain EOS. Thus there's no need to provide EOS
     * mapping. Hence, the length of the array is 256, not 257.
     */
    private static final long[] codes = new long[256];

    private static long codeValueOf(char c) {
        return codes[c] & 0xffffffff00000000L;
    }

    private static long codeLengthOf(char c) {
        return codes[c] & 0x00000000ffffffffL;
    }

    private static final int EOS_LENGTH = 30;
    private static final int EOS_LSB = 0x3fffffff;
    // EOS_LSB is casted to long before the shift, to allow long shift (6 bits)
    // instead of int shift (5 bits):
    private static final long EOS_MSB = ((long) EOS_LSB) << (64 - EOS_LENGTH);

    /*
     * Huffman trie.
     *
     * The root node leads to 257 descendant leaf nodes, of which 256 nodes
     * correspond to characters and 1 node correspond to EOS.
     */
    private static final Node root = buildTrie();

    private static Node buildTrie() {
        TemporaryNode tmpRoot = new TemporaryNode();
        addChar(tmpRoot,   0, 0x1ff8,     13);
        addChar(tmpRoot,   1, 0x7fffd8,   23);
        addChar(tmpRoot,   2, 0xfffffe2,  28);
        addChar(tmpRoot,   3, 0xfffffe3,  28);
        addChar(tmpRoot,   4, 0xfffffe4,  28);
        addChar(tmpRoot,   5, 0xfffffe5,  28);
        addChar(tmpRoot,   6, 0xfffffe6,  28);
        addChar(tmpRoot,   7, 0xfffffe7,  28);
        addChar(tmpRoot,   8, 0xfffffe8,  28);
        addChar(tmpRoot,   9, 0xffffea,   24);
        addChar(tmpRoot,  10, 0x3ffffffc, 30);
        addChar(tmpRoot,  11, 0xfffffe9,  28);
        addChar(tmpRoot,  12, 0xfffffea,  28);
        addChar(tmpRoot,  13, 0x3ffffffd, 30);
        addChar(tmpRoot,  14, 0xfffffeb,  28);
        addChar(tmpRoot,  15, 0xfffffec,  28);
        addChar(tmpRoot,  16, 0xfffffed,  28);
        addChar(tmpRoot,  17, 0xfffffee,  28);
        addChar(tmpRoot,  18, 0xfffffef,  28);
        addChar(tmpRoot,  19, 0xffffff0,  28);
        addChar(tmpRoot,  20, 0xffffff1,  28);
        addChar(tmpRoot,  21, 0xffffff2,  28);
        addChar(tmpRoot,  22, 0x3ffffffe, 30);
        addChar(tmpRoot,  23, 0xffffff3,  28);
        addChar(tmpRoot,  24, 0xffffff4,  28);
        addChar(tmpRoot,  25, 0xffffff5,  28);
        addChar(tmpRoot,  26, 0xffffff6,  28);
        addChar(tmpRoot,  27, 0xffffff7,  28);
        addChar(tmpRoot,  28, 0xffffff8,  28);
        addChar(tmpRoot,  29, 0xffffff9,  28);
        addChar(tmpRoot,  30, 0xffffffa,  28);
        addChar(tmpRoot,  31, 0xffffffb,  28);
        addChar(tmpRoot,  32, 0x14,        6);
        addChar(tmpRoot,  33, 0x3f8,      10);
        addChar(tmpRoot,  34, 0x3f9,      10);
        addChar(tmpRoot,  35, 0xffa,      12);
        addChar(tmpRoot,  36, 0x1ff9,     13);
        addChar(tmpRoot,  37, 0x15,        6);
        addChar(tmpRoot,  38, 0xf8,        8);
        addChar(tmpRoot,  39, 0x7fa,      11);
        addChar(tmpRoot,  40, 0x3fa,      10);
        addChar(tmpRoot,  41, 0x3fb,      10);
        addChar(tmpRoot,  42, 0xf9,        8);
        addChar(tmpRoot,  43, 0x7fb,      11);
        addChar(tmpRoot,  44, 0xfa,        8);
        addChar(tmpRoot,  45, 0x16,        6);
        addChar(tmpRoot,  46, 0x17,        6);
        addChar(tmpRoot,  47, 0x18,        6);
        addChar(tmpRoot,  48, 0x0,         5);
        addChar(tmpRoot,  49, 0x1,         5);
        addChar(tmpRoot,  50, 0x2,         5);
        addChar(tmpRoot,  51, 0x19,        6);
        addChar(tmpRoot,  52, 0x1a,        6);
        addChar(tmpRoot,  53, 0x1b,        6);
        addChar(tmpRoot,  54, 0x1c,        6);
        addChar(tmpRoot,  55, 0x1d,        6);
        addChar(tmpRoot,  56, 0x1e,        6);
        addChar(tmpRoot,  57, 0x1f,        6);
        addChar(tmpRoot,  58, 0x5c,        7);
        addChar(tmpRoot,  59, 0xfb,        8);
        addChar(tmpRoot,  60, 0x7ffc,     15);
        addChar(tmpRoot,  61, 0x20,        6);
        addChar(tmpRoot,  62, 0xffb,      12);
        addChar(tmpRoot,  63, 0x3fc,      10);
        addChar(tmpRoot,  64, 0x1ffa,     13);
        addChar(tmpRoot,  65, 0x21,        6);
        addChar(tmpRoot,  66, 0x5d,        7);
        addChar(tmpRoot,  67, 0x5e,        7);
        addChar(tmpRoot,  68, 0x5f,        7);
        addChar(tmpRoot,  69, 0x60,        7);
        addChar(tmpRoot,  70, 0x61,        7);
        addChar(tmpRoot,  71, 0x62,        7);
        addChar(tmpRoot,  72, 0x63,        7);
        addChar(tmpRoot,  73, 0x64,        7);
        addChar(tmpRoot,  74, 0x65,        7);
        addChar(tmpRoot,  75, 0x66,        7);
        addChar(tmpRoot,  76, 0x67,        7);
        addChar(tmpRoot,  77, 0x68,        7);
        addChar(tmpRoot,  78, 0x69,        7);
        addChar(tmpRoot,  79, 0x6a,        7);
        addChar(tmpRoot,  80, 0x6b,        7);
        addChar(tmpRoot,  81, 0x6c,        7);
        addChar(tmpRoot,  82, 0x6d,        7);
        addChar(tmpRoot,  83, 0x6e,        7);
        addChar(tmpRoot,  84, 0x6f,        7);
        addChar(tmpRoot,  85, 0x70,        7);
        addChar(tmpRoot,  86, 0x71,        7);
        addChar(tmpRoot,  87, 0x72,        7);
        addChar(tmpRoot,  88, 0xfc,        8);
        addChar(tmpRoot,  89, 0x73,        7);
        addChar(tmpRoot,  90, 0xfd,        8);
        addChar(tmpRoot,  91, 0x1ffb,     13);
        addChar(tmpRoot,  92, 0x7fff0,    19);
        addChar(tmpRoot,  93, 0x1ffc,     13);
        addChar(tmpRoot,  94, 0x3ffc,     14);
        addChar(tmpRoot,  95, 0x22,        6);
        addChar(tmpRoot,  96, 0x7ffd,     15);
        addChar(tmpRoot,  97, 0x3,         5);
        addChar(tmpRoot,  98, 0x23,        6);
        addChar(tmpRoot,  99, 0x4,         5);
        addChar(tmpRoot, 100, 0x24,        6);
        addChar(tmpRoot, 101, 0x5,         5);
        addChar(tmpRoot, 102, 0x25,        6);
        addChar(tmpRoot, 103, 0x26,        6);
        addChar(tmpRoot, 104, 0x27,        6);
        addChar(tmpRoot, 105, 0x6,         5);
        addChar(tmpRoot, 106, 0x74,        7);
        addChar(tmpRoot, 107, 0x75,        7);
        addChar(tmpRoot, 108, 0x28,        6);
        addChar(tmpRoot, 109, 0x29,        6);
        addChar(tmpRoot, 110, 0x2a,        6);
        addChar(tmpRoot, 111, 0x7,         5);
        addChar(tmpRoot, 112, 0x2b,        6);
        addChar(tmpRoot, 113, 0x76,        7);
        addChar(tmpRoot, 114, 0x2c,        6);
        addChar(tmpRoot, 115, 0x8,         5);
        addChar(tmpRoot, 116, 0x9,         5);
        addChar(tmpRoot, 117, 0x2d,        6);
        addChar(tmpRoot, 118, 0x77,        7);
        addChar(tmpRoot, 119, 0x78,        7);
        addChar(tmpRoot, 120, 0x79,        7);
        addChar(tmpRoot, 121, 0x7a,        7);
        addChar(tmpRoot, 122, 0x7b,        7);
        addChar(tmpRoot, 123, 0x7ffe,     15);
        addChar(tmpRoot, 124, 0x7fc,      11);
        addChar(tmpRoot, 125, 0x3ffd,     14);
        addChar(tmpRoot, 126, 0x1ffd,     13);
        addChar(tmpRoot, 127, 0xffffffc,  28);
        addChar(tmpRoot, 128, 0xfffe6,    20);
        addChar(tmpRoot, 129, 0x3fffd2,   22);
        addChar(tmpRoot, 130, 0xfffe7,    20);
        addChar(tmpRoot, 131, 0xfffe8,    20);
        addChar(tmpRoot, 132, 0x3fffd3,   22);
        addChar(tmpRoot, 133, 0x3fffd4,   22);
        addChar(tmpRoot, 134, 0x3fffd5,   22);
        addChar(tmpRoot, 135, 0x7fffd9,   23);
        addChar(tmpRoot, 136, 0x3fffd6,   22);
        addChar(tmpRoot, 137, 0x7fffda,   23);
        addChar(tmpRoot, 138, 0x7fffdb,   23);
        addChar(tmpRoot, 139, 0x7fffdc,   23);
        addChar(tmpRoot, 140, 0x7fffdd,   23);
        addChar(tmpRoot, 141, 0x7fffde,   23);
        addChar(tmpRoot, 142, 0xffffeb,   24);
        addChar(tmpRoot, 143, 0x7fffdf,   23);
        addChar(tmpRoot, 144, 0xffffec,   24);
        addChar(tmpRoot, 145, 0xffffed,   24);
        addChar(tmpRoot, 146, 0x3fffd7,   22);
        addChar(tmpRoot, 147, 0x7fffe0,   23);
        addChar(tmpRoot, 148, 0xffffee,   24);
        addChar(tmpRoot, 149, 0x7fffe1,   23);
        addChar(tmpRoot, 150, 0x7fffe2,   23);
        addChar(tmpRoot, 151, 0x7fffe3,   23);
        addChar(tmpRoot, 152, 0x7fffe4,   23);
        addChar(tmpRoot, 153, 0x1fffdc,   21);
        addChar(tmpRoot, 154, 0x3fffd8,   22);
        addChar(tmpRoot, 155, 0x7fffe5,   23);
        addChar(tmpRoot, 156, 0x3fffd9,   22);
        addChar(tmpRoot, 157, 0x7fffe6,   23);
        addChar(tmpRoot, 158, 0x7fffe7,   23);
        addChar(tmpRoot, 159, 0xffffef,   24);
        addChar(tmpRoot, 160, 0x3fffda,   22);
        addChar(tmpRoot, 161, 0x1fffdd,   21);
        addChar(tmpRoot, 162, 0xfffe9,    20);
        addChar(tmpRoot, 163, 0x3fffdb,   22);
        addChar(tmpRoot, 164, 0x3fffdc,   22);
        addChar(tmpRoot, 165, 0x7fffe8,   23);
        addChar(tmpRoot, 166, 0x7fffe9,   23);
        addChar(tmpRoot, 167, 0x1fffde,   21);
        addChar(tmpRoot, 168, 0x7fffea,   23);
        addChar(tmpRoot, 169, 0x3fffdd,   22);
        addChar(tmpRoot, 170, 0x3fffde,   22);
        addChar(tmpRoot, 171, 0xfffff0,   24);
        addChar(tmpRoot, 172, 0x1fffdf,   21);
        addChar(tmpRoot, 173, 0x3fffdf,   22);
        addChar(tmpRoot, 174, 0x7fffeb,   23);
        addChar(tmpRoot, 175, 0x7fffec,   23);
        addChar(tmpRoot, 176, 0x1fffe0,   21);
        addChar(tmpRoot, 177, 0x1fffe1,   21);
        addChar(tmpRoot, 178, 0x3fffe0,   22);
        addChar(tmpRoot, 179, 0x1fffe2,   21);
        addChar(tmpRoot, 180, 0x7fffed,   23);
        addChar(tmpRoot, 181, 0x3fffe1,   22);
        addChar(tmpRoot, 182, 0x7fffee,   23);
        addChar(tmpRoot, 183, 0x7fffef,   23);
        addChar(tmpRoot, 184, 0xfffea,    20);
        addChar(tmpRoot, 185, 0x3fffe2,   22);
        addChar(tmpRoot, 186, 0x3fffe3,   22);
        addChar(tmpRoot, 187, 0x3fffe4,   22);
        addChar(tmpRoot, 188, 0x7ffff0,   23);
        addChar(tmpRoot, 189, 0x3fffe5,   22);
        addChar(tmpRoot, 190, 0x3fffe6,   22);
        addChar(tmpRoot, 191, 0x7ffff1,   23);
        addChar(tmpRoot, 192, 0x3ffffe0,  26);
        addChar(tmpRoot, 193, 0x3ffffe1,  26);
        addChar(tmpRoot, 194, 0xfffeb,    20);
        addChar(tmpRoot, 195, 0x7fff1,    19);
        addChar(tmpRoot, 196, 0x3fffe7,   22);
        addChar(tmpRoot, 197, 0x7ffff2,   23);
        addChar(tmpRoot, 198, 0x3fffe8,   22);
        addChar(tmpRoot, 199, 0x1ffffec,  25);
        addChar(tmpRoot, 200, 0x3ffffe2,  26);
        addChar(tmpRoot, 201, 0x3ffffe3,  26);
        addChar(tmpRoot, 202, 0x3ffffe4,  26);
        addChar(tmpRoot, 203, 0x7ffffde,  27);
        addChar(tmpRoot, 204, 0x7ffffdf,  27);
        addChar(tmpRoot, 205, 0x3ffffe5,  26);
        addChar(tmpRoot, 206, 0xfffff1,   24);
        addChar(tmpRoot, 207, 0x1ffffed,  25);
        addChar(tmpRoot, 208, 0x7fff2,    19);
        addChar(tmpRoot, 209, 0x1fffe3,   21);
        addChar(tmpRoot, 210, 0x3ffffe6,  26);
        addChar(tmpRoot, 211, 0x7ffffe0,  27);
        addChar(tmpRoot, 212, 0x7ffffe1,  27);
        addChar(tmpRoot, 213, 0x3ffffe7,  26);
        addChar(tmpRoot, 214, 0x7ffffe2,  27);
        addChar(tmpRoot, 215, 0xfffff2,   24);
        addChar(tmpRoot, 216, 0x1fffe4,   21);
        addChar(tmpRoot, 217, 0x1fffe5,   21);
        addChar(tmpRoot, 218, 0x3ffffe8,  26);
        addChar(tmpRoot, 219, 0x3ffffe9,  26);
        addChar(tmpRoot, 220, 0xffffffd,  28);
        addChar(tmpRoot, 221, 0x7ffffe3,  27);
        addChar(tmpRoot, 222, 0x7ffffe4,  27);
        addChar(tmpRoot, 223, 0x7ffffe5,  27);
        addChar(tmpRoot, 224, 0xfffec,    20);
        addChar(tmpRoot, 225, 0xfffff3,   24);
        addChar(tmpRoot, 226, 0xfffed,    20);
        addChar(tmpRoot, 227, 0x1fffe6,   21);
        addChar(tmpRoot, 228, 0x3fffe9,   22);
        addChar(tmpRoot, 229, 0x1fffe7,   21);
        addChar(tmpRoot, 230, 0x1fffe8,   21);
        addChar(tmpRoot, 231, 0x7ffff3,   23);
        addChar(tmpRoot, 232, 0x3fffea,   22);
        addChar(tmpRoot, 233, 0x3fffeb,   22);
        addChar(tmpRoot, 234, 0x1ffffee,  25);
        addChar(tmpRoot, 235, 0x1ffffef,  25);
        addChar(tmpRoot, 236, 0xfffff4,   24);
        addChar(tmpRoot, 237, 0xfffff5,   24);
        addChar(tmpRoot, 238, 0x3ffffea,  26);
        addChar(tmpRoot, 239, 0x7ffff4,   23);
        addChar(tmpRoot, 240, 0x3ffffeb,  26);
        addChar(tmpRoot, 241, 0x7ffffe6,  27);
        addChar(tmpRoot, 242, 0x3ffffec,  26);
        addChar(tmpRoot, 243, 0x3ffffed,  26);
        addChar(tmpRoot, 244, 0x7ffffe7,  27);
        addChar(tmpRoot, 245, 0x7ffffe8,  27);
        addChar(tmpRoot, 246, 0x7ffffe9,  27);
        addChar(tmpRoot, 247, 0x7ffffea,  27);
        addChar(tmpRoot, 248, 0x7ffffeb,  27);
        addChar(tmpRoot, 249, 0xffffffe,  28);
        addChar(tmpRoot, 250, 0x7ffffec,  27);
        addChar(tmpRoot, 251, 0x7ffffed,  27);
        addChar(tmpRoot, 252, 0x7ffffee,  27);
        addChar(tmpRoot, 253, 0x7ffffef,  27);
        addChar(tmpRoot, 254, 0x7fffff0,  27);
        addChar(tmpRoot, 255, 0x3ffffee,  26);
        addEOS (tmpRoot, 256, EOS_LSB, EOS_LENGTH);

        // The difference in performance can always be checked by not using
        // the immutable trie:
        //     return tmpRoot;
        return ImmutableNode.copyOf(tmpRoot);
    }

    private QuickHuffman() { }

    private static void addChar(Node root, int symbol, int code, int bitLength)
    {
        addLeaf(root, (char) symbol, code, bitLength, false);
        long value = ((long) code) << (64 - bitLength); // re-align MSB <- LSB
        codes[symbol] = value | bitLength;
    }

    private static void addEOS(Node root, int symbol, int code, int bitLength)
    {
        addLeaf(root, (char) symbol, code, bitLength, true);
    }

    private static void addLeaf(Node root,
                                char symbol,
                                int code,
                                int bitLength,
                                boolean isEOS)
    {
        assert 0 < bitLength && bitLength <= 32 : bitLength;
        Node curr = root;
        int nBytes = bytesForBits(bitLength);
        // The number of bits the code needs to be shifted to the left in order
        // to align with the byte #nBytes boundary:
        int align = (nBytes << 3) - bitLength;
        code <<= align;
        // descend the trie until the last element
        int l = 0;
        for (int i = 0, probe = 0xff << ((nBytes - 1) << 3);
             i < nBytes - 1;
             i++, probe >>>= 8)
        {
            curr.setEOSPath(curr.isEOSPath() | isEOS);
            int idx = (code & probe) >>> ((nBytes - 1 - i) << 3);
            curr = curr.getOrCreateChild(idx);
            curr.setLength(8);
            l += 8;
        }
        // Assign the same char to all byte variants. For example, if the code
        // and its length are 00011b and 5 respectively (letter 'a') then, in
        // order to be able to match any byte starting with 00011b prefix,
        // the following nodes need to be created:
        //
        //     00011000b, 00011001b, 00011010b, 00011011b, 00011100b, 00011101b,
        //     00011110b and 00011111b
        int idx = code & 0xff;
        curr.setEOSPath(curr.isEOSPath() | isEOS);
        for (int i = 0; i < (1 << align); i++) {
            Node child = curr.getOrCreateChild(idx | i);
            child.setSymbol(symbol);
            child.setEOSPath(child.isEOSPath() | isEOS);
            child.setLength(bitLength - l);
        }
    }

    /*
     * A node in the Huffman trie.
     */
    interface Node {

        boolean isEOSPath();

        void setEOSPath(boolean value);

        boolean isLeaf();

        Node getChild(int index);

        Node getOrCreateChild(int index);

        Node[] getChildren();

        char getSymbol();

        void setSymbol(char symbol);

        int getLength();

        void setLength(int value);
    }

    /*
     * Mutable nodes used for initial construction of the Huffman trie.
     * (These nodes are perfectly ok to be used after that.)
     */
    static final class TemporaryNode implements Node {

        private char symbol;
        private boolean eosPath;
        private TemporaryNode[] children;
        private int length;

        @Override
        public TemporaryNode getOrCreateChild(int index) {
            ensureChildrenExist();
            if (children[index] == null) {
                children[index] = new TemporaryNode();
            }
            return children[index];
        }

        private void ensureChildrenExist() {
            if (children == null) {
                children = new TemporaryNode[256];
            }
        }

        @Override
        public boolean isLeaf() {
            return children == null;
        }

        @Override
        public boolean isEOSPath() {
            return eosPath;
        }

        @Override
        public void setEOSPath(boolean value) {
            eosPath = value;
        }

        @Override
        public TemporaryNode getChild(int index) {
            ensureChildrenExist();
            return children[index];
        }

        @Override
        public Node[] getChildren() {
            if (children == null) {
                return new Node[0];
            }
            return children;
        }

        @Override
        public char getSymbol() {
            return symbol;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public void setSymbol(char value) {
            this.symbol = value;
        }

        @Override
        public void setLength(int value) {
            this.length = value;
        }
    }

    /*
     * Immutable node used to construct traversal-only Huffman trie.
     *
     * Once the trie has been built, the support of modifications is no longer
     * required. An immutable trie should be used. Not only it will help to
     * catch possible bugs, but hopefully speedup the traversal operations.
     */
    static final class ImmutableNode implements Node {

        private final char symbol;
        private final boolean eosPath;
        private final int length;
        private final List<ImmutableNode> children;

        public static ImmutableNode copyOf(Node node) {
            if (node.isLeaf()) {
                return new ImmutableNode(node.getSymbol(), node.isEOSPath(),
                                         node.getLength());
            }
            Node[] children = node.getChildren();
            ImmutableNode[] immutableChildren = new ImmutableNode[children.length];
            for (int i = 0; i < children.length; i++) {
                immutableChildren[i] = copyOf(children[i]);
            }
            return new ImmutableNode(node.isEOSPath(), node.getLength(),
                                     immutableChildren);
        }

        /* Creates a leaf node */
        private ImmutableNode(char symbol,
                              boolean eosPath,
                              int length) {
            this.symbol = symbol;
            this.eosPath = eosPath;
            this.length = length;
            this.children = List.of();
        }

        /* Creates a node with children */
        private ImmutableNode(boolean eosPath,
                              int length,
                              ImmutableNode[] children)
        {
            this.symbol = 0;
            this.eosPath = eosPath;
            this.length = length;
            if (children.length == 0) {
                throw new IllegalArgumentException();
            }
            // A list produced by List.of should not be slower than array for
            // accessing elements by index, and hopefully use additional
            // optimizations (e.g. jdk.internal.vm.annotation.Stable)
            this.children = List.of(children);
        }

        @Override
        public boolean isLeaf() {
            return children.isEmpty();
        }

        @Override
        public boolean isEOSPath() {
            return eosPath;
        }

        @Override
        public void setEOSPath(boolean value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ImmutableNode getChild(int index) {
            return children.get(index);
        }

        @Override
        public ImmutableNode getOrCreateChild(int index) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ImmutableNode[] getChildren() {
            // This method is not expected to be called on an immutable node.
            // If it is called, it requires some investigation.
            throw new UnsupportedOperationException();
        }

        @Override
        public char getSymbol() {
            return symbol;
        }

        @Override
        public void setSymbol(char symbol) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public void setLength(int value) {
            throw new UnsupportedOperationException();
        }
    }

    static final class Reader implements Huffman.Reader {

        private final BufferUpdateConsumer UPDATER =
                (buf, bufLen) -> {
                    buffer = buf;
                    bufferLen = bufLen;
                };

        private Node curr = root;  // current position in the trie
        private long buffer;       // bits left from the previous match (aligned to the left, or MSB)
        private int bufferLen;     // number of bits in the buffer
        private int len;           // length (in bits) of path to curr
        private boolean done;

        @Override
        public void read(ByteBuffer source,
                         Appendable destination,
                         boolean isLast) throws IOException
        {
            while (!done) {
                int remaining = source.remaining();
                int nBytes = HPACK.read(source, buffer, bufferLen, UPDATER);
                // write as much as possible
                while (true) {
                    if (bufferLen < 8) {
                        if (nBytes < remaining) { // read again
                            break;
                        } else if (!isLast) { // exit the method to accept more input
                            return;
                        } else if (bufferLen > 0) { // no more data is expected, pad
                                                    // (this padding may be done more than once)
                            buffer |= ((0xff00000000000000L >>> bufferLen)
                                    & 0xff00000000000000L);
                            // do not update bufferLen, since all those ones are
                            // synthetic and are appended merely for the sake of
                            // lookup
                        } else {
                            done = true;
                            break;
                        }
                    }
                    int idx = (int) (buffer >>> 56);
                    Node node = curr.getChild(idx);
                    if (node == null) { // TODO: TEST
                        throw new IOException("Unexpected byte");
                    }
                    if (node.isLeaf()) {
                        if (node.getLength() > bufferLen) { // matched more than we actually could (because of padding)
                            throw new IOException(
                                    "Not a EOS prefix padding or unexpected end of data");
                        }
                        if (node.isEOSPath()) {
                            throw new IOException("Encountered EOS");
                        }
                        destination.append(node.getSymbol());
                        curr = root;
                        len = 0;
                    } else {
                        curr = node;
                        // because of the padding, we can't match more bits than
                        // there are currently in the buffer
                        len += Math.min(bufferLen, node.getLength());
                    }
                    buffer <<= node.getLength();
                    bufferLen -= node.getLength();
                }
                if (done && (curr.isEOSPath() && len > 7)) {
                    throw new IOException(
                            "Padding is too long (len=" + len + ") "
                                    + "or unexpected end of data");
                }
            }
        }

        @Override
        public void reset() {
            curr = root;
            len = 0;
            buffer = 0;
            bufferLen = 0;
            done = false;
        }
    }

    static final class Writer implements Huffman.Writer {

        private final BufferUpdateConsumer UPDATER =
                (buf, bufLen) -> {
                    buffer = buf;
                    bufferLen = bufLen;
                };

        private CharSequence source;
        private boolean padded;
        private int pos;
        private int end;
        private long buffer;
        private int bufferLen;

        @Override
        public QuickHuffman.Writer from(CharSequence input, int start, int end) {
            Objects.checkFromToIndex(start, end, input.length());
            this.pos = start;
            this.end = end;
            this.source = input;
            return this;
        }

        @Override
        public boolean write(ByteBuffer destination) {
            while (true) {
                while (true) { // stuff codes into long
                    if (pos >= end) {
                        break;
                    }
                    char c = source.charAt(pos);
                    if (c > 255) {
                        throw new IllegalArgumentException("char=" + ((int) c));
                    }
                    long len = codeLengthOf(c);
                    if (bufferLen + len <= 64) {
                        buffer |= (codeValueOf(c) >>> bufferLen); // append
                        bufferLen += len;
                        pos++;
                    } else {
                        break;
                    }
                }
                if (bufferLen == 0) {
                    return true;
                }
                if (pos >= end && !padded) { // no more input chars are expected, pad
                    padded = true;
                    // A long shift to 64 will result in the same long, not
                    // necessarily 0L. In which case padding will be performed
                    // incorrectly. If bufferLen is equal to 64, the shift (and
                    // padding) in not performed.
                    // (see https://docs.oracle.com/javase/specs/jls/se10/html/jls-15.html#jls-15.19)
                    if (bufferLen != 64) {
                        buffer |= (EOS_MSB >>> bufferLen);
                        bufferLen = bytesForBits(bufferLen) << 3;
                    }
                }
                int nBytes = HPACK.write(buffer, bufferLen, UPDATER, destination);
                if (nBytes == 0) {
                    return false;
                }
            }
        }

        @Override
        public QuickHuffman.Writer reset() {
            source = null;
            buffer = 0;
            bufferLen = 0;
            end = 0;
            pos = 0;
            padded = false;
            return this;
        }

        @Override
        public int lengthOf(CharSequence value, int start, int end) {
            int len = 0;
            for (int i = start; i < end; i++) {
                char c = value.charAt(i);
                len += codeLengthOf(c);
            }
            return bytesForBits(len);
        }
    }
}
