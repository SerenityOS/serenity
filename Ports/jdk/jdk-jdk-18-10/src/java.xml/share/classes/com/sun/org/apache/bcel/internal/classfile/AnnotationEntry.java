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

package com.sun.org.apache.bcel.internal.classfile;

import java.io.DataInput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sun.org.apache.bcel.internal.Const;

/**
 * represents one annotation in the annotation table
 *
 * @since 6.0
 */
public class AnnotationEntry implements Node {

    private final int typeIndex;
    private final ConstantPool constantPool;
    private final boolean isRuntimeVisible;

    private List<ElementValuePair> elementValuePairs;

    /*
     * Factory method to create an AnnotionEntry from a DataInput
     *
     * @param input
     * @param constantPool
     * @param isRuntimeVisible
     * @return the entry
     * @throws IOException
     */
    public static AnnotationEntry read(final DataInput input, final ConstantPool constant_pool, final boolean isRuntimeVisible) throws IOException {

        final AnnotationEntry annotationEntry = new AnnotationEntry(input.readUnsignedShort(), constant_pool, isRuntimeVisible);
        final int num_element_value_pairs = input.readUnsignedShort();
        annotationEntry.elementValuePairs = new ArrayList<>();
        for (int i = 0; i < num_element_value_pairs; i++) {
            annotationEntry.elementValuePairs.add(
                    new ElementValuePair(input.readUnsignedShort(), ElementValue.readElementValue(input, constant_pool),
                    constant_pool));
        }
        return annotationEntry;
    }

    public AnnotationEntry(final int type_index, final ConstantPool constant_pool, final boolean isRuntimeVisible) {
        this.typeIndex = type_index;
        this.constantPool = constant_pool;
        this.isRuntimeVisible = isRuntimeVisible;
    }

    public int getTypeIndex() {
        return typeIndex;
    }

    public ConstantPool getConstantPool() {
        return constantPool;
    }

    public boolean isRuntimeVisible() {
        return isRuntimeVisible;
    }

    /**
     * Called by objects that are traversing the nodes of the tree implicitely defined by the contents of a Java class.
     * I.e., the hierarchy of methods, fields, attributes, etc. spawns a tree of objects.
     *
     * @param v Visitor object
     */
    @Override
    public void accept(final Visitor v) {
        v.visitAnnotationEntry(this);
    }

    /**
     * @return the annotation type name
     */
    public String getAnnotationType() {
        final ConstantUtf8 c = (ConstantUtf8) constantPool.getConstant(typeIndex, Const.CONSTANT_Utf8);
        return c.getBytes();
    }

    /**
     * @return the annotation type index
     */
    public int getAnnotationTypeIndex() {
        return typeIndex;
    }

    /**
     * @return the number of element value pairs in this annotation entry
     */
    public final int getNumElementValuePairs() {
        return elementValuePairs.size();
    }

    /**
     * @return the element value pairs in this annotation entry
     */
    public ElementValuePair[] getElementValuePairs() {
        // TODO return List
        return elementValuePairs.toArray(new ElementValuePair[elementValuePairs.size()]);
    }

    public void dump(final DataOutputStream dos) throws IOException {
        dos.writeShort(typeIndex); // u2 index of type name in cpool
        dos.writeShort(elementValuePairs.size()); // u2 element_value pair
        // count
        for (final ElementValuePair envp : elementValuePairs) {
            envp.dump(dos);
        }
    }

    public void addElementNameValuePair(final ElementValuePair elementNameValuePair) {
        elementValuePairs.add(elementNameValuePair);
    }

    public String toShortString() {
        final StringBuilder result = new StringBuilder();
        result.append("@");
        result.append(getAnnotationType());
        final ElementValuePair[] evPairs = getElementValuePairs();
        if (evPairs.length > 0) {
            result.append("(");
            for (final ElementValuePair element : evPairs) {
                result.append(element.toShortString());
            }
            result.append(")");
        }
        return result.toString();
    }

    @Override
    public String toString() {
        return toShortString();
    }

    public static AnnotationEntry[] createAnnotationEntries(final Attribute[] attrs) {
        // Find attributes that contain annotation data
        final List<AnnotationEntry> accumulatedAnnotations = new ArrayList<>(attrs.length);
        for (final Attribute attribute : attrs) {
            if (attribute instanceof Annotations) {
                final Annotations runtimeAnnotations = (Annotations) attribute;
                Collections.addAll(accumulatedAnnotations, runtimeAnnotations.getAnnotationEntries());
            }
        }
        return accumulatedAnnotations.toArray(new AnnotationEntry[accumulatedAnnotations.size()]);
    }
}
