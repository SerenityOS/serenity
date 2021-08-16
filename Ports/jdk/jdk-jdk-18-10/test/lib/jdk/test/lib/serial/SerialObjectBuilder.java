/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.test.lib.serial;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.Map;
import static java.io.ObjectStreamConstants.*;

/**
 * A basic builder of a serial object.
 */
public class SerialObjectBuilder {

    private final ObjectOutputStream objectOutputStream;
    private final ByteArrayOutputStream byteArrayOutputStream;

    private record NameAndType<T>(String name, Class<T>type) { }

    private String className;
    private long suid;
    private SerialObjectBuilder superClass;
    private final LinkedHashMap<NameAndType<?>, Object> primFields = new LinkedHashMap<>();
    private final LinkedHashMap<NameAndType<?>, Object> objectFields = new LinkedHashMap<>();

    private SerialObjectBuilder() {
        try {
            byteArrayOutputStream = new ByteArrayOutputStream();
            objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static SerialObjectBuilder newBuilder(String className) {
        return (new SerialObjectBuilder()).className(className);
    }

    private SerialObjectBuilder className(String className) {
        this.className = className;
        return this;
    }

    public SerialObjectBuilder suid(long suid) {
        this.suid = suid;
        return this;
    }

    public SerialObjectBuilder superClass(SerialObjectBuilder superClass) {
        this.superClass = superClass;
        return this;
    }

    public <T> SerialObjectBuilder addPrimitiveField(String name, Class<T> type, T value) {
        if (!type.isPrimitive())
            throw new IllegalArgumentException("Unexpected non-primitive field: " + type);
        primFields.put(new NameAndType<>(name, type), value);
        return this;
    }

    public <T> SerialObjectBuilder addField(String name, Class<T> type, T value) {
        if (type.isPrimitive())
            throw new IllegalArgumentException("Unexpected primitive field: " + type);
        objectFields.put(new NameAndType<>(name, type), value);
        return this;
    }

    private static void writeUTF(DataOutputStream out, String str) throws IOException {
        assert str.codePoints().noneMatch(cp -> cp > 127); // only ASCII for now
        int utflen = str.length();
        assert utflen <= 0xFFFF;  // only small strings for now
        out.writeShort(utflen);
        out.writeBytes(str);
    }

    private void writePrimFieldsDesc(DataOutputStream out) throws IOException {
        for (Map.Entry<NameAndType<?>, Object> entry : primFields.entrySet()) {
            Class<?> primClass = entry.getKey().type();
            assert primClass.isPrimitive();
            assert primClass != void.class;
            out.writeByte(primClass.descriptorString().getBytes()[0]);   // prim_typecode
            out.writeUTF(entry.getKey().name());                         // fieldName
        }
    }

    private void writePrimFieldsValues(DataOutputStream out) throws IOException {
        for (Map.Entry<NameAndType<?>, Object> entry : primFields.entrySet()) {
            Class<?> cl = entry.getKey().type();
            Object value = entry.getValue();
            if (cl == Integer.TYPE) out.writeInt((int) value);
            else if (cl == Byte.TYPE) out.writeByte((byte) value);
            else if (cl == Long.TYPE) out.writeLong((long) value);
            else if (cl == Float.TYPE) out.writeFloat((float) value);
            else if (cl == Double.TYPE) out.writeDouble((double) value);
            else if (cl == Short.TYPE) out.writeShort((short) value);
            else if (cl == Character.TYPE) out.writeChar((char) value);
            else if (cl == Boolean.TYPE) out.writeBoolean((boolean) value);
            else throw new InternalError();
        }
    }

    private void writeObjectFieldDesc(DataOutputStream out) throws IOException {
        for (Map.Entry<NameAndType<?>, Object> entry : objectFields.entrySet()) {
            Class<?> cl = entry.getKey().type();
            assert !cl.isPrimitive();
            // obj_typecode
            if (cl.isArray()) {
                out.writeByte('[');
            } else {
                out.writeByte('L');
            }
            writeUTF(out, entry.getKey().name());
            out.writeByte(TC_STRING);
            writeUTF(out, cl.descriptorString());
        }
    }

    private void writeObject(DataOutputStream out, Object value) throws IOException {
        objectOutputStream.reset();
        byteArrayOutputStream.reset();
        objectOutputStream.writeUnshared(value);
        out.write(byteArrayOutputStream.toByteArray());
    }

    private void writeObjectFieldValues(DataOutputStream out) throws IOException {
        for (Map.Entry<NameAndType<?>, Object> entry : objectFields.entrySet()) {
            Class<?> cl = entry.getKey().type();
            assert !cl.isPrimitive();
            if (cl == String.class) {
                out.writeByte(TC_STRING);
                writeUTF(out, (String) entry.getValue());
            } else {
                writeObject(out, entry.getValue());
            }
        }
    }

    private int numFields() {
        return primFields.size() + objectFields.size();
    }

    private static void writeClassDesc(DataOutputStream dos,
                                       SerialObjectBuilder sb)
        throws IOException
    {
        dos.writeByte(TC_CLASSDESC);
        dos.writeUTF(sb.className);
        dos.writeLong(sb.suid);
        dos.writeByte(SC_SERIALIZABLE);
        dos.writeShort(sb.numFields());      // number of fields
        sb.writePrimFieldsDesc(dos);
        sb.writeObjectFieldDesc(dos);
        dos.writeByte(TC_ENDBLOCKDATA);   // no annotations
        if (sb.superClass == null) {
            dos.writeByte(TC_NULL);       // no superclasses
        } else {
            writeClassDesc(dos, sb.superClass);
        }
    }

    public byte[] build() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeShort(STREAM_MAGIC);
            dos.writeShort(STREAM_VERSION);
            dos.writeByte(TC_OBJECT);
            writeClassDesc(dos, this);
            if (superClass != null) {
                superClass.writePrimFieldsValues(dos);
                superClass.writeObjectFieldValues(dos);
            }
            writePrimFieldsValues(dos);
            writeObjectFieldValues(dos);
            dos.close();
            return baos.toByteArray();
        } catch (IOException unexpected) {
            throw new AssertionError(unexpected);
        }
    }
}
