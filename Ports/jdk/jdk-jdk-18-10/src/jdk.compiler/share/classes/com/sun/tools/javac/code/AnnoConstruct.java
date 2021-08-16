/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.code;

import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.lang.model.AnnotatedConstruct;

import com.sun.tools.javac.model.AnnotationProxyMaker;
import com.sun.tools.javac.util.DefinedBy;
import com.sun.tools.javac.util.DefinedBy.Api;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;

/**
 * Common super type for annotated constructs such as Types and Symbols.
 *
 * This class should *not* contain any fields since it would have a significant
 * impact on the javac memory footprint.
 *
 * <p><b>This is NOT part of any supported API.
 * If you write code that depends on this, you do so at your own
 * risk.  This code and its internal interfaces are subject to change
 * or deletion without notice.</b></p>
 */
public abstract class AnnoConstruct implements AnnotatedConstruct {


    // Override to enforce a narrower return type.
    @Override @DefinedBy(Api.LANGUAGE_MODEL)
    public abstract List<? extends Attribute.Compound> getAnnotationMirrors();


    // This method is part of the javax.lang.model API, do not use this in javac code.
    protected <A extends Annotation> Attribute.Compound getAttribute(Class<A> annoType) {
        String name = annoType.getName();

        for (Attribute.Compound anno : getAnnotationMirrors()) {
            if (name.equals(anno.type.tsym.flatName().toString()))
                return anno;
        }

        return null;
    }


    @SuppressWarnings("unchecked")
    protected <A extends Annotation> A[] getInheritedAnnotations(Class<A> annoType) {
        return (A[]) java.lang.reflect.Array.newInstance(annoType, 0);  // annoType is the Class for A
    }


    // This method is part of the javax.lang.model API, do not use this in javac code.
    @DefinedBy(Api.LANGUAGE_MODEL)
    public <A extends Annotation> A[] getAnnotationsByType(Class<A> annoType) {

        if (!annoType.isAnnotation())
            throw new IllegalArgumentException("Not an annotation type: "
                                               + annoType);
        // If annoType does not declare a container this is equivalent to wrapping
        // getAnnotation(...) in an array.
        Class <? extends Annotation> containerType = getContainer(annoType);
        if (containerType == null) {
            A res = getAnnotation(annoType);
            int size = res == null ? 0 : 1;

            @SuppressWarnings("unchecked") // annoType is the Class for A
            A[] arr = (A[])java.lang.reflect.Array.newInstance(annoType, size);
            if (res != null)
                arr[0] = res;
            return arr;
        }

        // So we have a containing type
        String annoTypeName = annoType.getName();
        String containerTypeName = containerType.getName();
        int directIndex = -1, containerIndex = -1;
        Attribute.Compound direct = null, container = null;
        // Find directly (explicit or implicit) present annotations
        int index = -1;
        for (Attribute.Compound attribute : getAnnotationMirrors()) {
            index++;
            if (attribute.type.tsym.flatName().contentEquals(annoTypeName)) {
                directIndex = index;
                direct = attribute;
            } else if(containerTypeName != null &&
                      attribute.type.tsym.flatName().contentEquals(containerTypeName)) {
                containerIndex = index;
                container = attribute;
            }
        }

        // Deal with inherited annotations
        if (direct == null && container == null &&
                annoType.isAnnotationPresent(Inherited.class))
            return getInheritedAnnotations(annoType);

        Attribute.Compound[] contained = unpackContained(container);

        // In case of an empty legacy container we might need to look for
        // inherited annos as well
        if (direct == null && contained.length == 0 &&
                annoType.isAnnotationPresent(Inherited.class))
            return getInheritedAnnotations(annoType);

        int size = (direct == null ? 0 : 1) + contained.length;
        @SuppressWarnings("unchecked") // annoType is the Class for A
        A[] arr = (A[])java.lang.reflect.Array.newInstance(annoType, size);

        // if direct && container, which is first?
        int insert = -1;
        int length = arr.length;
        if (directIndex >= 0 && containerIndex >= 0) {
            if (directIndex < containerIndex) {
                arr[0] = AnnotationProxyMaker.generateAnnotation(direct, annoType);
                insert = 1;
            } else {
                arr[arr.length - 1] = AnnotationProxyMaker.generateAnnotation(direct, annoType);
                insert = 0;
                length--;
            }
        } else if (directIndex >= 0) {
            arr[0] = AnnotationProxyMaker.generateAnnotation(direct, annoType);
            return arr;
        } else {
            // Only container
            insert = 0;
        }

        for (int i = 0; i + insert < length; i++)
            arr[insert + i] = AnnotationProxyMaker.generateAnnotation(contained[i], annoType);

        return arr;
    }

    private Attribute.Compound[] unpackContained(Attribute.Compound container) {
        // Pack them in an array
        Attribute[] contained0 = null;
        if (container != null)
            contained0 = unpackAttributes(container);
        ListBuffer<Attribute.Compound> compounds = new ListBuffer<>();
        if (contained0 != null) {
            for (Attribute a : contained0)
                if (a instanceof Attribute.Compound attributeCompound)
                    compounds = compounds.append(attributeCompound);
        }
        return compounds.toArray(new Attribute.Compound[compounds.size()]);
    }

    // This method is part of the javax.lang.model API, do not use this in javac code.
    @DefinedBy(Api.LANGUAGE_MODEL)
    public <A extends Annotation> A getAnnotation(Class<A> annoType) {

        if (!annoType.isAnnotation())
            throw new IllegalArgumentException("Not an annotation type: " + annoType);

        Attribute.Compound c = getAttribute(annoType);
        return c == null ? null : AnnotationProxyMaker.generateAnnotation(c, annoType);
    }

    // Helper to getAnnotationsByType
    private static Class<? extends Annotation> getContainer(Class<? extends Annotation> annoType) {
        Repeatable repeatable = annoType.getAnnotation(Repeatable.class);
        return (repeatable == null) ? null : repeatable.value();
    }

    // Helper to getAnnotationsByType
    private static Attribute[] unpackAttributes(Attribute.Compound container) {
        // We now have an instance of the container,
        // unpack it returning an instance of the
        // contained type or null
        return ((Attribute.Array)container.member(container.type.tsym.name.table.names.value)).values;
    }

}
