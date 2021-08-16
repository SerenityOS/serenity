/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
package jdk.test.lib.hexdump;

import java.io.CharArrayWriter;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.InputStream;
import java.io.IOException;
import java.io.ObjectStreamConstants;
import java.io.PrintWriter;
import java.io.UTFDataFormatException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.BiConsumer;

import static java.io.ObjectStreamConstants.STREAM_MAGIC;
import static java.io.ObjectStreamConstants.TC_BLOCKDATA;
import static java.io.ObjectStreamConstants.TC_BLOCKDATALONG;
import static java.io.ObjectStreamConstants.TC_ENDBLOCKDATA;
import static java.io.ObjectStreamConstants.TC_MAX;
import static java.io.ObjectStreamConstants.TC_NULL;
import static java.io.ObjectStreamConstants.baseWireHandle;

/**
 * Object Serialization stream annotation printer.
 * On the first call, the stream header is expected, raw bytes
 * are printed until the header is found.
 * Each call decodes a hierarchy rooted at the first typecode.
 * Unrecognized bytes are read and printed.
 */
public class ObjectStreamPrinter implements HexPrinter.Formatter {

    private int nextHandle = 0;
    private List<Handle> labels = new ArrayList<>();

    /**
     * Returns an ObjectStreamPrinter.
     * @return an ObjectStreamPrinter
     */
    public static ObjectStreamPrinter formatter() {
        return new ObjectStreamPrinter();
    }

    /**
     * Create a ObjectStreamPrinter.
     */
    private ObjectStreamPrinter() {
    }

    /**
     * Read bytes from the stream and annotate the stream as a
     * ObjectInputStream. Each call to convert() reads an object.
     * Before the first object is read, the Stream header is expected and annotated.
     * Unrecognized bytes are printed as decimal values.
     *
     * @param in  a DataInputStream
     * @return a string representation of the ObjectInputStream
     */
    public String annotate(DataInputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        try {
            this.annotate(in, sb);
        } catch (IOException e) {
            // ignore the exception so the accumulated output can be returned
        }
        return sb.toString();
    }

    /**
     * Read bytes from the stream and annotate the stream as a
     * ObjectInputStream. Each call to convert() reads an object.
     * Before the first object is read, the Stream header is expected
     * and annotated.
     * Unrecognized bytes are printed as decimal values.
     *
     * @param in  a DataInputStream
     * @param out an Appendable for the output
     */
    @Override
    public void annotate(DataInputStream in, Appendable out) throws IOException {
        annotate(in, out, 0);
    }

    /**
     * Read bytes from the stream and annotate the stream as a
     * ObjectInputStream. Each call to convert() reads an object.
     * The Stream header is expected and annotated.
     * Unrecognized bytes are printed as decimal values.
     *
     * @param in  a DataInputStream
     * @param out an Appendable for the output
     * @param indent indentation level
     */
    public void annotate(DataInputStream in, Appendable out, int indent) throws IOException {
        // Read and format a single object, if its the header format another
        if (formatObject(in, out, indent).equals(HEADER_HANDLE))
            formatObject(in, out, indent);
    }

    /**
     * Read and format objects in the stream based on the type code.
     * Unrecognized type codes are printed in decimal.
     *
     * @param in input stream
     * @param infoOut output stream
     * @param indent indentation level
     * @return a label for the object just read
     * @throws IOException if an error occurs on the stream
     */
    Handle formatObject(DataInputStream in, Appendable infoOut, int indent) throws IOException {
        int tc;
        if (((tc = in.read()) < TC_NULL || tc > TC_MAX) && tc != ((STREAM_MAGIC >>> 8) & 0xff)) {
            if (tc < 0)
                throw new EOFException();
            infoOut.append("raw: [ " + tc + " ");
            while ((tc = in.read()) >= 0 && (tc < TC_NULL || tc > TC_MAX)) {
                infoOut.append(tc + " ");
            }
            infoOut.append("] ");
            if (tc < 0)
                throw new EOFException();
        }
        switch (tc) {
            case TC_NULL:
                return formatTC_NULL(in, infoOut);
            case ObjectStreamConstants.TC_REFERENCE:
                return formatTC_REFERENCE(in, infoOut);
            case ObjectStreamConstants.TC_CLASSDESC:
                return formatTC_CLASSDESC(in, infoOut, indent);
            case ObjectStreamConstants.TC_OBJECT:
                return formatTC_OBJECT(in, infoOut, indent);
            case ObjectStreamConstants.TC_STRING:
                return formatTC_STRING(in, infoOut, indent);
            case ObjectStreamConstants.TC_ARRAY:
                return formatTC_ARRAY(in, infoOut, indent);
            case ObjectStreamConstants.TC_CLASS:
                return formatTC_CLASS(in, infoOut, indent);
            case ObjectStreamConstants.TC_BLOCKDATA:
                return formatTC_BLOCKDATA(in, infoOut);
            case ObjectStreamConstants.TC_ENDBLOCKDATA:
                return formatTC_ENDBLOCKDATA(in, infoOut);
            case ObjectStreamConstants.TC_RESET:
                return formatTC_RESET(in, infoOut);
            case ObjectStreamConstants.TC_BLOCKDATALONG:
                return formatTC_BLOCKDATALONG(in, infoOut);
            case ObjectStreamConstants.TC_EXCEPTION:
                return formatTC_EXCEPTION(in, infoOut);
            case ObjectStreamConstants.TC_LONGSTRING:
                return formatTC_LONGSTRING(in, infoOut);
            case ObjectStreamConstants.TC_PROXYCLASSDESC:
                return formatTC_PROXYCLASSDESC(in, infoOut, indent);
            case ObjectStreamConstants.TC_ENUM:
                return formatTC_ENUM(in, infoOut, indent);
            case (STREAM_MAGIC >>> 8) & 0xff:
                return formatSTREAM_MAGIC(in, infoOut, indent);
            default:
                infoOut.append("data: " + tc + ' ');
                return EMPTY_HANDLE;
        }
    }

    /**
     * Read and print a UTF string tagged as a string.
     *
     * @param in input stream
     * @param infoOut output stream
     * @param indent indentation level
     * @return a label for the object just read
     * @throws IOException if an error occurs on the stream
     */
    private Handle formatTC_STRING(DataInputStream in, Appendable infoOut, int indent)
            throws IOException {
        String s = in.readUTF();
        Handle handle = new Handle(s);
        setLabel(nextHandle, handle);
        infoOut.append(String.format("STRING #%d \"%s\" ", nextHandle++, s));
        return handle;
    }

    /**
     * Read and print a long UTF string.
     *
     * @param in input stream
     * @param infoOut output stream
     * @return a label for the object just read
     * @throws IOException if an error occurs on the stream
     */
    private Handle formatTC_LONGSTRING(DataInputStream in, Appendable infoOut) throws IOException {
        long utflen = in.readLong();
        String s = "longstring " + nextHandle + ",len: " + utflen;
        Handle handle = new Handle(s);
        setLabel(nextHandle, handle);
        infoOut.append(String.format("LONGSTRING #%d \"", nextHandle++));

        long count = 0;
        while (count < utflen) {
            int c = (char)(in.readUnsignedByte() & 0xff);
            int char2, char3;
            switch (c >> 4) {
                case 0: case 1: case 2: case 3: case 4: case 5: case 6: case 7:
                    /* 0xxxxxxx*/
                    count++;
                    break;
                case 12: case 13:
                    /* 110x xxxx   10xx xxxx*/
                    count += 2;
                    if (count > utflen)
                        throw new UTFDataFormatException(
                                "malformed input: partial character at end");
                    char2 = in.readUnsignedByte() & 0xff;
                    if ((char2 & 0xC0) != 0x80)
                        throw new UTFDataFormatException(
                                "malformed input around byte " + count);
                    c = (char)(((c & 0x1F) << 6) | (char2 & 0x3F));
                    break;
                case 14:
                    /* 1110 xxxx  10xx xxxx  10xx xxxx */
                    count += 3;
                    if (count > utflen)
                        throw new UTFDataFormatException(
                                "malformed input: partial character at end");
                    char2 = in.readUnsignedByte() & 0xff;
                    char3 = in.readUnsignedByte() & 0xff;
                    if (((char2 & 0xC0) != 0x80) || ((char3 & 0xC0) != 0x80))
                        throw new UTFDataFormatException(
                                "malformed input around byte " + (count-1));
                    c = (char)(((c     & 0x0F) << 12) |
                            ((char2 & 0x3F) << 6)  |
                            ((char3 & 0x3F) << 0));
                    break;
                default:
                    /* 10xx xxxx,  1111 xxxx */
                    throw new UTFDataFormatException(
                            "malformed input around byte " + count);
            }
            infoOut.append((char)c);
        }
        infoOut.append("\" ");
        return handle;
    }

    /**
     * Format a null.
     *
     * @param in input stream
     * @param infoOut output stream
     * @return a label for the object just read
     * @throws IOException if an error occurs on the stream
     */
    private Handle formatTC_NULL(DataInputStream in, Appendable infoOut) throws IOException {
        infoOut.append("NULL; ");
        return NULL_HANDLE;
    }

    /**
     * Read and format a reference to a previously read object.
     *
     * @param in input stream
     * @param infoOut output stream
     * @return a label for the object just read
     * @throws IOException if an error occurs on the stream
     */
    private Handle formatTC_REFERENCE(DataInputStream in, Appendable infoOut) throws IOException {
        int offset = in.readInt();
        int h = offset - baseWireHandle;
        Handle handle = getHandle(h);
        infoOut.append("REF #" + h + ' ' + getLabel(h) + ' ');
        return handle;
    }

    /**
     * Read and format a class descriptor.
     *
     * @param in input stream
     * @param infoOut output stream
     * @return a label for the object just read
     * @throws IOException if an error occurs on the stream
     */
    private Handle formatTC_CLASSDESC(DataInputStream in, Appendable infoOut, int indent) throws IOException {
        char[] buf = new char[1];       // buffer for 1 char type names
        String name = in.readUTF();
        ClassHandle handle = new ClassHandle(name);
        setLabel(nextHandle, handle);
        infoOut.append(String.format("CLASSDESC #%d %s", nextHandle++, name));
        newlineIndent(infoOut, indent + 2);
        long svid = in.readLong();
        infoOut.append(String.format("svid: %dL", svid));
        newlineIndent(infoOut, indent + 2);
        int flags = in.readUnsignedByte();
        handle.setFlags(flags);
        StringJoiner flagsJoiner = new StringJoiner(", ");
        if (handle.hasFlag(ObjectStreamConstants.SC_WRITE_METHOD)) flagsJoiner.add("WRITE_OBJECT");
        if (handle.hasFlag(ObjectStreamConstants.SC_SERIALIZABLE)) flagsJoiner.add("SERIALIZABLE");
        if (handle.hasFlag(ObjectStreamConstants.SC_EXTERNALIZABLE)) flagsJoiner.add("EXTERNALIZABLE");
        if (handle.hasFlag(ObjectStreamConstants.SC_BLOCK_DATA)) flagsJoiner.add("BLOCK_DATA");
        if (handle.hasFlag(ObjectStreamConstants.SC_ENUM)) flagsJoiner.add("ENUM");
        infoOut.append(String.format("flags: %s", flagsJoiner.toString()));

        newlineIndent(infoOut, indent + 2);
        int numFields = in.readShort();
        {   // Append list of fields
            infoOut.append(String.format("%d field(s) {", numFields));
            CharArrayWriter caw = new CharArrayWriter();
            for (int i = 0; i < numFields; i++) {
                newlineIndent(infoOut, indent + 4);
                buf[0] = (char) in.readByte();
                String fname = in.readUTF();
                caw.write(buf[0]);
                caw.write(' ');
                caw.write(fname);
                caw.write(' ');
                String typeName;
                if ((buf[0] == 'L') || (buf[0] == '[')) {
                    typeName = formatObject(in, caw, 0).toString();
                } else {
                    typeName = new String(buf);
                    caw.write(mapClassName(typeName).toString());
                }
                caw.write("; ");
                infoOut.append(caw.toString());
                caw.reset();
                handle.addField(fname, typeName);   // Add field to handle
            }
            if (numFields > 0)
                newlineIndent(infoOut, indent + 2);
            infoOut.append("} ");
        }
        skipCustomData(in, infoOut, indent);
        newlineIndent(infoOut, indent + 2);
        infoOut.append("Super: ");
        Handle sup = formatObject(in, infoOut, indent);
        if (sup instanceof ClassHandle) {
            handle.superClass((ClassHandle)sup);
        }
        return handle;
    }

    private Appendable newlineIndent(Appendable infoOut, int indent) throws IOException {
        return infoOut.append(System.lineSeparator()).append(" ".repeat(indent));
    }

    /**
     * Read and format custom data until end of block data.
     *
     * @param in input stream
     * @param infoOut output stream
     * @throws IOException if an error occurs on the stream
     */
    private void skipCustomData(DataInputStream in, Appendable infoOut, int indent) throws IOException {
        while (!formatObject(in, infoOut, indent).equals(END_HANDLE)) {
            // ignore anything but ENDBLOCK
        }
    }

    /**
     * Read and format a proxy class descriptor.
     *
     * @param in input stream
     * @param infoOut output stream
     * @return a label for the object just read
     * @throws IOException if an error occurs on the stream
     */
    private Handle formatTC_PROXYCLASSDESC(DataInputStream in, Appendable infoOut, int indent) throws IOException {
        int numIfaces = in.readInt();
        setLabel(nextHandle, CLASS_HANDLE);
        infoOut.append(String.format("PROXYCLASSDESC #%d %d ",
                nextHandle++, numIfaces));
        if (numIfaces > 0) {
            CharArrayWriter caw = new CharArrayWriter();
            PrintWriter pw = new PrintWriter(caw);
            pw.append("{");
            String delim = "";
            for (int i = 0; i < numIfaces; i++, delim = ", ") {
                caw.write(delim);
                caw.write(in.readUTF());
            }
            pw.write("} ");
            infoOut.append(caw.toString());
        }
        skipCustomData(in, infoOut, indent);
        newlineIndent(infoOut, indent + 2);
        infoOut.append("Super: ");
        formatObject(in, infoOut, indent);
        return CLASS_HANDLE;
    }

    /**
     * Read and format a class.
     *
     * @param in input stream
     * @param infoOut output stream
     * @param indent indentation level
     * @return a label for the object just read
     * @throws IOException if an error occurs on the stream
     */
    private Handle formatTC_CLASS(DataInputStream in, Appendable infoOut, int indent) throws IOException {
        Handle label = new Handle("class#" + nextHandle);
        setLabel(nextHandle, label);
        infoOut.append(String.format("CLASS #%d; ", nextHandle++));
        formatObject(in, infoOut, indent);
        return label;
    }

    /**
     * Read and format an instance.
     *
     * @param in input stream
     * @param infoOut output stream
     * @param indent indentation level
     * @return a label for the object just read
     * @throws IOException if an error occurs on the stream
     */
    private Handle formatTC_OBJECT(DataInputStream in, Appendable infoOut, int indent) throws IOException {
        infoOut.append("READ ");
        Handle type = formatObject(in, infoOut, indent);
        setLabel(nextHandle, OBJ_HANDLE);
        newlineIndent(infoOut, indent);
        infoOut.append(String.format("OBJ #%d %s ", nextHandle++, type.label()));

        if (type instanceof ClassHandle) {
            ClassHandle[] types =  ((ClassHandle) type).allClasses();
            for (ClassHandle ch : types) {
                ch.forEachField((n, v) -> {
                    try {
                        newlineIndent(infoOut, indent + 2).append(n).append(":");
                        var cl = mapClassName(v.substring(0, 1));
                        if (cl != Object.class) {
                            var f = HexPrinter.getFormatter(cl, "%s ");
                            f.annotate(in, infoOut);
                        } else {
                            formatObject(in, infoOut, indent + 2);
                        }
                    } catch (IOException ioe) {
                        // ignore
                    }
                });
                // Check for Custom data followed by ENDBLOCK, if there was a writeObject method
                if (ch.hasFlag(ObjectStreamConstants.SC_WRITE_METHOD)) {
                    newlineIndent(infoOut, indent + 2);
                    infoOut.append("CustomData: ");
                    Handle skipped;
                    do {
                        newlineIndent(infoOut, indent + 4);
                        skipped = formatObject(in, infoOut, indent + 4);
                    } while (!skipped.equals(END_HANDLE));
                }
            }
        }
        return OBJ_HANDLE;
    }

    /**
     * Read and format an Enum.
     *
     * @param in input stream
     * @param infoOut output stream
     * @param indent indentation level
     * @return a label for the object just read
     * @throws IOException if an error occurs on the stream
     */
    private Handle formatTC_ENUM(DataInputStream in, Appendable infoOut, int indent) throws IOException {
        setLabel(nextHandle, ENUM_HANDLE);
        infoOut.append(String.format("READ "));
        Handle enumType = formatObject(in, infoOut, indent);
        Handle h = formatObject(in, infoOut, indent + 2);
        setLabel(nextHandle, h);
        newlineIndent(infoOut, indent);
        infoOut.append(String.format("ENUM #%d %s.%s ", nextHandle++, enumType.label(), h.label()));

        return ENUM_HANDLE;
    }

    /**
     * Read and format an array.
     *
     * @param in input stream
     * @param infoOut output stream
     * @param indent indentation level
     * @return a label for the object just read
     * @throws IOException if an error occurs on the stream
     */
    private Handle formatTC_ARRAY(DataInputStream in, Appendable infoOut, int indent) throws IOException {
        infoOut.append("READ ");
        Handle type = formatObject(in, infoOut, indent);
        newlineIndent(infoOut, indent);
        setLabel(nextHandle, ARRAY_HANDLE);
        int nelements = in.readInt();
        infoOut.append(String.format("ARRAY #%d ", nextHandle++));

        infoOut.append(String.format("%d", nelements));
        if (type.toString().charAt(0) == '[') {
            infoOut.append("[");
            formatArray(in, infoOut, nelements, type.toString().charAt(1), indent + 2);
            infoOut.append("] ");
        }
        return ARRAY_HANDLE;
    }

    /**
     * Read and format block data.
     *
     * @param in input stream
     * @param infoOut output stream
     * @return a label for the object just read
     * @throws IOException if an error occurs on the stream
     */
    private Handle formatTC_BLOCKDATA(DataInputStream in, Appendable infoOut) throws IOException {
        int l = in.readUnsignedByte();
        StringBuilder sb = new StringBuilder(32 + l + 2);
        sb.append("BLOCKDATA " + l + "[ ");
        for (int i = 0; i < l; i++) {
            int v = in.readUnsignedByte();
            sb.append(toPrintable((char)v));
        }
        sb.append("]; ");
        infoOut.append(sb.toString());
        return BLOCK_HANDLE;
    }

    /**
     * Read and format long block data.
     *
     * @param in input stream
     * @param infoOut output stream
     * @return a label for the object just read
     * @throws IOException if an error occurs on the stream
     */
    private Handle formatTC_BLOCKDATALONG(DataInputStream in, Appendable infoOut) throws IOException {
        int l = in.readInt();
        StringBuilder sb = new StringBuilder(32 + l + 2);
        sb.append("BLOCKDATALONG: " + l + " [ ");
        for (int i = 0; i < l; i++) {
            int v = in.readUnsignedByte();
            sb.append(String.format("%02x ", v));
        }
        sb.append("]; ");
        infoOut.append(sb.toString());
        return BLOCKLONG_HANDLE;
    }

    /**
     * Read and format end-of-block-data.
     *
     * @param in input stream
     * @param infoOut output stream
     * @return a label for the object just read
     * @throws IOException if an error occurs on the stream
     */
    private Handle formatTC_ENDBLOCKDATA(DataInputStream in, Appendable infoOut) throws IOException {
        infoOut.append("ENDBLOCK; ");
        return END_HANDLE;
    }

    /**
     * Read and format a stream exception.
     *
     * @param in input stream
     * @param infoOut output stream
     * @return a label for the object just read
     * @throws IOException if an error occurs on the stream
     */
    private Handle formatTC_EXCEPTION(DataInputStream in, Appendable infoOut) throws IOException {
        infoOut.append("EXCEPTION; ");
        return EXCEPTION_HANDLE;
    }

    /**
     * Read and format a stream reset.
     *
     * @param in input stream
     * @param infoOut output stream
     * @return a label for the object just read
     * @throws IOException if an error occurs on the stream
     */
    private Handle formatTC_RESET(DataInputStream in, Appendable infoOut) throws IOException {
        nextHandle = 0;
        infoOut.append("RESET; ");
        return RESET_HANDLE;
    }

    /**
     * Format the stream header.
     * The first byte already has been read.
     * @param in  a DataInputStream
     * @param out an Appendable for the output
     * @param indent indentation level
     * @return a Handle for the Header - unused.
     * @throws IOException if an error occurs on the stream
     */
    private Handle formatSTREAM_MAGIC(DataInputStream in, Appendable out, int indent) throws IOException {
        // Scan for the Serialization protocol header
        // Format anything else as bytes
        int high = 0xAC, low;
        String prefix = " ".repeat(indent);
        if ((low = in.read()) != 0xED || high != 0xAC) {
            out.append(prefix).append("data: " + high  + ", " + low)
                    .append(System.lineSeparator());
            if (low < 0)
                throw new EOFException();
            throw new IOException("malformed stream header");
        }

        int s1 = in.readUnsignedShort();
        out.append(" ".repeat(indent));
        out.append(String.format("ObjectStream Version: %d", s1));
        newlineIndent(out, indent); // Start a new line for the object
        return HEADER_HANDLE;
    }

    /**
     * Set the label for a handle.
     *
     * @param handle
     * @param label
     */
    private void setLabel(int handle, String label) {
        setLabel(handle, new Handle(label));
    }
    private void setLabel(int handle, Handle label) {
        while (labels.size() <= handle)
            labels.add(label);
        labels.set(handle, label);
    }

    /**
     * Get the label for a handle.
     *
     * @param handle
     * @return
     */
    private String getLabel(int handle) {
        return getHandle(handle).label();
    }

    private Handle getHandle(int handle) {
        return (handle < 0 || handle >= labels.size()) ? INVALID_HANDLE : labels.get(handle);
    }

    /**
     * Map a raw class name to a Class based on the type.
     *
     * @param name
     * @return
     */
    private Class<?> mapClassName(String name) {
        switch (name.substring(0, 1)) {
            case "I":
                return int.class;
            case "J":
                return long.class;
            case "Z":
                return boolean.class;
            case "S":
                return short.class;
            case "C":
                return char.class;
            case "F":
                return float.class;
            case "D":
                return double.class;
            case "B":
                return byte.class;
            case "L":
            case "[":
                return Object.class;
            default:
                throw new RuntimeException("unknown class char: " + name);
        }
    }

    /**
     * Read and format an array.
     *
     * @param in input stream
     * @param infoOut output stream
     * @param count the number of elements
     * @param type the type code
     * @param indent indentation level
     * @return a label for the object just read
     * @throws IOException if an error occurs on the stream
     */
    private void formatArray(DataInputStream in, Appendable infoOut, int count, char type, int indent)
            throws IOException {
        switch (type) {
            case 'I':
                while (count-- > 0) {
                    int v = in.readInt();
                    infoOut.append(Integer.toString(v));
                    if (count > 0) infoOut.append(' ');
                }
                break;
            case 'J':
                while (count-- > 0) {
                    long v = in.readLong();
                    infoOut.append(Long.toString(v));
                    if (count > 0) infoOut.append(' ');
                }
                break;
            case 'C':
                while (count-- > 0) {
                    int v = in.readUnsignedShort();
                    infoOut.append((char) v);
                    if (count > 0) infoOut.append(' ');
                }
                break;
            case 'S':
                while (count-- > 0) {
                    int v = in.readUnsignedShort();
                    infoOut.append(Integer.toString(v));
                    if (count > 0) infoOut.append(' ');
                }
                break;
            case 'F':
                while (count-- > 0) {
                    float v = in.readFloat();
                    infoOut.append(Float.toString(v));
                    if (count > 0) infoOut.append(' ');
                }
                break;
            case 'D':
                while (count-- > 0) {
                    double v = in.readDouble();
                    infoOut.append(Double.toString(v));
                    if (count > 0) infoOut.append(' ');
                }
                break;
            case 'Z':
                while (count-- > 0) {
                    boolean v = in.readBoolean();
                    infoOut.append(v ? "true" : "false");
                    if (count > 0) infoOut.append(' ');
                }
                break;
            case 'L':
                while (count-- > 0) {
                    formatObject(in, infoOut, indent + 2);
                    if (count > 0) newlineIndent(infoOut, indent);
                }
                break;
            case 'B':
            default:            // anything unknown as bytes
                while (count-- > 0) {
                    int v = in.readUnsignedByte();
                    infoOut.append(toPrintable((char) v));
                }
                break;
        }
    }


    private char toPrintable(char ch) {
        if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') ||
                (ch >= '0' && ch <= '9')) {
            return ch;
        } else {
            switch (ch) {
                case ' ':       /* space */
                case '\'':      /* apostrophe */
                case '(':       /* left paren */
                case ')':       /* right paren */
                case '+':       /* plus */
                case ',':       /* comma */
                case '-':       /* hyphen */
                case '.':       /* period */
                case '/':       /* slash */
                case ':':       /* colon */
                case '=':       /* equals */
                case '?':       /* question mark */
                    return ch;
                default:
                    return '.';
            }
        }
    }

    static class Handle {
        private final String label;

        Handle(String label) {
            this.label = label;
        }

        String label() {
            return label;
        }
        public String toString() {
            return label;
        }
    }

    static final Handle EMPTY_HANDLE = new Handle("");
    static final Handle INVALID_HANDLE = new Handle("invalid handle");
    static final Handle NULL_HANDLE = new Handle("null");
    static final Handle RESET_HANDLE = new Handle("reset");
    static final Handle EXCEPTION_HANDLE = new Handle("exception");
    static final Handle END_HANDLE = new Handle("end");
    static final Handle BLOCK_HANDLE = new Handle("block");
    static final Handle BLOCKLONG_HANDLE = new Handle("blocklong");
    static final Handle ARRAY_HANDLE = new Handle("array");
    static final Handle ENUM_HANDLE = new Handle("enum");
    static final Handle OBJ_HANDLE = new Handle("obj");
    static final Handle CLASS_HANDLE = new Handle("class");
    static final Handle HEADER_HANDLE = new Handle("header");

    static class ClassHandle extends Handle {
        private final Map<String, String> fields;
        private int flags;
        private ClassHandle[] allClasses;

        ClassHandle(String label) {
            super(label);
            this.fields = new LinkedHashMap<>();
            allClasses = new ClassHandle[] { this };
        }

        void addField(String name, String type) {
            fields.put(name, type);
        }

        void superClass(ClassHandle superClass) {
            ClassHandle[] types = superClass.allClasses();
            types = Arrays.copyOf(types, types.length + 1);
            types[types.length - 1] = this;
            this.allClasses = types;
        }

        void setFlags(int flags) {
            this.flags = flags;
        }

        boolean hasFlag(int flagBits) {
            return (flags & flagBits) != 0;
        }

        void forEachField(BiConsumer<String, String> doit) {
            fields.forEach(doit);
        }

        ClassHandle[] allClasses() {
            return allClasses;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(super.toString()).append(": ");
            fields.forEach( (k,v) -> sb
                    .append(k)
                    .append('=')
                    .append(v)
                    .append(", ")
                    .append((allClasses == null) ? "" : "; super: " + allClasses[allClasses.length - 1].label()));
            return sb.toString();
        }
    }
    /**
     * Simple utility to open and print contents of one or more files as a serialized object stream.
     * @param args file names
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage:  <object stream files>");
            return;
        }
        ObjectStreamPrinter fmt = ObjectStreamPrinter.formatter();
        for (String file : args) {
            System.out.printf("%s%n", file);
            try (InputStream is = Files.newInputStream(Path.of(file))) {

                DataInputStream dis = new DataInputStream(is);
                HexPrinter p = HexPrinter.simple()
                        .dest(System.out)
                        .formatter(ObjectStreamPrinter.formatter(), "; ", 100);
                p.format(dis);
            } catch (EOFException eof) {
                System.out.println();
            } catch (IOException ioe) {
                System.out.printf("%s: %s%n", file, ioe);
            }
        }
    }
}
