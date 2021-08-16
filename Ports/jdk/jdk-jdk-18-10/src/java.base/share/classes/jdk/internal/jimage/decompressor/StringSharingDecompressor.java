/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
package jdk.internal.jimage.decompressor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 *
 * A Decompressor that reconstructs the constant pool of classes.
 *
 * @implNote This class needs to maintain JDK 8 source compatibility.
 *
 * It is used internally in the JDK to implement jimage/jrtfs access,
 * but also compiled and delivered as part of the jrtfs.jar to support access
 * to the jimage file provided by the shipped JDK by tools running on JDK 8.
 */
public class StringSharingDecompressor implements ResourceDecompressor {

    public static final int EXTERNALIZED_STRING = 23;
    public static final int EXTERNALIZED_STRING_DESCRIPTOR = 25;

    private static final int CONSTANT_Utf8 = 1;
    private static final int CONSTANT_Integer = 3;
    private static final int CONSTANT_Float = 4;
    private static final int CONSTANT_Long = 5;
    private static final int CONSTANT_Double = 6;
    private static final int CONSTANT_Class = 7;
    private static final int CONSTANT_String = 8;
    private static final int CONSTANT_Fieldref = 9;
    private static final int CONSTANT_Methodref = 10;
    private static final int CONSTANT_InterfaceMethodref = 11;
    private static final int CONSTANT_NameAndType = 12;
    private static final int CONSTANT_MethodHandle = 15;
    private static final int CONSTANT_MethodType = 16;
    private static final int CONSTANT_InvokeDynamic = 18;
    private static final int CONSTANT_Module = 19;
    private static final int CONSTANT_Package = 20;

    private static final int[] SIZES = new int[21];

    static {

        //SIZES[CONSTANT_Utf8] = XXX;
        SIZES[CONSTANT_Integer] = 4;
        SIZES[CONSTANT_Float] = 4;
        SIZES[CONSTANT_Long] = 8;
        SIZES[CONSTANT_Double] = 8;
        SIZES[CONSTANT_Class] = 2;
        SIZES[CONSTANT_String] = 2;
        SIZES[CONSTANT_Fieldref] = 4;
        SIZES[CONSTANT_Methodref] = 4;
        SIZES[CONSTANT_InterfaceMethodref] = 4;
        SIZES[CONSTANT_NameAndType] = 4;
        SIZES[CONSTANT_MethodHandle] = 3;
        SIZES[CONSTANT_MethodType] = 2;
        SIZES[CONSTANT_InvokeDynamic] = 4;
        SIZES[CONSTANT_Module] = 2;
        SIZES[CONSTANT_Package] = 2;
    }

    public static int[] getSizes() {
        return SIZES.clone();
    }

    @SuppressWarnings("fallthrough")
    public static byte[] normalize(StringsProvider provider, byte[] transformed,
            int offset) throws IOException {
        DataInputStream stream = new DataInputStream(new ByteArrayInputStream(transformed,
                offset, transformed.length - offset));
        ByteArrayOutputStream outStream = new ByteArrayOutputStream(transformed.length);
        DataOutputStream out = new DataOutputStream(outStream);
        byte[] header = new byte[8]; //maginc/4, minor/2, major/2
        stream.readFully(header);
        out.write(header);
        int count = stream.readUnsignedShort();
        out.writeShort(count);
        for (int i = 1; i < count; i++) {
            int tag = stream.readUnsignedByte();
            byte[] arr;
            switch (tag) {
                case CONSTANT_Utf8: {
                    out.write(tag);
                    String utf = stream.readUTF();
                    out.writeUTF(utf);
                    break;
                }

                case EXTERNALIZED_STRING: {
                    int index = CompressIndexes.readInt(stream);
                    String orig = provider.getString(index);
                    out.write(CONSTANT_Utf8);
                    out.writeUTF(orig);
                    break;
                }

                case EXTERNALIZED_STRING_DESCRIPTOR: {
                    String orig = reconstruct(provider, stream);
                    out.write(CONSTANT_Utf8);
                    out.writeUTF(orig);
                    break;
                }
                case CONSTANT_Long:
                case CONSTANT_Double: {
                    i++;
                }
                default: {
                    out.write(tag);
                    int size = SIZES[tag];
                    arr = new byte[size];
                    stream.readFully(arr);
                    out.write(arr);
                }
            }
        }
        out.write(transformed, transformed.length - stream.available(),
                stream.available());
        out.flush();

        return outStream.toByteArray();
    }

    private static String reconstruct(StringsProvider reader, DataInputStream cr)
            throws IOException {
        int descIndex = CompressIndexes.readInt(cr);
        String desc = reader.getString(descIndex);
        byte[] encodedDesc = getEncoded(desc);
        int indexes_length = CompressIndexes.readInt(cr);
        byte[] bytes = new byte[indexes_length];
        cr.readFully(bytes);
        List<Integer> indices = CompressIndexes.decompressFlow(bytes);
        ByteBuffer buffer = ByteBuffer.allocate(encodedDesc.length * 2);
        buffer.order(ByteOrder.BIG_ENDIAN);
        int argIndex = 0;
        for (byte c : encodedDesc) {
            if (c == 'L') {
                buffer = safeAdd(buffer, c);
                int index = indices.get(argIndex);
                argIndex += 1;
                String pkg = reader.getString(index);
                if (!pkg.isEmpty()) {
                    pkg = pkg + "/";
                    byte[] encoded = getEncoded(pkg);
                    buffer = safeAdd(buffer, encoded);
                }
                int classIndex = indices.get(argIndex);
                argIndex += 1;
                String clazz = reader.getString(classIndex);
                byte[] encoded = getEncoded(clazz);
                buffer = safeAdd(buffer, encoded);
            } else {
                buffer = safeAdd(buffer, c);
            }
        }

        byte[] encoded = buffer.array();
        ByteBuffer result = ByteBuffer.allocate(encoded.length + 2);
        result.order(ByteOrder.BIG_ENDIAN);
        result.putShort((short) buffer.position());
        result.put(encoded, 0, buffer.position());
        ByteArrayInputStream stream = new ByteArrayInputStream(result.array());
        DataInputStream inStream = new DataInputStream(stream);
        String str = inStream.readUTF();
        return str;
    }

    public static byte[] getEncoded(String pre) throws IOException {
        ByteArrayOutputStream resultStream = new ByteArrayOutputStream();
        DataOutputStream resultOut = new DataOutputStream(resultStream);
        resultOut.writeUTF(pre);
        byte[] content = resultStream.toByteArray();
        // first 2 items are length;
        if (content.length <= 2) {
            return new byte[0];
        }
        return Arrays.copyOfRange(content, 2, content.length);
    }

    private static ByteBuffer safeAdd(ByteBuffer current, byte b) {
        byte[] bytes = {b};
        return safeAdd(current, bytes);
    }

    private static ByteBuffer safeAdd(ByteBuffer current, byte[] bytes) {
        if (current.remaining() < bytes.length) {
            ByteBuffer newBuffer = ByteBuffer.allocate((current.capacity()
                    + bytes.length) * 2);
            newBuffer.order(ByteOrder.BIG_ENDIAN);
            newBuffer.put(current.array(), 0, current.position());
            current = newBuffer;
        }
        current.put(bytes);
        return current;
    }

    @Override
    public String getName() {
        return StringSharingDecompressorFactory.NAME;
    }

    public StringSharingDecompressor(Properties properties) {

    }

    @Override
    public byte[] decompress(StringsProvider reader, byte[] content,
            int offset, long originalSize) throws Exception {
        return normalize(reader, content, offset);
    }
}
