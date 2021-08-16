/*
 * Copyright (c) 2004, 2019, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

package sun.jvm.hotspot.utilities;

import java.io.*;
import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.gc.shared.OopStorage;
import sun.jvm.hotspot.memory.*;
import sun.jvm.hotspot.oops.*;
import sun.jvm.hotspot.runtime.*;

/**
 * This is abstract base class for heap graph writers. This class does
 * not assume any file format for the heap graph. It hides heap
 * iteration, object (fields) iteration mechanism from derived
 * classes. This class does not even accept OutputStream etc. so that
 * derived class can construct specific writer/filter from input
 * stream.
 */

public abstract class AbstractHeapGraphWriter implements HeapGraphWriter {
    // the function iterates heap and calls Oop type specific writers
    protected void write() throws IOException {
        javaLangClass = "java/lang/Class";
        javaLangString = "java/lang/String";
        javaLangThread = "java/lang/Thread";
        ObjectHeap heap = VM.getVM().getObjectHeap();
        try {
            heap.iterate(new DefaultHeapVisitor() {
                    public void prologue(long usedSize) {
                        try {
                            writeHeapHeader();
                        } catch (IOException exp) {
                            throw new RuntimeException(exp);
                        }
                    }

                    public boolean doObj(Oop oop) {
                        try {
                            writeHeapRecordPrologue();
                            if (oop instanceof TypeArray) {
                                writePrimitiveArray((TypeArray)oop);
                            } else if (oop instanceof ObjArray) {
                                Klass klass = oop.getKlass();
                                ObjArrayKlass oak = (ObjArrayKlass) klass;
                                Klass bottomType = oak.getBottomKlass();
                                if (bottomType instanceof InstanceKlass ||
                                    bottomType instanceof TypeArrayKlass) {
                                    writeObjectArray((ObjArray)oop);
                                } else {
                                    writeInternalObject(oop);
                                }
                            } else if (oop instanceof Instance) {
                                Instance instance = (Instance) oop;
                                Klass klass = instance.getKlass();
                                Symbol name = klass.getName();
                                if (name.equals(javaLangString)) {
                                    writeString(instance);
                                } else if (name.equals(javaLangClass)) {
                                    writeClass(instance);
                                } else if (name.equals(javaLangThread)) {
                                    writeThread(instance);
                                } else {
                                    klass = klass.getSuper();
                                    while (klass != null) {
                                        name = klass.getName();
                                        if (name.equals(javaLangThread)) {
                                            writeThread(instance);
                                            return false;
                                        }
                                        klass = klass.getSuper();
                                    }
                                    writeInstance(instance);
                                }
                            } else {
                                // not-a-Java-visible oop
                                writeInternalObject(oop);
                            }
                            writeHeapRecordEpilogue();
                        } catch (IOException exp) {
                            throw new RuntimeException(exp);
                        }
                        return false;
                    }

                    public void epilogue() {
                        try {
                            writeHeapFooter();
                        } catch (IOException exp) {
                            throw new RuntimeException(exp);
                        }
                    }
                });

                writeHeapRecordPrologue();

                // write JavaThreads
                writeJavaThreads();

                // write JNI global handles
                writeGlobalJNIHandles();

        } catch (RuntimeException re) {
            handleRuntimeException(re);
        }
    }

    protected void writeJavaThreads() throws IOException {
        Threads threads = VM.getVM().getThreads();
        for (int i = 0; i < threads.getNumberOfThreads(); i++) {
            JavaThread jt = threads.getJavaThreadAt(i);
            if (jt.getThreadObj() != null) {
                // Note that the thread serial number range is 1-to-N
                writeJavaThread(jt, i + 1);
            }
        }
    }

    protected void writeJavaThread(JavaThread jt, int index)
                            throws IOException {
    }

    protected void writeGlobalJNIHandles() throws IOException {
        JNIHandles handles = VM.getVM().getJNIHandles();
        OopStorage blk = handles.globalHandles();
        if (blk != null) {
            try {
                blk.oopsDo(new AddressVisitor() {
                          public void visitAddress(Address handleAddr) {
                              try {
                                  if (handleAddr != null) {
                                      writeGlobalJNIHandle(handleAddr);
                                  }
                              } catch (IOException exp) {
                                  throw new RuntimeException(exp);
                              }
                          }
                              public void visitCompOopAddress(Address handleAddr) {
                             throw new RuntimeException("Should not reach here. JNIHandles are not compressed");
                          }
                       });
            } catch (RuntimeException re) {
                handleRuntimeException(re);
            }
        }
    }

    protected void writeGlobalJNIHandle(Address handleAddr) throws IOException {
    }

    protected void writeHeapHeader() throws IOException {
    }

    // write non-Java-visible (hotspot internal) object
    protected void writeInternalObject(Oop oop) throws IOException {
    }

    // write Java primitive array
    protected void writePrimitiveArray(TypeArray array) throws IOException {
        writeObject(array);
    }

    // write Java object array
    protected void writeObjectArray(ObjArray array) throws IOException {
        writeObject(array);
    }

    protected void writeInstance(Instance instance) throws IOException {
        writeObject(instance);
    }

    protected void writeString(Instance instance) throws IOException {
        writeInstance(instance);
    }

    protected void writeClass(Instance instance) throws IOException {
        writeInstance(instance);
    }

    protected void writeThread(Instance instance) throws IOException {
        writeInstance(instance);
    }

    protected void writeObject(Oop oop) throws IOException {
        writeObjectHeader(oop);
        writeObjectFields(oop);
        writeObjectFooter(oop);
    }

    protected void writeObjectHeader(Oop oop) throws IOException {
    }

    // write instance fields of given object
    protected void writeObjectFields(final Oop oop) throws IOException {
        try {
            oop.iterate(new DefaultOopVisitor() {
                    public void doOop(OopField field, boolean isVMField) {
                        try {
                                writeReferenceField(oop, field);
                        } catch (IOException exp) {
                            throw new RuntimeException(exp);
                        }
                    }

                    public void doByte(ByteField field, boolean isVMField) {
                        try {
                            writeByteField(oop, field);
                        } catch (IOException exp) {
                            throw new RuntimeException(exp);
                        }
                    }

                    public void doChar(CharField field, boolean isVMField) {
                        try {
                            writeCharField(oop, field);
                        } catch (IOException exp) {
                            throw new RuntimeException(exp);
                        }
                    }

                    public void doBoolean(BooleanField field, boolean vField) {
                        try {
                            writeBooleanField(oop, field);
                        } catch (IOException exp) {
                            throw new RuntimeException(exp);
                        }
                    }

                    public void doShort(ShortField field, boolean isVMField) {
                        try {
                            writeShortField(oop, field);
                        } catch (IOException exp) {
                            throw new RuntimeException(exp);
                        }
                    }

                    public void doInt(IntField field, boolean isVMField) {
                        try {
                            writeIntField(oop, field);
                        } catch (IOException exp) {
                            throw new RuntimeException(exp);
                        }
                    }

                    public void doLong(LongField field, boolean isVMField) {
                        try {
                            writeLongField(oop, field);
                        } catch (IOException exp) {
                            throw new RuntimeException(exp);
                        }
                    }

                    public void doFloat(FloatField field, boolean isVMField) {
                        try {
                            writeFloatField(oop, field);
                        } catch (IOException exp) {
                            throw new RuntimeException(exp);
                        }
                    }

                    public void doDouble(DoubleField field, boolean vField) {
                        try {
                            writeDoubleField(oop, field);
                        } catch (IOException exp) {
                            throw new RuntimeException(exp);
                        }
                    }
                }, false);
        } catch (RuntimeException re) {
            handleRuntimeException(re);
        }
    }

    // write instance fields of given object
    protected void writeObjectFields(final InstanceKlass oop) throws IOException {
        try {
            oop.iterateStaticFields(new DefaultOopVisitor() {
                    public void doOop(OopField field, boolean isVMField) {
                        try {
                            writeReferenceField(null, field);
                        } catch (IOException exp) {
                            throw new RuntimeException(exp);
                        }
    }

                    public void doByte(ByteField field, boolean isVMField) {
                        try {
                            writeByteField(null, field);
                        } catch (IOException exp) {
                            throw new RuntimeException(exp);
                        }
                    }

                    public void doChar(CharField field, boolean isVMField) {
                        try {
                            writeCharField(null, field);
                        } catch (IOException exp) {
                            throw new RuntimeException(exp);
                        }
                    }

                    public void doBoolean(BooleanField field, boolean vField) {
                        try {
                            writeBooleanField(null, field);
                        } catch (IOException exp) {
                            throw new RuntimeException(exp);
                        }
                    }

                    public void doShort(ShortField field, boolean isVMField) {
                        try {
                            writeShortField(null, field);
                        } catch (IOException exp) {
                            throw new RuntimeException(exp);
                        }
                    }

                    public void doInt(IntField field, boolean isVMField) {
                        try {
                            writeIntField(null, field);
                        } catch (IOException exp) {
                            throw new RuntimeException(exp);
                        }
                    }

                    public void doLong(LongField field, boolean isVMField) {
                        try {
                            writeLongField(null, field);
                        } catch (IOException exp) {
                            throw new RuntimeException(exp);
                        }
                    }

                    public void doFloat(FloatField field, boolean isVMField) {
                        try {
                            writeFloatField(null, field);
                        } catch (IOException exp) {
                            throw new RuntimeException(exp);
                        }
                    }

                    public void doDouble(DoubleField field, boolean vField) {
                        try {
                            writeDoubleField(null, field);
                        } catch (IOException exp) {
                            throw new RuntimeException(exp);
                        }
                    }
                });
        } catch (RuntimeException re) {
            handleRuntimeException(re);
        }
    }

    // object field writers
    protected void writeReferenceField(Oop oop, OopField field)
        throws IOException {
    }

    protected void writeByteField(Oop oop, ByteField field)
        throws IOException {
    }

    protected void writeCharField(Oop oop, CharField field)
        throws IOException {
    }

    protected void writeBooleanField(Oop oop, BooleanField field)
        throws IOException {
    }

    protected void writeShortField(Oop oop, ShortField field)
        throws IOException {
    }

    protected void writeIntField(Oop oop, IntField field)
        throws IOException {
    }

    protected void writeLongField(Oop oop, LongField field)
        throws IOException {
    }

    protected void writeFloatField(Oop oop, FloatField field)
        throws IOException {
    }

    protected void writeDoubleField(Oop oop, DoubleField field)
        throws IOException {
    }

    protected void writeObjectFooter(Oop oop) throws IOException {
    }

    protected void writeHeapFooter() throws IOException {
    }

    protected void writeHeapRecordPrologue() throws IOException {
    }

    protected void writeHeapRecordEpilogue() throws IOException {
    }

    // HeapVisitor, OopVisitor methods can't throw any non-runtime
    // exception. But, derived class write methods (which are called
    // from visitor callbacks) may throw IOException. Hence, we throw
    // RuntimeException with origianal IOException as cause from the
    // visitor methods. This method gets back the original IOException
    // (if any) and re-throws the same.
    protected void handleRuntimeException(RuntimeException re)
        throws IOException {
        Throwable cause = re.getCause();
        if (cause != null && cause instanceof IOException) {
            throw (IOException) cause;
        } else {
            // some other RuntimeException, just re-throw
            throw re;
        }
    }

    // whether a given oop is Java visible or hotspot internal?
    protected boolean isJavaVisible(Oop oop) {
        if (oop instanceof Instance || oop instanceof TypeArray) {
            return true;
        } else if (oop instanceof ObjArray) {
            ObjArrayKlass oak = (ObjArrayKlass) oop.getKlass();
            Klass bottomKlass = oak.getBottomKlass();
            return bottomKlass instanceof InstanceKlass ||
                   bottomKlass instanceof TypeArrayKlass;
        } else {
            return false;
        }
    }

    protected String javaLangClass;
    protected String javaLangString;
    protected String javaLangThread;
}
