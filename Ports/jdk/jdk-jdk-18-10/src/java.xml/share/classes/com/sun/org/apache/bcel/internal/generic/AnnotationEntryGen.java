/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.org.apache.bcel.internal.generic;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.sun.org.apache.bcel.internal.classfile.AnnotationEntry;
import com.sun.org.apache.bcel.internal.classfile.Attribute;
import com.sun.org.apache.bcel.internal.classfile.ConstantUtf8;
import com.sun.org.apache.bcel.internal.classfile.ElementValuePair;
import com.sun.org.apache.bcel.internal.classfile.RuntimeInvisibleAnnotations;
import com.sun.org.apache.bcel.internal.classfile.RuntimeInvisibleParameterAnnotations;
import com.sun.org.apache.bcel.internal.classfile.RuntimeVisibleAnnotations;
import com.sun.org.apache.bcel.internal.classfile.RuntimeVisibleParameterAnnotations;

/**
 * @since 6.0
 * @LastModified: Jan 2020
 */
public class AnnotationEntryGen {
    private int typeIndex;

    private List<ElementValuePairGen> evs;

    private final ConstantPoolGen cpool;

    private boolean isRuntimeVisible = false;

    /**
     * Here we are taking a fixed annotation of type Annotation and building a
     * modifiable AnnotationGen object. If the pool passed in is for a different
     * class file, then copyPoolEntries should have been passed as true as that
     * will force us to do a deep copy of the annotation and move the cpool
     * entries across. We need to copy the type and the element name value pairs
     * and the visibility.
     */
    public AnnotationEntryGen(final AnnotationEntry a, final ConstantPoolGen cpool,
                              final boolean copyPoolEntries) {
        this.cpool = cpool;
        if (copyPoolEntries) {
            typeIndex = cpool.addUtf8(a.getAnnotationType());
        } else {
            typeIndex = a.getAnnotationTypeIndex();
        }
        isRuntimeVisible = a.isRuntimeVisible();
        evs = copyValues(a.getElementValuePairs(), cpool, copyPoolEntries);
    }

    private List<ElementValuePairGen> copyValues(final ElementValuePair[] in, final ConstantPoolGen cpool,
                                                 final boolean copyPoolEntries) {
        final List<ElementValuePairGen> out = new ArrayList<>();
        for (final ElementValuePair nvp : in) {
            out.add(new ElementValuePairGen(nvp, cpool, copyPoolEntries));
        }
        return out;
    }

    private AnnotationEntryGen(final ConstantPoolGen cpool) {
        this.cpool = cpool;
    }

    /**
     * Retrieve an immutable version of this AnnotationGen
     */
    public AnnotationEntry getAnnotation() {
        final AnnotationEntry a = new AnnotationEntry(typeIndex, cpool.getConstantPool(),
                isRuntimeVisible);
        for (final ElementValuePairGen element : evs) {
            a.addElementNameValuePair(element.getElementNameValuePair());
        }
        return a;
    }

    public AnnotationEntryGen(final ObjectType type,
                              final List<ElementValuePairGen> elements, final boolean vis,
                              final ConstantPoolGen cpool) {
        this.cpool = cpool;
        this.typeIndex = cpool.addUtf8(type.getSignature());
        evs = elements;
        isRuntimeVisible = vis;
    }

    public static AnnotationEntryGen read(final DataInput dis,
                                          final ConstantPoolGen cpool, final boolean b) throws IOException {
        final AnnotationEntryGen a = new AnnotationEntryGen(cpool);
        a.typeIndex = dis.readUnsignedShort();
        final int elemValuePairCount = dis.readUnsignedShort();
        for (int i = 0; i < elemValuePairCount; i++) {
            final int nidx = dis.readUnsignedShort();
            a.addElementNameValuePair(new ElementValuePairGen(nidx,
                    ElementValueGen.readElementValue(dis, cpool), cpool));
        }
        a.isRuntimeVisible(b);
        return a;
    }

    public void dump(final DataOutputStream dos) throws IOException {
        dos.writeShort(typeIndex); // u2 index of type name in cpool
        dos.writeShort(evs.size()); // u2 element_value pair count
        for (final ElementValuePairGen envp : evs) {
            envp.dump(dos);
        }
    }

    public void addElementNameValuePair(final ElementValuePairGen evp) {
        if (evs == null) {
            evs = new ArrayList<>();
        }
        evs.add(evp);
    }

    public int getTypeIndex() {
        return typeIndex;
    }

    public final String getTypeSignature() {
        // ConstantClass c = (ConstantClass)cpool.getConstant(typeIndex);
        final ConstantUtf8 utf8 = (ConstantUtf8) cpool
                .getConstant(typeIndex/* c.getNameIndex() */);
        return utf8.getBytes();
    }

    public final String getTypeName() {
        return getTypeSignature();// BCELBUG: Should I use this instead?
        // Utility.signatureToString(getTypeSignature());
    }

    /**
     * Returns list of ElementNameValuePair objects
     */
    public List<ElementValuePairGen> getValues() {
        return evs;
    }

    @Override
    public String toString() {
        final StringBuilder s = new StringBuilder(32); // CHECKSTYLE IGNORE MagicNumber
        s.append("AnnotationGen:[").append(getTypeName()).append(" #").append(evs.size()).append(" {");
        for (int i = 0; i < evs.size(); i++) {
            s.append(evs.get(i));
            if (i + 1 < evs.size()) {
                s.append(",");
            }
        }
        s.append("}]");
        return s.toString();
    }

    public String toShortString() {
        final StringBuilder s = new StringBuilder();
        s.append("@").append(getTypeName()).append("(");
        for (int i = 0; i < evs.size(); i++) {
            s.append(evs.get(i));
            if (i + 1 < evs.size()) {
                s.append(",");
            }
        }
        s.append(")");
        return s.toString();
    }

    private void isRuntimeVisible(final boolean b) {
        isRuntimeVisible = b;
    }

    public boolean isRuntimeVisible() {
        return isRuntimeVisible;
    }


    /**
     * Converts a list of AnnotationGen objects into a set of attributes
     * that can be attached to the class file.
     *
     * @param cp  The constant pool gen where we can create the necessary name refs
     * @param annotationEntryGens An array of AnnotationGen objects
     */
    static Attribute[] getAnnotationAttributes(final ConstantPoolGen cp, final AnnotationEntryGen[] annotationEntryGens) {
        if (annotationEntryGens.length == 0) {
            return new Attribute[0];
        }

        try {
            int countVisible = 0;
            int countInvisible = 0;

            //  put the annotations in the right output stream
            for (final AnnotationEntryGen a : annotationEntryGens) {
                if (a.isRuntimeVisible()) {
                    countVisible++;
                } else {
                    countInvisible++;
                }
            }

            final ByteArrayOutputStream rvaBytes = new ByteArrayOutputStream();
            final ByteArrayOutputStream riaBytes = new ByteArrayOutputStream();
            try (DataOutputStream rvaDos = new DataOutputStream(rvaBytes);
                    DataOutputStream riaDos = new DataOutputStream(riaBytes)) {

                rvaDos.writeShort(countVisible);
                riaDos.writeShort(countInvisible);

                // put the annotations in the right output stream
                for (final AnnotationEntryGen a : annotationEntryGens) {
                    if (a.isRuntimeVisible()) {
                        a.dump(rvaDos);
                    } else {
                        a.dump(riaDos);
                    }
                }
            }

            final byte[] rvaData = rvaBytes.toByteArray();
            final byte[] riaData = riaBytes.toByteArray();

            int rvaIndex = -1;
            int riaIndex = -1;

            if (rvaData.length > 2) {
                rvaIndex = cp.addUtf8("RuntimeVisibleAnnotations");
            }
            if (riaData.length > 2) {
                riaIndex = cp.addUtf8("RuntimeInvisibleAnnotations");
            }

            final List<Attribute> newAttributes = new ArrayList<>();
            if (rvaData.length > 2) {
                newAttributes.add(
                        new RuntimeVisibleAnnotations(rvaIndex, rvaData.length,
                            new DataInputStream(new ByteArrayInputStream(rvaData)), cp.getConstantPool()));
            }
            if (riaData.length > 2) {
                newAttributes.add(
                        new RuntimeInvisibleAnnotations(riaIndex, riaData.length,
                            new DataInputStream(new ByteArrayInputStream(riaData)), cp.getConstantPool()));
            }

            return newAttributes.toArray(new Attribute[newAttributes.size()]);
        } catch (final IOException e) {
            System.err.println("IOException whilst processing annotations");
            e.printStackTrace();
        }
        return null;
    }


    /**
     * Annotations against a class are stored in one of four attribute kinds:
     * - RuntimeVisibleParameterAnnotations
     * - RuntimeInvisibleParameterAnnotations
     */
    static Attribute[] getParameterAnnotationAttributes(
            final ConstantPoolGen cp,
            final List<AnnotationEntryGen>[] /*Array of lists, array size depends on #params */vec) {
        final int[] visCount = new int[vec.length];
        int totalVisCount = 0;
        final int[] invisCount = new int[vec.length];
        int totalInvisCount = 0;
        try {
            for (int i = 0; i < vec.length; i++) {
                if (vec[i] != null) {
                    for (final AnnotationEntryGen element : vec[i]) {
                        if (element.isRuntimeVisible()) {
                            visCount[i]++;
                            totalVisCount++;
                        } else {
                            invisCount[i]++;
                            totalInvisCount++;
                        }
                    }
                }
            }
            // Lets do the visible ones
            final ByteArrayOutputStream rvaBytes = new ByteArrayOutputStream();
            try (DataOutputStream rvaDos = new DataOutputStream(rvaBytes)) {
                rvaDos.writeByte(vec.length); // First goes number of parameters
                for (int i = 0; i < vec.length; i++) {
                    rvaDos.writeShort(visCount[i]);
                    if (visCount[i] > 0) {
                        for (final AnnotationEntryGen element : vec[i]) {
                            if (element.isRuntimeVisible()) {
                                element.dump(rvaDos);
                            }
                        }
                    }
                }
            }
            // Lets do the invisible ones
            final ByteArrayOutputStream riaBytes = new ByteArrayOutputStream();
            try (DataOutputStream riaDos = new DataOutputStream(riaBytes)) {
                riaDos.writeByte(vec.length); // First goes number of parameters
                for (int i = 0; i < vec.length; i++) {
                    riaDos.writeShort(invisCount[i]);
                    if (invisCount[i] > 0) {
                        for (final AnnotationEntryGen element : vec[i]) {
                            if (!element.isRuntimeVisible()) {
                                element.dump(riaDos);
                            }
                        }
                    }
                }
            }
            final byte[] rvaData = rvaBytes.toByteArray();
            final byte[] riaData = riaBytes.toByteArray();
            int rvaIndex = -1;
            int riaIndex = -1;
            if (totalVisCount > 0) {
                rvaIndex = cp.addUtf8("RuntimeVisibleParameterAnnotations");
            }
            if (totalInvisCount > 0) {
                riaIndex = cp.addUtf8("RuntimeInvisibleParameterAnnotations");
            }
            final List<Attribute> newAttributes = new ArrayList<>();
            if (totalVisCount > 0) {
                newAttributes
                        .add(new RuntimeVisibleParameterAnnotations(rvaIndex,
                                rvaData.length,
                                new DataInputStream(new ByteArrayInputStream(rvaData)),
                                    cp.getConstantPool()));
            }
            if (totalInvisCount > 0) {
                newAttributes
                        .add(new RuntimeInvisibleParameterAnnotations(riaIndex,
                                riaData.length,
                                new DataInputStream(new ByteArrayInputStream(riaData)),
                                    cp.getConstantPool()));
            }
            return newAttributes.toArray(new Attribute[newAttributes.size()]);
        } catch (final IOException e) {
            System.err.println("IOException whilst processing parameter annotations." +
                    e.getMessage());
        }
        return null;
    }

}
